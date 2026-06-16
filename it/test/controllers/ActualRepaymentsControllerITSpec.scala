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

class ActualRepaymentsControllerITSpec
    extends AnyFreeSpec
    with Matchers
    with OptionValues
    with WireMockSupport
    with ScalaFutures
    with IntegrationPatience {

  private val regime    = "gbd"
  private val regNumber = "XWM00003102200"

  private val singlePageJson =
    s"""
       |{
       |  "periodStartDate": "2024-01-01",
       |  "periodEndDate": "2024-12-31",
       |  "total": 45.60,
       |  "totalRecords": 1,
       |  "items": [
       |    {
       |      "transactionDate": "2024-08-01",
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
       |  "totalRecords": 25,
       |  "items": [
       |    {
       |      "transactionDate": "2024-08-01",
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
       |  "totalRecords": 0,
       |  "items": []
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

  private def stubActualRepayments(regime: String, regNumber: String, pageSize: Int, pageNo: Int, responseJson: String): Unit =
    stubFor(
      get(urlEqualTo(s"/gambling/actual-repayments/$regime/$regNumber?pageSize=$pageSize&pageNo=$pageNo"))
        .willReturn(okJson(responseJson))
    )

  private val url = routes.ActualRepaymentsController.onPageLoad().url

  "ActualRepaymentsController" - {

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

      "must return PageNotFound when the session contains an unrecognised regime" in {
        val app = buildApp()

        running(app) {
          val request = FakeRequest(GET, url)
            .withSession(SessionKeys.regime -> "unknown", SessionKeys.regNumber -> regNumber)
          val result = route(app, request).value

          status(result) mustEqual NOT_FOUND
          contentAsString(result) must include("Page not found")
        }
      }
    }

    "successful page load" - {

      "must return OK and render the page heading, paragraph in the page body" in {
        val app = buildApp()

        stubActualRepayments(regime, regNumber, pageSize = 10, pageNo = 1, singlePageJson)

        running(app) {
          val request = FakeRequest(GET, url)
            .withSession(SessionKeys.regime -> regime, SessionKeys.regNumber -> regNumber)
          val result = route(app, request).value

          status(result) mustEqual OK
          contentAsString(result) must include("Actual repayments")
          contentAsString(result) must include("Repayments HMRC has made, or will make to you.")
          contentAsString(result) must include("1 January 2024 to 31 December 2024")
        }
      }

      "must render the data table when records are returned" in {
        val app = buildApp()

        stubActualRepayments(regime, regNumber, pageSize = 10, pageNo = 1, singlePageJson)

        running(app) {
          val request = FakeRequest(GET, url)
            .withSession(SessionKeys.regime -> regime, SessionKeys.regNumber -> regNumber)
          val result  = route(app, request).value
          val body    = contentAsString(result)

          status(result) mustEqual OK
          body must include("govuk-table")
        }
      }

      "must render the empty-state message when the backend returns no items" in {
        val app = buildApp()

        stubActualRepayments(regime, regNumber, pageSize = 10, pageNo = 1, emptyPageJson)

        running(app) {
          val request = FakeRequest(GET, url)
            .withSession(SessionKeys.regime -> regime, SessionKeys.regNumber -> regNumber)
          val result = route(app, request).value

          status(result) mustEqual OK
          contentAsString(result) must include("Actual repayments")
          contentAsString(result) must include("Repayments HMRC has made, or will make to you.")
          contentAsString(result) must include("1 January 2024 to 31 December 2024")
          contentAsString(result) must include("You have no actual repayments.")
        }
      }
    }

    "pagination" - {

      "must not include pagination markup when the response fits on a single page" in {
        val app = buildApp()

        stubActualRepayments(regime, regNumber, pageSize = 10, pageNo = 1, singlePageJson)

        running(app) {
          val request = FakeRequest(GET, url)
            .withSession(SessionKeys.regime -> regime, SessionKeys.regNumber -> regNumber)
          val result = route(app, request).value

          status(result) mustEqual OK
          contentAsString(result) must not include "govuk-pagination"
          contentAsString(result) must not include("The total of the")
          contentAsString(result) must not include("Displaying 1 to 10 of")
        }
      }

      "must include pagination markup and summary paragraphs when totalRecords spans multiple pages" in {
        val app = buildApp()

        stubActualRepayments(regime, regNumber, pageSize = 10, pageNo = 1, multiPageJson)

        running(app) {
          val request = FakeRequest(GET, url)
            .withSession(SessionKeys.regime -> regime, SessionKeys.regNumber -> regNumber)
          val result = route(app, request).value
          val body   = contentAsString(result)

          status(result) mustEqual OK
          body must include("govuk-pagination")
          body must include("The total of the 25 records is")
          body must include("Displaying 1 to 10 of 25 records")
        }
      }

      "must forward custom pageSize and pageNo query parameters to the backend" in {
        val customPageSize = 5
        val customPageNo   = 3

        stubActualRepayments(regime, regNumber, pageSize = customPageSize, pageNo = customPageNo, multiPageJson)

        val app = buildApp()

        running(app) {
          val customUrl = routes.ActualRepaymentsController.onPageLoad(pageSize = customPageSize, pageNo = customPageNo).url
          val request   = FakeRequest(GET, customUrl)
            .withSession(SessionKeys.regime -> regime, SessionKeys.regNumber -> regNumber)
          val result = route(app, request).value

          status(result) mustEqual OK
          verify(1, getRequestedFor(
            urlEqualTo(s"/gambling/actual-repayments/$regime/$regNumber?pageSize=$customPageSize&pageNo=$customPageNo")
          ))
        }
      }

      "must return Not Found with page not found content when pageNo exceeds totalPages" in {
        val app = buildApp()

        stubActualRepayments(regime, regNumber, pageSize = 10, pageNo = 99, multiPageJson)

        running(app) {
          val request = FakeRequest(GET, routes.ActualRepaymentsController.onPageLoad(pageSize = 10, pageNo = 99).url)
            .withSession(SessionKeys.regime -> regime, SessionKeys.regNumber -> regNumber)
          val result = route(app, request).value

          status(result) mustEqual NOT_FOUND
          contentAsString(result) must include("Page not found")
        }
      }
    }

    "regime handling" - {

      Seq("gbd", "pbd", "rgd", "mgd").foreach { code =>

        s"must pass regime code '$code' through to the backend" in {
          stubActualRepayments(code, regNumber, pageSize = 10, pageNo = 1, singlePageJson)

          val app = buildApp()

          running(app) {
            val request = FakeRequest(GET, url)
              .withSession(SessionKeys.regime -> code, SessionKeys.regNumber -> regNumber)
            val result = route(app, request).value

            status(result) mustEqual OK
            verify(1, getRequestedFor(
              urlEqualTo(s"/gambling/actual-repayments/$code/$regNumber?pageSize=10&pageNo=1")
            ))
          }
        }
      }

      "must pass the registration number through to the backend unchanged" in {
        val otherRegNumber = "XWM00003102999"

        stubActualRepayments(regime, otherRegNumber, pageSize = 10, pageNo = 1, singlePageJson)

        val app = buildApp()

        running(app) {
          val request = FakeRequest(GET, url)
            .withSession(SessionKeys.regime -> regime, SessionKeys.regNumber -> otherRegNumber)
          val result = route(app, request).value

          status(result) mustEqual OK
          verify(1, getRequestedFor(
            urlEqualTo(s"/gambling/actual-repayments/$regime/$otherRegNumber?pageSize=10&pageNo=1")
          ))
        }
      }
    }
  }
}
