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

package connectors

import com.github.tomakehurst.wiremock.client.WireMock.*
import models.assessments.{AssessmentItem, Assessments, Penalties, PenaltyItem}
import models.reallocations.{ReallocationItem, Reallocations}
import models.returns.{AmountDeclared, ReturnsSubmitted}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.running
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.test.WireMockSupport

import java.time.LocalDate

class GamblingConnectorSpec extends AnyFreeSpec with Matchers with WireMockSupport with ScalaFutures with IntegrationPatience {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val regime = "gbd"
  private val regNumber = "XWM00003102200"
  private val pageSize = 10
  private val pageNo = 1

  private val responseJson =
    s"""
       |{
       |  "periodStartDate": "2024-01-01",
       |  "periodEndDate": "2024-12-31",
       |  "amountDeclared": [
       |    {
       |      "descriptionCode": 1,
       |      "periodStartDate": "2024-01-01",
       |      "periodEndDate": "2024-03-31",
       |      "amount": 1000.50
       |    }
       |  ],
       |  "total": 1000.50,
       |  "totalPeriodRecords": 1
       |}
       |""".stripMargin

  private val expectedResponse = ReturnsSubmitted(
    periodStartDate    = Some(LocalDate.of(2024, 1, 1)),
    periodEndDate      = Some(LocalDate.of(2024, 12, 31)),
    amountDeclared     = Seq(AmountDeclared(Some(1), Some(LocalDate.of(2024, 1, 1)), Some(LocalDate.of(2024, 3, 31)), Some(BigDecimal("1000.5")))),
    total              = Some(BigDecimal("1000.5")),
    totalPeriodRecords = Some(1)
  )

