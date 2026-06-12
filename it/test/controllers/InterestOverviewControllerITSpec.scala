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

class InterestOverviewControllerITSpec
    extends AnyFreeSpec
    with Matchers
    with OptionValues
    with WireMockSupport
    with ScalaFutures
    with IntegrationPatience {

  private val regime = "mgd"
  private val regNumber = "XWM00003102200"

  private val interestOverviewJson =
    s"""
       |{
       |  "periodStartDate": "2024-01-01",
       |  "periodEndDate": "2024-12-31",
       |  "interestAmount":-81.84,
       |  "interestAccruingAmount":-25.76,
       |  "repaymentInterestAmount":41.23,
       |  "total":66.37
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

  private def stubInterestOverview(regime: String, regNumber: String, responseJson: String): Unit =
    stubFor(
      get(urlEqualTo(s"/gambling/interest-overview/$regime/$regNumber")).willReturn(okJson(responseJson))
    )

  private val url = routes.InterestOverviewController.onPageLoad().url

  "InterestOverviewController" - {

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
        stubInterestOverview(regime, regNumber, interestOverviewJson)

        running(app) {
          val request = FakeRequest(GET, url)
            .withSession(SessionKeys.regime -> regime, SessionKeys.regNumber -> regNumber)
          val result = route(app, request).value

          status(result) mustEqual OK
          contentAsString(result) must include("Breakdown of interest")
          contentAsString(result) must include("govuk-table")
          contentAsString(result) must include("Interest")
          contentAsString(result) must include("Accruing interest")
          contentAsString(result) must include("Repayment interest")
        }
      }

      "must include the introductory paragraph in the page body" in {
        val app = buildApp()
        stubInterestOverview(regime, regNumber, interestOverviewJson)

        running(app) {
          val request = FakeRequest(GET, url)
            .withSession(SessionKeys.regime -> regime, SessionKeys.regNumber -> regNumber)
          val result = route(app, request).value

          status(result) mustEqual OK
          contentAsString(result) must include("The amount of unpaid interest on your account.")
        }
      }
    }

    "regime handling" - {

      Seq("gbd", "pbd", "rgd", "mgd").foreach { code =>

        s"must pass regime code '$code' through to the backend" in {
          stubInterestOverview(code, regNumber, interestOverviewJson)

          val app = buildApp()

          running(app) {
            val request = FakeRequest(GET, url)
              .withSession(SessionKeys.regime -> code, SessionKeys.regNumber -> regNumber)
            val result = route(app, request).value

            status(result) mustEqual OK
            verify(1,
                   getRequestedFor(
                     urlEqualTo(s"/gambling/interest-overview/$code/$regNumber")
                   )
                  )
          }
        }
      }

      "must pass the registration number through to the backend unchanged" in {
        val otherRegNumber = "XWM00003102999"
        stubInterestOverview(regime, otherRegNumber, interestOverviewJson)

        val app = buildApp()

        running(app) {
          val request = FakeRequest(GET, url)
            .withSession(SessionKeys.regime -> regime, SessionKeys.regNumber -> otherRegNumber)
          val result = route(app, request).value

          status(result) mustEqual OK
          verify(1,
                 getRequestedFor(
                   urlEqualTo(s"/gambling/interest-overview/$regime/$otherRegNumber")
                 )
                )
        }
      }
    }
  }
}
