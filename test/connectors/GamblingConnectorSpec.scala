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
import models.assessments.{AssessmentItem, Assessments}
import models.payments.{PaymentItem, Payments}
import models.penalties.{Penalties, PenaltyItem}
import models.reallocations.{ReallocationItem, Reallocations, ReallocationsDetails}
import models.repayments.{ActualRepaymentItem, ActualRepayments, RepaymentInterestRepaid, RepaymentInterestRepaidItem, RepaymentsSummary}
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

    "getReallocationsDetails" - {

      val reallocationsDetailsResponseJson =
        s"""
           |{
           |  "periodStartDate": "2024-01-01",
           |  "periodEndDate": "2024-12-31",
           |  "reallocationsInAmount": 45.60,
           |  "reallocationsOutAmount": -85.60,
           |  "total": -45.60
           |}
           |""".stripMargin

      val expectedReallocationsDetailsResponse = ReallocationsDetails(
        periodStartDate        = Some(LocalDate.of(2024, 1, 1)),
        periodEndDate          = Some(LocalDate.of(2024, 12, 31)),
        reallocationsInAmount  = BigDecimal("45.6"),
        reallocationsOutAmount = BigDecimal("-85.6"),
        total                  = BigDecimal("-45.6")
      )

      "must return a deserialized ReallocationsDetails for a 200 response" in {
        stubFor(
          get(urlEqualTo(s"/gambling/reallocations-details/$regime/$regNumber"))
            .willReturn(okJson(reallocationsDetailsResponseJson))
        )

        val app = buildApp()
        running(app) {
          val connector = app.injector.instanceOf[GamblingConnector]
          val result = connector.getReallocationsDetails(regime, regNumber).futureValue

          result mustEqual expectedReallocationsDetailsResponse
        }
      }

      "must forward the correct regime and registration number in the URL" in {
        val otherRegime = "pbd"
        val otherRegNumber = "XWM00003102999"

        stubFor(
          get(urlEqualTo(s"/gambling/reallocations-details/$otherRegime/$otherRegNumber"))
            .willReturn(okJson(reallocationsDetailsResponseJson))
        )

        val app = buildApp()
        running(app) {
          val connector = app.injector.instanceOf[GamblingConnector]
          val result = connector.getReallocationsDetails(otherRegime, otherRegNumber).futureValue

          result mustEqual expectedReallocationsDetailsResponse
        }
      }
    }

    "getAssessmentsWithoutReturns" - {

      val assessmentsWithoutReturnsResponseJson =
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

      val expectedAssessmentsWithoutReturnsResponse = Assessments(
        periodStartDate = Some(LocalDate.of(2024, 1, 1)),
        periodEndDate   = Some(LocalDate.of(2024, 12, 31)),
        total           = Some(BigDecimal("65.6")),
        totalRecords    = Some(1),
        items = Seq(
          AssessmentItem(Some(LocalDate.of(2024, 8, 1)), Some(LocalDate.of(2024, 7, 1)), Some(LocalDate.of(2024, 9, 1)), Some(BigDecimal("65.6")))
        )
      )

      "must return a deserialized Assessments for a 200 response" in {
        stubFor(
          get(urlEqualTo(s"/gambling/assessments-without-returns/$regime/$regNumber?pageSize=$pageSize&pageNo=$pageNo"))
            .willReturn(okJson(assessmentsWithoutReturnsResponseJson))
        )

        val app = buildApp()
        running(app) {
          val connector = app.injector.instanceOf[GamblingConnector]
          val result = connector.getAssessmentsWithoutReturns(regime, regNumber, pageSize, pageNo).futureValue

          result mustEqual expectedAssessmentsWithoutReturnsResponse
        }
      }

      "must forward the correct regime and registration number in the URL" in {
        val otherRegime = "pbd"
        val otherRegNumber = "XWM00003102999"

        stubFor(
          get(urlEqualTo(s"/gambling/assessments-without-returns/$otherRegime/$otherRegNumber?pageSize=$pageSize&pageNo=$pageNo"))
            .willReturn(okJson(assessmentsWithoutReturnsResponseJson))
        )

        val app = buildApp()
        running(app) {
          val connector = app.injector.instanceOf[GamblingConnector]
          val result = connector.getAssessmentsWithoutReturns(otherRegime, otherRegNumber, pageSize, pageNo).futureValue

          result mustEqual expectedAssessmentsWithoutReturnsResponse
        }
      }

      "must forward custom pageSize and pageNo query parameters" in {
        val customPageSize = 5
        val customPageNo = 3

        stubFor(
          get(urlEqualTo(s"/gambling/assessments-without-returns/$regime/$regNumber?pageSize=$customPageSize&pageNo=$customPageNo"))
            .willReturn(okJson(assessmentsWithoutReturnsResponseJson))
        )

        val app = buildApp()
        running(app) {
          val connector = app.injector.instanceOf[GamblingConnector]
          val result = connector.getAssessmentsWithoutReturns(regime, regNumber, customPageSize, customPageNo).futureValue

          result mustEqual expectedAssessmentsWithoutReturnsResponse
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

    "getPayments" - {

      val paymentsResponseJson =
        s"""
           |{
           |  "periodStartDate": "2023-11-01",
           |  "periodEndDate": "2025-01-27",
           |  "total": -291.64,
           |  "totalRecords": 1,
           |  "items": [
           |    {
           |      "transactionDate": "2024-07-23",
           |      "descriptionCode": "E",
           |      "amount": -291.64
           |    }
           |  ]
           |}
           |""".stripMargin

      val expectedPaymentsResponse = Payments(
        periodStartDate = Some(LocalDate.of(2023, 11, 1)),
        periodEndDate   = Some(LocalDate.of(2025, 1, 27)),
        total           = BigDecimal("-291.64"),
        totalRecords    = 1,
        items           = Seq(PaymentItem(LocalDate.of(2024, 7, 23), "E", BigDecimal("-291.64")))
      )

      "must return a deserialized Payments for a 200 response" in {
        stubFor(
          get(urlEqualTo(s"/gambling/payments/$regime/$regNumber?pageSize=$pageSize&pageNo=$pageNo"))
            .willReturn(okJson(paymentsResponseJson))
        )

        val app = buildApp()
        running(app) {
          val connector = app.injector.instanceOf[GamblingConnector]
          val result = connector.getPayments(regime, regNumber, pageSize, pageNo).futureValue

          result mustEqual expectedPaymentsResponse
        }
      }

      "must forward the correct regime and registration number in the URL" in {
        val otherRegime = "pbd"
        val otherRegNumber = "XWM00003102999"

        stubFor(
          get(urlEqualTo(s"/gambling/payments/$otherRegime/$otherRegNumber?pageSize=$pageSize&pageNo=$pageNo"))
            .willReturn(okJson(paymentsResponseJson))
        )

        val app = buildApp()
        running(app) {
          val connector = app.injector.instanceOf[GamblingConnector]
          val result = connector.getPayments(otherRegime, otherRegNumber, pageSize, pageNo).futureValue

          result mustEqual expectedPaymentsResponse
        }
      }

      "must forward custom pageSize and pageNo query parameters" in {
        val customPageSize = 5
        val customPageNo = 3

        stubFor(
          get(urlEqualTo(s"/gambling/payments/$regime/$regNumber?pageSize=$customPageSize&pageNo=$customPageNo"))
            .willReturn(okJson(paymentsResponseJson))
        )

        val app = buildApp()
        running(app) {
          val connector = app.injector.instanceOf[GamblingConnector]
          val result = connector.getPayments(regime, regNumber, customPageSize, customPageNo).futureValue

          result mustEqual expectedPaymentsResponse
        }
      }
    }

    "getActualRepayments" - {

      val actualRepaymentsResponseJson =
        s"""
           |{
           |  "periodStartDate": "2024-01-01",
           |  "periodEndDate": "2024-12-31",
           |  "total": 150.00,
           |  "totalRecords": 1,
           |  "items": [
           |    {
           |      "transactionDate": "2024-07-01",
           |      "amount": 150.00
           |    }
           |  ]
           |}
           |""".stripMargin

      val expectedActualRepaymentsResponse = ActualRepayments(
        periodStartDate = Some(LocalDate.of(2024, 1, 1)),
        periodEndDate   = Some(LocalDate.of(2024, 12, 31)),
        total           = BigDecimal("150.0"),
        totalRecords    = 1,
        items           = Seq(ActualRepaymentItem(LocalDate.of(2024, 7, 1), BigDecimal("150.0")))
      )

      "must return a deserialized ActualRepayments for a 200 response" in {
        stubFor(
          get(urlEqualTo(s"/gambling/actual-repayments/$regime/$regNumber?pageSize=$pageSize&pageNo=$pageNo"))
            .willReturn(okJson(actualRepaymentsResponseJson))
        )

        val app = buildApp()
        running(app) {
          val connector = app.injector.instanceOf[GamblingConnector]
          val result = connector.getActualRepayments(regime, regNumber, pageSize, pageNo).futureValue

          result mustEqual expectedActualRepaymentsResponse
        }
      }

      "must forward the correct regime and registration number in the URL" in {
        val otherRegime = "pbd"
        val otherRegNumber = "XWM00003102999"

        stubFor(
          get(urlEqualTo(s"/gambling/actual-repayments/$otherRegime/$otherRegNumber?pageSize=$pageSize&pageNo=$pageNo"))
            .willReturn(okJson(actualRepaymentsResponseJson))
        )

        val app = buildApp()
        running(app) {
          val connector = app.injector.instanceOf[GamblingConnector]
          val result = connector.getActualRepayments(otherRegime, otherRegNumber, pageSize, pageNo).futureValue

          result mustEqual expectedActualRepaymentsResponse
        }
      }

      "must forward custom pageSize and pageNo query parameters" in {
        val customPageSize = 5
        val customPageNo = 3

        stubFor(
          get(urlEqualTo(s"/gambling/actual-repayments/$regime/$regNumber?pageSize=$customPageSize&pageNo=$customPageNo"))
            .willReturn(okJson(actualRepaymentsResponseJson))
        )

        val app = buildApp()
        running(app) {
          val connector = app.injector.instanceOf[GamblingConnector]
          val result = connector.getActualRepayments(regime, regNumber, customPageSize, customPageNo).futureValue

          result mustEqual expectedActualRepaymentsResponse
        }
      }
    }

    "getRepaymentInterestRepaid" - {

      val repaymentInterestRepaidResponseJson =
        s"""
           |{
           |  "periodStartDate": "2024-01-01",
           |  "periodEndDate": "2024-12-31",
           |  "total": 45.60,
           |  "totalRecords": 1,
           |  "items": [
           |    {
           |      "transactionDate": "2024-07-01",
           |      "amount": 45.60
           |    }
           |  ]
           |}
           |""".stripMargin

      val expectedRepaymentInterestRepaidResponse = RepaymentInterestRepaid(
        periodStartDate = Some(LocalDate.of(2024, 1, 1)),
        periodEndDate   = Some(LocalDate.of(2024, 12, 31)),
        total           = BigDecimal("45.6"),
        totalRecords    = 1,
        items           = Seq(RepaymentInterestRepaidItem(LocalDate.of(2024, 7, 1), BigDecimal("45.6")))
      )

      "must return a deserialized RepaymentInterestRepaid for a 200 response" in {
        stubFor(
          get(urlEqualTo(s"/gambling/repayment-interest-repaid/$regime/$regNumber?pageSize=$pageSize&pageNo=$pageNo"))
            .willReturn(okJson(repaymentInterestRepaidResponseJson))
        )

        val app = buildApp()
        running(app) {
          val connector = app.injector.instanceOf[GamblingConnector]
          val result = connector.getRepaymentInterestRepaid(regime, regNumber, pageSize, pageNo).futureValue

          result mustEqual expectedRepaymentInterestRepaidResponse
        }
      }

      "must forward the correct regime and registration number in the URL" in {
        val otherRegime = "pbd"
        val otherRegNumber = "XWM00003102999"

        stubFor(
          get(urlEqualTo(s"/gambling/repayment-interest-repaid/$otherRegime/$otherRegNumber?pageSize=$pageSize&pageNo=$pageNo"))
            .willReturn(okJson(repaymentInterestRepaidResponseJson))
        )

        val app = buildApp()
        running(app) {
          val connector = app.injector.instanceOf[GamblingConnector]
          val result = connector.getRepaymentInterestRepaid(otherRegime, otherRegNumber, pageSize, pageNo).futureValue

          result mustEqual expectedRepaymentInterestRepaidResponse
        }
      }

      "must forward custom pageSize and pageNo query parameters" in {
        val customPageSize = 5
        val customPageNo = 3

        stubFor(
          get(urlEqualTo(s"/gambling/repayment-interest-repaid/$regime/$regNumber?pageSize=$customPageSize&pageNo=$customPageNo"))
            .willReturn(okJson(repaymentInterestRepaidResponseJson))
        )

        val app = buildApp()
        running(app) {
          val connector = app.injector.instanceOf[GamblingConnector]
          val result = connector.getRepaymentInterestRepaid(regime, regNumber, customPageSize, customPageNo).futureValue

          result mustEqual expectedRepaymentInterestRepaidResponse
        }
      }
    }

    "getRepaymentsSummary" - {

      val repaymentsSummaryResponseJson =
        s"""
           |{
           |  "periodStartDate": "2024-01-01",
           |  "periodEndDate": "2024-12-31",
           |  "actualRepaymentsAmount": 45.60,
           |  "repaymentsInterestRepaidAmount": -85.60,
           |  "total": -45.60
           |}
           |""".stripMargin

      val expectedRepaymentsSummaryResponse = RepaymentsSummary(
        periodStartDate                = Some(LocalDate.of(2024, 1, 1)),
        periodEndDate                  = Some(LocalDate.of(2024, 12, 31)),
        actualRepaymentsAmount         = BigDecimal("45.6"),
        repaymentsInterestRepaidAmount = BigDecimal("-85.6"),
        total                          = BigDecimal("-45.6")
      )

      "must return a deserialized RepaymentsSummary for a 200 response" in {
        stubFor(
          get(urlEqualTo(s"/gambling/repayment-summary/$regime/$regNumber"))
            .willReturn(okJson(repaymentsSummaryResponseJson))
        )

        val app = buildApp()
        running(app) {
          val connector = app.injector.instanceOf[GamblingConnector]
          val result = connector.getRepaymentsSummary(regime, regNumber).futureValue

          result mustEqual expectedRepaymentsSummaryResponse
        }
      }

      "must forward the correct regime and registration number in the URL" in {
        val otherRegime = "pbd"
        val otherRegNumber = "XWM00003102999"

        stubFor(
          get(urlEqualTo(s"/gambling/repayment-summary/$otherRegime/$otherRegNumber"))
            .willReturn(okJson(repaymentsSummaryResponseJson))
        )

        val app = buildApp()
        running(app) {
          val connector = app.injector.instanceOf[GamblingConnector]
          val result = connector.getRepaymentsSummary(otherRegime, otherRegNumber).futureValue

          result mustEqual expectedRepaymentsSummaryResponse
        }
      }
    }
  }
}
