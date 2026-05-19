/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers

import com.github.tomakehurst.wiremock.client.WireMock.{get, getRequestedFor, okJson, stubFor, urlEqualTo, verify}
import controllers.actions.{FakeIdentifierAction, IdentifierAction}
import models.SessionKeys
import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.http.test.WireMockSupport

class ReallocationsControllerITSpec
    extends AnyFreeSpec
    with Matchers
    with OptionValues
    with WireMockSupport
    with ScalaFutures
    with IntegrationPatience {

  private val regime = "gbd"
  private val regNumber = "XWM00003102200"

  private val reallocationsInJson =
    s"""
       |{
       |  "periodStartDate": "2024-01-01",
       |  "periodEndDate": "2024-12-31",
       |  "total": 45.60,
       |  "totalRecords": 1,
       |  "items": [
       |    {
       |      "dateProcessed": "2024-08-01",
       |      "amount": 45.60
       |    }
       |  ]
       |}
       |""".stripMargin

  private val reallocationsOutJson =
    s"""
       |{
       |  "periodStartDate": "2024-01-01",
       |  "periodEndDate": "2024-12-31",
       |  "total": -45.60,
       |  "totalRecords": 1,
       |  "items": [
       |    {
       |      "dateProcessed": "2024-08-01",
       |      "amount": -45.60
       |    }
       |  ]
       |}
       |""".stripMargin

  private def buildApp() =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.gambling.protocol" -> "http",
        "microservice.services.gambling.host"     -> wireMockHost,
        "microservice.services.gambling.port"     -> wireMockPort,
        "play.http.router"                        -> "app.Routes"
      )
      .overrides(
        bind[IdentifierAction].to[FakeIdentifierAction]
      )
      .build()

  private def stubReallocationsIn(regime: String, regNumber: String, pageSize: Int, pageNo: Int, responseJson: String): Unit =
    stubFor(
      get(urlEqualTo(s"/gambling/reallocations-in/$regime/$regNumber?pageSize=$pageSize&pageNo=$pageNo"))
        .willReturn(okJson(responseJson))
    )

  private def stubReallocationsOut(regime: String, regNumber: String, pageSize: Int, pageNo: Int, responseJson: String): Unit =
    stubFor(
      get(urlEqualTo(s"/gambling/reallocations-out/$regime/$regNumber?pageSize=$pageSize&pageNo=$pageNo"))
        .willReturn(okJson(responseJson))
    )

  private val url = routes.ReallocationsController.onPageLoad().url

  "ReallocationsController" - {

    "session validation" - {

      "must redirect to Unauthorised when regime is absent from the session" in {
        val app = buildApp()

        running(app) {
          val request = FakeRequest(GET, url)
          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }

      "must redirect to Unauthorised when regNumber is absent from the session" in {
        val app = buildApp()

        running(app) {
          val request = FakeRequest(GET, url).withSession(SessionKeys.regime -> regime)
          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }

      "must redirect to PageNotFound when the session contains an unrecognised regime" in {
        val app = buildApp()

        running(app) {
          val request = FakeRequest(GET, url)
            .withSession(SessionKeys.regime -> "unknown", SessionKeys.regNumber -> regNumber)
          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.PageNotFoundController.onPageLoad().url
        }
      }
    }

    "successful page load" - {

      "must return OK and render the page heading" in {
        val app = buildApp()
        stubReallocationsOut(regime, regNumber, pageSize = 10, pageNo = 1, reallocationsOutJson)
        stubReallocationsIn(regime, regNumber, pageSize  = 10, pageNo = 1, reallocationsInJson)

        running(app) {
          val request = FakeRequest(GET, url)
            .withSession(SessionKeys.regime -> regime, SessionKeys.regNumber -> regNumber)
          val result = route(app, request).value

          status(result) mustEqual OK
          contentAsString(result) must include("Reallocations")
          contentAsString(result) must include("govuk-table")
          contentAsString(result) must include("Reallocations In")
          contentAsString(result) must include("Reallocations Out")
        }
      }

      "must include the introductory paragraph in the page body" in {
        val app = buildApp()
        stubReallocationsOut(regime, regNumber, pageSize = 10, pageNo = 1, reallocationsOutJson)
        stubReallocationsIn(regime, regNumber, pageSize  = 10, pageNo = 1, reallocationsInJson)

        running(app) {
          val request = FakeRequest(GET, url)
            .withSession(SessionKeys.regime -> regime, SessionKeys.regNumber -> regNumber)
          val result = route(app, request).value

          status(result) mustEqual OK
          contentAsString(result) must include("Money moved in or out of your account, or transferred to another tax.")
        }
      }
    }

    "regime handling" - {

      Seq("gbd", "pbd", "rgd", "mgd").foreach { code =>

        s"must pass regime code '$code' through to the backend" in {
          stubReallocationsOut(code, regNumber, pageSize = 10, pageNo = 1, reallocationsOutJson)
          stubReallocationsIn(code, regNumber, pageSize  = 10, pageNo = 1, reallocationsInJson)

          val app = buildApp()

          running(app) {
            val request = FakeRequest(GET, url)
              .withSession(SessionKeys.regime -> code, SessionKeys.regNumber -> regNumber)
            val result = route(app, request).value

            status(result) mustEqual OK
            verify(1,
                   getRequestedFor(
                     urlEqualTo(s"/gambling/reallocations-out/$code/$regNumber?pageSize=10&pageNo=1")
                   )
                  )
            verify(1,
                   getRequestedFor(
                     urlEqualTo(s"/gambling/reallocations-in/$code/$regNumber?pageSize=10&pageNo=1")
                   )
                  )
          }
        }
      }

      "must pass the registration number through to the backend unchanged" in {
        val otherRegNumber = "XWM00003102999"
        stubReallocationsOut(regime, otherRegNumber, pageSize = 10, pageNo = 1, reallocationsOutJson)
        stubReallocationsIn(regime, otherRegNumber, pageSize  = 10, pageNo = 1, reallocationsInJson)

        val app = buildApp()

        running(app) {
          val request = FakeRequest(GET, url)
            .withSession(SessionKeys.regime -> regime, SessionKeys.regNumber -> otherRegNumber)
          val result = route(app, request).value

          status(result) mustEqual OK
          verify(1,
                 getRequestedFor(
                   urlEqualTo(s"/gambling/reallocations-out/$regime/$otherRegNumber?pageSize=10&pageNo=1")
                 )
                )
          verify(1,
                 getRequestedFor(
                   urlEqualTo(s"/gambling/reallocations-in/$regime/$otherRegNumber?pageSize=10&pageNo=1")
                 )
                )
        }
      }
    }
  }
}
