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

class InterestAccruingDrilldownControllerITSpec
    extends AnyFreeSpec
    with Matchers
    with OptionValues
    with WireMockSupport
    with ScalaFutures
    with IntegrationPatience {

  private val regime = "gbd"
  private val regNumber = "XWM00003102200"

  private val interestAccruingJson =
    s"""
       |{
       |  "periodStartDate": "2024-01-01",
       |  "periodEndDate": "2024-12-31",
       |  "total": 123.45,
       |  "totalRecords": 1,
       |  "descriptionCode": 2650,
       |  "items": [
       |    {
       |      "interestOn": 1000.00,
       |      "dateFrom": "2024-01-01",
       |      "dateTo": "2024-03-31",
       |      "noOfDays": 90,
       |      "rate": 2.5,
       |      "amount": 123.45
       |    }
       |  ]
       |}
       |""".stripMargin

  private val multiPageJson =
    s"""
       |{
       |  "periodStartDate": "2024-01-01",
       |  "periodEndDate": "2024-12-31",
       |  "total": 123.45,
       |  "totalRecords": 25,
       |  "descriptionCode": 2650,
       |  "items": [
       |    {
       |      "interestOn": 1000.00,
       |      "dateFrom": "2024-01-01",
       |      "dateTo": "2024-03-31",
       |      "noOfDays": 90,
       |      "rate": 2.5,
       |      "amount": 123.45
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

  private val interestId = "INT-001"
  private val pageSize = 10
  private val pageNo = 1

  private def stubInterestAccruing(
    regime: String,
    regNumber: String,
    responseJson: String,
    interestId: String = interestId,
    page: Int = pageNo
  ): Unit =
    stubFor(
      get(urlEqualTo(s"/gambling/interest-accruing-drilldown/$regime/$regNumber/$interestId?pageSize=$pageSize&pageNo=$page"))
        .willReturn(okJson(responseJson))
    )

  private val url = routes.InterestAccruingDrilldownController.onPageLoad(interestId).url

  "InterestAccruingDrilldownController" - {

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

      Seq(
        (1940, "PPLR Interest Bearing"),
        (1950, "Return Charge"),
        (1960, "Central Assessment"),
        (1970, "Officer Assessment"),
        (1980, "Late Filing Penalty"),
        (1990, "Late Payment Penalty"),
        (2640, "PPLR Interest Bearing"),
        (2650, "Return Charge"),
        (2655, "Return Interest"),
        (2660, "Central Assessment"),
        (2670, "Officer Assessment"),
        (2680, "Late Filing Penalty"),
        (2685, "Late Filing Penalty Interest"),
        (2690, "Late Payment Penalty"),
        (2695, "Late Payment Penalty Interest")
      ).foreach { case (code, label) =>
        s"must render the heading, paragraph for description code $code ($label) and table" in {
          val json =
            s"""
               |{
               |  "periodStartDate": "2024-01-01",
               |  "periodEndDate": "2024-12-31",
               |  "total": 123.45,
               |  "totalRecords": 1,
               |  "descriptionCode": $code,
               |  "items": [
               |    {
               |      "interestOn": 1000.00,
               |      "dateFrom": "2024-01-01",
               |      "dateTo": "2024-03-31",
               |      "noOfDays": 90,
               |      "rate": 2.5,
               |      "amount": 123.45
               |    }
               |  ]
               |}
               |""".stripMargin

          val app = buildApp()
          stubInterestAccruing(regime, regNumber, json)

          running(app) {
            val request = FakeRequest(GET, url)
              .withSession(SessionKeys.regime -> regime, SessionKeys.regNumber -> regNumber)
            val result = route(app, request).value

            status(result) mustEqual OK
            contentAsString(result) must include(s"Interest (accruing) on $label")
            contentAsString(result) must include(s"The amount of unpaid interest on $label.")
            contentAsString(result) must include("govuk-table")
          }
        }
      }

      "must render pagination and summary paragraphs when there are multiple pages" in {
        val app = buildApp()
        stubInterestAccruing(regime, regNumber, multiPageJson)

        running(app) {
          val request = FakeRequest(GET, url)
            .withSession(SessionKeys.regime -> regime, SessionKeys.regNumber -> regNumber)
          val result = route(app, request).value
          val body = contentAsString(result)

          status(result) mustEqual OK
          body must include("govuk-pagination")
          body must include("The total of the 25 records is")
          body must include("Displaying 1 to 10 of 25 records")
          body must not include "interest-accruing-total"
        }
      }

      "must not render pagination when there is only one page" in {
        val app = buildApp()
        stubInterestAccruing(regime, regNumber, interestAccruingJson)

        running(app) {
          val request = FakeRequest(GET, url)
            .withSession(SessionKeys.regime -> regime, SessionKeys.regNumber -> regNumber)
          val result = route(app, request).value
          val body = contentAsString(result)

          status(result) mustEqual OK
          body must not include "govuk-pagination"
          body must not include "The total of the"
          body must not include "Displaying"
        }
      }

      "must not render the total row in the table when there are multiple pages" in {
        val app = buildApp()
        stubInterestAccruing(regime, regNumber, multiPageJson)

        running(app) {
          val request = FakeRequest(GET, url)
            .withSession(SessionKeys.regime -> regime, SessionKeys.regNumber -> regNumber)
          val result = route(app, request).value

          status(result) mustEqual OK
          contentAsString(result) must not include "interest-accruing-total"
        }
      }

      "must return Not Found with page not found content when pageNo exceeds totalPages" in {
        val app = buildApp()
        stubInterestAccruing(regime, regNumber, multiPageJson, page = 99)

        running(app) {
          val request = FakeRequest(GET, routes.InterestAccruingDrilldownController.onPageLoad(interestId, pageSize, 99).url)
            .withSession(SessionKeys.regime -> regime, SessionKeys.regNumber -> regNumber)
          val result = route(app, request).value

          status(result) mustEqual NOT_FOUND
          contentAsString(result) must include("Page not found")
        }
      }

      "must return page not found when data has 0 items" in {
        val emptyJson =
          s"""
             |{
             |  "periodStartDate": "2024-01-01",
             |  "periodEndDate": "2024-12-31",
             |  "total": 0,
             |  "totalRecords": 0,
             |  "descriptionCode": 2650,
             |  "items": []
             |}
             |""".stripMargin

        val app = buildApp()
        stubInterestAccruing(regime, regNumber, emptyJson)

        running(app) {
          val request = FakeRequest(GET, url)
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
          stubInterestAccruing(code, regNumber, interestAccruingJson)

          val app = buildApp()

          running(app) {
            val request = FakeRequest(GET, url)
              .withSession(SessionKeys.regime -> code, SessionKeys.regNumber -> regNumber)
            val result = route(app, request).value

            status(result) mustEqual OK
            verify(1,
                   getRequestedFor(
                     urlEqualTo(s"/gambling/interest-accruing-drilldown/$code/$regNumber/$interestId?pageSize=$pageSize&pageNo=$pageNo")
                   )
                  )
          }
        }
      }

      "must pass the registration number through to the backend unchanged" in {
        val otherRegNumber = "XWM00003102999"
        stubInterestAccruing(regime, otherRegNumber, interestAccruingJson)

        val app = buildApp()

        running(app) {
          val request = FakeRequest(GET, url)
            .withSession(SessionKeys.regime -> regime, SessionKeys.regNumber -> otherRegNumber)
          val result = route(app, request).value

          status(result) mustEqual OK
          verify(1,
                 getRequestedFor(
                   urlEqualTo(s"/gambling/interest-accruing-drilldown/$regime/$otherRegNumber/$interestId?pageSize=$pageSize&pageNo=$pageNo")
                 )
                )
        }
      }
    }
  }
}