  private def buildApp() =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.gambling.protocol" -> "http",
        "microservice.services.gambling.host"     -> wireMockHost,
        "microservice.services.gambling.port"     -> wireMockPort
      )
      .build()

  "GamblingConnector" - {

    "getReturnsSubmitted" - {

      "must return a deserialized ReturnsSubmitted for a 200 response" in {
        stubFor(
          get(urlEqualTo(s"/gambling/returns-submitted/$regime/$regNumber?pageSize=$pageSize&pageNo=$pageNo"))
            .willReturn(okJson(responseJson))
        )

        val app = buildApp()
        running(app) {
          val connector = app.injector.instanceOf[GamblingConnector]
          val result = connector.getReturnsSubmitted(regime, regNumber, pageSize, pageNo).futureValue

          result mustEqual expectedResponse
        }
      }

      "must forward the correct regime and registration number in the URL" in {
        val otherRegime = "pbd"
        val otherRegNumber = "XWM00003102999"

        stubFor(
          get(urlEqualTo(s"/gambling/returns-submitted/$otherRegime/$otherRegNumber?pageSize=$pageSize&pageNo=$pageNo"))
            .willReturn(okJson(responseJson))
        )

        val app = buildApp()
        running(app) {
          val connector = app.injector.instanceOf[GamblingConnector]
          val result = connector.getReturnsSubmitted(otherRegime, otherRegNumber, pageSize, pageNo).futureValue

          result mustEqual expectedResponse
        }
      }

      "must forward custom pageSize and PageNo query parameters" in {
        val customPageSize = 5
        val customPageNo = 3

        stubFor(
          get(urlEqualTo(s"/gambling/returns-submitted/$regime/$regNumber?pageSize=$customPageSize&pageNo=$customPageNo"))
            .willReturn(okJson(responseJson))
        )

        val app = buildApp()
        running(app) {
          val connector = app.injector.instanceOf[GamblingConnector]
          val result = connector.getReturnsSubmitted(regime, regNumber, customPageSize, customPageNo).futureValue

          result mustEqual expectedResponse
        }
      }
    }

    "getReallocationsIn" - {

      val reallocationsResponseJson =
        s"""
           |{
           |  "periodStartDate": "2024-01-01",
           |  "periodEndDate": "2024-12-31",
           |  "total": 30.80,
           |  "totalRecords": 1,
           |  "items": [
           |    {
           |      "dateProcessed": "2024-07-01",
           |      "amount": 30.80
           |    }
           |  ]
           |}
           |""".stripMargin

      val expectedReallocationsResponse = Reallocations(
        periodStartDate = Some(LocalDate.of(2024, 1, 1)),
        periodEndDate   = Some(LocalDate.of(2024, 12, 31)),
        total           = Some(BigDecimal("30.8")),
        totalRecords    = Some(1),
        items           = Seq(ReallocationItem(Some(LocalDate.of(2024, 7, 1)), Some(BigDecimal("30.8"))))
      )

      "must return a deserialized ReallocationsIn for a 200 response" in {
        stubFor(
          get(urlEqualTo(s"/gambling/reallocations-in/$regime/$regNumber?pageSize=$pageSize&pageNo=$pageNo"))
            .willReturn(okJson(reallocationsResponseJson))
        )

        val app = buildApp()
        running(app) {
          val connector = app.injector.instanceOf[GamblingConnector]
          val result = connector.getReallocationsIn(regime, regNumber, pageSize, pageNo).futureValue

          result mustEqual expectedReallocationsResponse
        }
      }

      "must forward the correct regime and registration number in the URL" in {
        val otherRegime = "pbd"
        val otherRegNumber = "XWM00003102999"

        stubFor(
          get(urlEqualTo(s"/gambling/reallocations-in/$otherRegime/$otherRegNumber?pageSize=$pageSize&pageNo=$pageNo"))
            .willReturn(okJson(reallocationsResponseJson))
        )

        val app = buildApp()
        running(app) {
          val connector = app.injector.instanceOf[GamblingConnector]
          val result = connector.getReallocationsIn(otherRegime, otherRegNumber, pageSize, pageNo).futureValue

          result mustEqual expectedReallocationsResponse
        }
      }

      "must forward custom pageSize and PageNo query parameters" in {
        val customPageSize = 5
        val customPageNo = 3

        stubFor(
          get(urlEqualTo(s"/gambling/reallocations-in/$regime/$regNumber?pageSize=$customPageSize&pageNo=$customPageNo"))
            .willReturn(okJson(reallocationsResponseJson))
        )

        val app = buildApp()
        running(app) {
          val connector = app.injector.instanceOf[GamblingConnector]
          val result = connector.getReallocationsIn(regime, regNumber, customPageSize, customPageNo).futureValue

          result mustEqual expectedReallocationsResponse
        }
      }
    }

    "getReallocationsOut" - {

      val reallocationsOutResponseJson =
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

      val expectedReallocationsOutResponse = Reallocations(
        periodStartDate = Some(LocalDate.of(2024, 1, 1)),
        periodEndDate   = Some(LocalDate.of(2024, 12, 31)),
        total           = Some(BigDecimal("45.6")),
        totalRecords    = Some(1),
        items           = Seq(ReallocationItem(Some(LocalDate.of(2024, 8, 1)), Some(BigDecimal("45.6"))))
      )

      "must return a deserialized ReallocationsOut for a 200 response" in {
        stubFor(
          get(urlEqualTo(s"/gambling/reallocations-out/$regime/$regNumber?pageSize=$pageSize&pageNo=$pageNo"))
            .willReturn(okJson(reallocationsOutResponseJson))
        )

        val app = buildApp()
        running(app) {
          val connector = app.injector.instanceOf[GamblingConnector]
          val result = connector.getReallocationsOut(regime, regNumber, pageSize, pageNo).futureValue

          result mustEqual expectedReallocationsOutResponse
        }
      }

      "must forward the correct regime and registration number in the URL" in {
        val otherRegime = "pbd"
        val otherRegNumber = "XWM00003102999"

        stubFor(
          get(urlEqualTo(s"/gambling/reallocations-out/$otherRegime/$otherRegNumber?pageSize=$pageSize&pageNo=$pageNo"))
            .willReturn(okJson(reallocationsOutResponseJson))
        )

        val app = buildApp()
        running(app) {
          val connector = app.injector.instanceOf[GamblingConnector]
          val result = connector.getReallocationsOut(otherRegime, otherRegNumber, pageSize, pageNo).futureValue

          result mustEqual expectedReallocationsOutResponse
        }
      }

      "must forward custom pageSize and pageNo query parameters" in {
        val customPageSize = 5
        val customPageNo = 3

        stubFor(
          get(urlEqualTo(s"/gambling/reallocations-out/$regime/$regNumber?pageSize=$customPageSize&pageNo=$customPageNo"))
            .willReturn(okJson(reallocationsOutResponseJson))
        )

        val app = buildApp()
        running(app) {
          val connector = app.injector.instanceOf[GamblingConnector]
          val result = connector.getReallocationsOut(regime, regNumber, customPageSize, customPageNo).futureValue

          result mustEqual expectedReallocationsOutResponse
        }
      }
    }

    "getOtherAssessments" - {

      val otherAssessmentsResponseJson =
        s"""
           |{
           |  "periodStartDate": "2024-01-01",
           |  "periodEndDate": "2024-12-31",
           |  "total": 65.60,
           |  "totalRecords": 1,
           |  "items": [
           |    {
           |      "dateRaised": "2024-08-01",
           |      "periodStartDate": "2024-07-01",
           |      "periodEndDate": "2024-09-01",
           |      "amount": 65.60
           |    }
           |  ]
           |}
           |""".stripMargin

      val expectedOtherAssessmentsResponse = Assessments(
        periodStartDate = Some(LocalDate.of(2024, 1, 1)),
        periodEndDate   = Some(LocalDate.of(2024, 12, 31)),
        total           = Some(BigDecimal("65.6")),
        totalRecords    = Some(1),
        items = Seq(
          AssessmentItem(Some(LocalDate.of(2024, 8, 1)), Some(LocalDate.of(2024, 7, 1)), Some(LocalDate.of(2024, 9, 1)), Some(BigDecimal("65.6")))
        )
      )

      "must return a deserialized OtherAssessments for a 200 response" in {
        stubFor(
          get(urlEqualTo(s"/gambling/other-assessments/$regime/$regNumber?pageSize=$pageSize&pageNo=$pageNo"))
            .willReturn(okJson(otherAssessmentsResponseJson))
        )

        val app = buildApp()
        running(app) {
          val connector = app.injector.instanceOf[GamblingConnector]
          val result = connector.getOtherAssessments(regime, regNumber, pageSize, pageNo).futureValue

          result mustEqual expectedOtherAssessmentsResponse
        }
      }

      "must forward the correct regime and registration number in the URL" in {
        val otherRegime = "pbd"
        val otherRegNumber = "XWM00003102999"

        stubFor(
          get(urlEqualTo(s"/gambling/other-assessments/$otherRegime/$otherRegNumber?pageSize=$pageSize&pageNo=$pageNo"))
            .willReturn(okJson(otherAssessmentsResponseJson))
        )

        val app = buildApp()
        running(app) {
          val connector = app.injector.instanceOf[GamblingConnector]
          val result = connector.getOtherAssessments(otherRegime, otherRegNumber, pageSize, pageNo).futureValue

          result mustEqual expectedOtherAssessmentsResponse
        }
      }

      "must forward custom pageSize and pageNo query parameters" in {
        val customPageSize = 5
        val customPageNo = 3

        stubFor(
          get(urlEqualTo(s"/gambling/other-assessments/$regime/$regNumber?pageSize=$customPageSize&pageNo=$customPageNo"))
            .willReturn(okJson(otherAssessmentsResponseJson))
        )

        val app = buildApp()
        running(app) {
          val connector = app.injector.instanceOf[GamblingConnector]
          val result = connector.getOtherAssessments(regime, regNumber, customPageSize, customPageNo).futureValue

          result mustEqual expectedOtherAssessmentsResponse
        }
      }
    }
    "getPenalties" - {

      val penaltiesResponseJson =
        s"""
           |{
           |  "periodStartDate": "2024-01-01",
           |  "periodEndDate": "2024-12-31",
           |  "total": -500.00,
           |  "totalRecords": 1,
           |  "items": [
           |    {
           |      "dateRaised": "2024-08-01",
           |      "descriptionCode": 1980,
           |      "amount": -500.00,
           |      "periodStartDate": "2024-01-01",
           |      "periodEndDate": "2024-03-31"
           |    }
           |  ]
           |}
           |""".stripMargin

      val expectedPenaltiesResponse = Penalties(
        periodStartDate = Some(LocalDate.of(2024, 1, 1)),
        periodEndDate   = Some(LocalDate.of(2024, 12, 31)),
        total           = BigDecimal("-500.0"),
        totalRecords    = 1,
        items = Seq(
          PenaltyItem(LocalDate.of(2024, 8, 1), 1980, BigDecimal("-500.0"), LocalDate.of(2024, 1, 1), LocalDate.of(2024, 3, 31))
        )
      )

      "must return a deserialized Penalties for a 200 response" in {
        stubFor(
          get(urlEqualTo(s"/gambling/penalties/$regime/$regNumber?pageSize=$pageSize&pageNo=$pageNo"))
            .willReturn(okJson(penaltiesResponseJson))
        )

        val app = buildApp()
        running(app) {
          val connector = app.injector.instanceOf[GamblingConnector]
          val result = connector.getPenalties(regime, regNumber, pageSize, pageNo).futureValue

          result mustEqual expectedPenaltiesResponse
        }
      }

      "must forward the correct regime and registration number in the URL" in {
        val otherRegime = "pbd"
        val otherRegNumber = "XWM00003102999"

        stubFor(
          get(urlEqualTo(s"/gambling/penalties/$otherRegime/$otherRegNumber?pageSize=$pageSize&pageNo=$pageNo"))
            .willReturn(okJson(penaltiesResponseJson))
        )

        val app = buildApp()
        running(app) {
          val connector = app.injector.instanceOf[GamblingConnector]
          val result = connector.getPenalties(otherRegime, otherRegNumber, pageSize, pageNo).futureValue

          result mustEqual expectedPenaltiesResponse
        }
      }

      "must forward custom pageSize and pageNo query parameters" in {
        val customPageSize = 5
        val customPageNo = 3

        stubFor(
          get(urlEqualTo(s"/gambling/penalties/$regime/$regNumber?pageSize=$customPageSize&pageNo=$customPageNo"))
            .willReturn(okJson(penaltiesResponseJson))
        )

        val app = buildApp()
        running(app) {
          val connector = app.injector.instanceOf[GamblingConnector]
          val result = connector.getPenalties(regime, regNumber, customPageSize, customPageNo).futureValue

          result mustEqual expectedPenaltiesResponse
        }
      }
    }
  }
}
