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

class ReallocationsOutControllerITSpec
    extends AnyFreeSpec
    with Matchers
    with OptionValues
    with WireMockSupport
    with ScalaFutures
    with IntegrationPatience {

  private val regime    = "gbd"
  private val regNumber = "XWM001"

  private val singlePageJson =
    s"""
       |{
       |  "periodStartDate": "2024-01-01",
       |  "periodEndDate": "2024-12-31",
       |  "total": 45.60,
       |  "totalPeriodRecords": 1,
       |  "reallocationsOutAmount": [
       |    {
       |      "dateProcessed": "2024-08-01",
       |      "amount": 45.60
       |    }
       |  ]
       |}
       |""".stripMargin

  private val multiPageJson =
    s"""
       |{
       |  "periodStartDate": "2024-01-01",
       |  "periodEndDate": "2024-12-31",
       |  "total": 600.00,
       |  "totalPeriodRecords": 25,
       |  "reallocationsOutAmount": [
       |    {
       |      "dateProcessed": "2024-08-01",
       |      "amount": 45.60
       |    }
       |  ]
       |}
       |""".stripMargin

  private val emptyPageJson =
    s"""
       |{
       |  "periodStartDate": "2024-01-01",
       |  "periodEndDate": "2024-12-31",
       |  "total": 0,
       |  "totalPeriodRecords": 0,
       |  "reallocationsOutAmount": []
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

  private def stubReallocationsOut(regime: String, regNumber: String, pageSize: Int, pageNo: Int, responseJson: String): Unit =
    stubFor(
      get(urlEqualTo(s"/gambling/reallocations-out/$regime/$regNumber?pageSize=$pageSize&pageNo=$pageNo"))
        .willReturn(okJson(responseJson))
    )

  private val url = routes.ReallocationsOutController.onPageLoad().url

  "ReallocationsOutController" - {

    "session validation" - {

      "must redirect to Unauthorised when regime is absent from the session" in {
        val app = buildApp()

        running(app) {
          val request = FakeRequest(GET, url)
          val result  = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }

      "must redirect to Unauthorised when regNumber is absent from the session" in {
        val app = buildApp()

        running(app) {
          val request = FakeRequest(GET, url).withSession(SessionKeys.regime -> regime)
          val result  = route(app, request).value

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

        stubReallocationsOut(regime, regNumber, pageSize = 10, pageNo = 1, singlePageJson)

        running(app) {
          val request = FakeRequest(GET, url)
            .withSession(SessionKeys.regime -> regime, SessionKeys.regNumber -> regNumber)
          val result = route(app, request).value

          status(result) mustEqual OK
          contentAsString(result) must include("Reallocations out")
        }
      }

      "must include the introductory paragraph in the page body" in {
        val app = buildApp()

        stubReallocationsOut(regime, regNumber, pageSize = 10, pageNo = 1, singlePageJson)

        running(app) {
          val request = FakeRequest(GET, url)
            .withSession(SessionKeys.regime -> regime, SessionKeys.regNumber -> regNumber)
          val result = route(app, request).value

          status(result) mustEqual OK
          contentAsString(result) must include("Payments are sometimes debited from the wrong account.")
        }
      }

      "must render the data table when records are returned" in {
        val app = buildApp()

        stubReallocationsOut(regime, regNumber, pageSize = 10, pageNo = 1, singlePageJson)

        running(app) {
          val request = FakeRequest(GET, url)
            .withSession(SessionKeys.regime -> regime, SessionKeys.regNumber -> regNumber)
          val result  = route(app, request).value
          val body    = contentAsString(result)

          status(result) mustEqual OK
          body must include("govuk-table")
          body must include("The total of the")
          body must include("Displaying")
        }
      }

      "must render the empty-state message when the backend returns no items" in {
        val app = buildApp()

        stubReallocationsOut(regime, regNumber, pageSize = 10, pageNo = 1, emptyPageJson)

        running(app) {
          val request = FakeRequest(GET, url)
            .withSession(SessionKeys.regime -> regime, SessionKeys.regNumber -> regNumber)
          val result = route(app, request).value

          status(result) mustEqual OK
          contentAsString(result) must include("You have no reallocations out.")
        }
      }
    }

    "pagination" - {

      "must not include pagination markup when the response fits on a single page" in {
        val app = buildApp()

        stubReallocationsOut(regime, regNumber, pageSize = 10, pageNo = 1, singlePageJson)

        running(app) {
          val request = FakeRequest(GET, url)
            .withSession(SessionKeys.regime -> regime, SessionKeys.regNumber -> regNumber)
          val result = route(app, request).value

          status(result) mustEqual OK
          contentAsString(result) must not include "govuk-pagination"
        }
      }

      "must include pagination markup when totalPeriodRecords spans multiple pages" in {
        val app = buildApp()

        stubReallocationsOut(regime, regNumber, pageSize = 10, pageNo = 1, multiPageJson)

        running(app) {
          val request = FakeRequest(GET, url)
            .withSession(SessionKeys.regime -> regime, SessionKeys.regNumber -> regNumber)
          val result = route(app, request).value

          status(result) mustEqual OK
          contentAsString(result) must include("govuk-pagination")
        }
      }

      "must forward custom pageSize and PageNo query parameters to the backend" in {
        val customPageSize = 5
        val customPageNo   = 3

        stubReallocationsOut(regime, regNumber, pageSize = customPageSize, pageNo = customPageNo, singlePageJson)

        val app = buildApp()

        running(app) {
          val customUrl = routes.ReallocationsOutController.onPageLoad(pageSize = customPageSize, pageNo = customPageNo).url
          val request   = FakeRequest(GET, customUrl)
            .withSession(SessionKeys.regime -> regime, SessionKeys.regNumber -> regNumber)
          val result = route(app, request).value

          status(result) mustEqual OK
          verify(1, getRequestedFor(
            urlEqualTo(s"/gambling/reallocations-out/$regime/$regNumber?pageSize=$customPageSize&pageNo=$customPageNo")
          ))
        }
      }
    }

    "regime handling" - {

      Seq(
        "gbd" -> "gbd",
        "pbd" -> "pbd",
        "rgd" -> "rgd",
        "mgd" -> "mgd"
      ).foreach { case (sessionCode, expectedBackendCode) =>

        s"must pass regime code '$expectedBackendCode' through to the backend for session value '$sessionCode'" in {
          stubReallocationsOut(expectedBackendCode, regNumber, pageSize = 10, pageNo = 1, singlePageJson)

          val app = buildApp()

          running(app) {
            val request = FakeRequest(GET, url)
              .withSession(SessionKeys.regime -> sessionCode, SessionKeys.regNumber -> regNumber)
            val result = route(app, request).value

            status(result) mustEqual OK
            verify(1, getRequestedFor(
              urlEqualTo(s"/gambling/reallocations-out/$expectedBackendCode/$regNumber?pageSize=10&pageNo=1")
            ))
          }
        }
      }

      "must pass the registration number through to the backend unchanged" in {
        val otherRegNumber = "XWM999"

        stubReallocationsOut(regime, otherRegNumber, pageSize = 10, pageNo = 1, singlePageJson)

        val app = buildApp()

        running(app) {
          val request = FakeRequest(GET, url)
            .withSession(SessionKeys.regime -> regime, SessionKeys.regNumber -> otherRegNumber)
          val result = route(app, request).value

          status(result) mustEqual OK
          verify(1, getRequestedFor(
            urlEqualTo(s"/gambling/reallocations-out/$regime/$otherRegNumber?pageSize=10&pageNo=1")
          ))
        }
      }
    }
  }
}
