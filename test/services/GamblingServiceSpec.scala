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

package services

import base.SpecBase
import connectors.GamblingConnector
import models.StatementOverview
import models.assessments.{AssessmentItem, Assessments}
import models.interest.{InterestDrilldown, InterestDrilldownItem, InterestOverview}
import models.payments.{PaymentItem, Payments}
import models.penalties.{Penalties, PenaltyItem}
import models.reallocations.{ReallocationItem, Reallocations, ReallocationsDetails}
import models.repayments.{ActualRepaymentItem, ActualRepayments, RepaymentInterestRepaid, RepaymentInterestRepaidItem, RepaymentsSummary}
import models.returns.{AmountDeclared, ReturnsSubmitted}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import scala.concurrent.Future

class GamblingServiceSpec extends SpecBase with MockitoSugar {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val regime = "gbd"
  private val regNumber = "XWM00003102200"
  private val pageSize = 10
  private val pageNo = 1

  private val returnsResponse = ReturnsSubmitted(
    periodStartDate    = Some(LocalDate.of(2024, 1, 1)),
    periodEndDate      = Some(LocalDate.of(2024, 12, 31)),
    amountDeclared     = Seq(AmountDeclared(Some(1), Some(LocalDate.of(2024, 1, 1)), Some(LocalDate.of(2024, 3, 31)), Some(BigDecimal("1000.50")))),
    total              = Some(BigDecimal("1000.50")),
    totalPeriodRecords = Some(1)
  )

  "GamblingService" - {

    "getReturnsSubmitted" - {

      "must delegate to the connector with the correct arguments and return its result" in {
        val mockConnector = mock[GamblingConnector]
        when(mockConnector.getReturnsSubmitted(eqTo(regime), eqTo(regNumber), eqTo(pageSize), eqTo(pageNo))(using any[HeaderCarrier]()))
          .thenReturn(Future.successful(returnsResponse))

        val service = new GamblingService(mockConnector)
        val result = service.getReturnsSubmitted(regime, regNumber, pageSize, pageNo).futureValue

        result mustEqual returnsResponse
      }

      "must delegate with the correct regime code when a different regime is provided" in {
        val mockConnector = mock[GamblingConnector]
        val otherRegime = "mgd"
        when(mockConnector.getReturnsSubmitted(eqTo(otherRegime), eqTo(regNumber), eqTo(pageSize), eqTo(pageNo))(using any[HeaderCarrier]()))
          .thenReturn(Future.successful(returnsResponse))

        val service = new GamblingService(mockConnector)
        val result = service.getReturnsSubmitted(otherRegime, regNumber, pageSize, pageNo).futureValue

        result mustEqual returnsResponse
      }

      "must propagate failures from the connector" in {
        val mockConnector = mock[GamblingConnector]
        val exception = new RuntimeException("upstream failure")
        when(mockConnector.getReturnsSubmitted(eqTo(regime), eqTo(regNumber), eqTo(pageSize), eqTo(pageNo))(using any[HeaderCarrier]()))
          .thenReturn(Future.failed(exception))

        val service = new GamblingService(mockConnector)
        val result = service.getReturnsSubmitted(regime, regNumber, pageSize, pageNo).failed.futureValue

        result mustEqual exception
      }
    }

    "getReallocationsIn" - {

      val reallocationsResponse = Reallocations(
        periodStartDate = Some(LocalDate.of(2024, 1, 1)),
        periodEndDate   = Some(LocalDate.of(2024, 12, 31)),
        total           = Some(BigDecimal("30.80")),
        totalRecords    = Some(1),
        items           = Seq(ReallocationItem(Some(LocalDate.of(2024, 7, 1)), Some(BigDecimal("30.80"))))
      )

      "must delegate to the connector with the correct arguments and return its result" in {
        val mockConnector = mock[GamblingConnector]
        when(mockConnector.getReallocationsIn(eqTo(regime), eqTo(regNumber), eqTo(pageSize), eqTo(pageNo))(using any[HeaderCarrier]()))
          .thenReturn(Future.successful(reallocationsResponse))

        val service = new GamblingService(mockConnector)
        val result = service.getReallocationsIn(regime, regNumber, pageSize, pageNo).futureValue

        result mustEqual reallocationsResponse
      }

      "must delegate with the correct regime code when a different regime is provided" in {
        val mockConnector = mock[GamblingConnector]
        val otherRegime = "mgd"
        when(mockConnector.getReallocationsIn(eqTo(otherRegime), eqTo(regNumber), eqTo(pageSize), eqTo(pageNo))(using any[HeaderCarrier]()))
          .thenReturn(Future.successful(reallocationsResponse))

        val service = new GamblingService(mockConnector)
        val result = service.getReallocationsIn(otherRegime, regNumber, pageSize, pageNo).futureValue

        result mustEqual reallocationsResponse
      }

      "must propagate failures from the connector" in {
        val mockConnector = mock[GamblingConnector]
        val exception = new RuntimeException("upstream failure")
        when(mockConnector.getReallocationsIn(eqTo(regime), eqTo(regNumber), eqTo(pageSize), eqTo(pageNo))(using any[HeaderCarrier]()))
          .thenReturn(Future.failed(exception))

        val service = new GamblingService(mockConnector)
        val result = service.getReallocationsIn(regime, regNumber, pageSize, pageNo).failed.futureValue

        result mustEqual exception
      }
    }

    "getReallocationsOut" - {

      val reallocationsOutResponse = Reallocations(
        periodStartDate = Some(LocalDate.of(2024, 1, 1)),
        periodEndDate   = Some(LocalDate.of(2024, 12, 31)),
        total           = Some(BigDecimal("45.60")),
        totalRecords    = Some(1),
        items           = Seq(ReallocationItem(Some(LocalDate.of(2024, 8, 1)), Some(BigDecimal("45.60"))))
      )

      "must delegate to the connector with the correct arguments and return its result" in {
        val mockConnector = mock[GamblingConnector]
        when(mockConnector.getReallocationsOut(eqTo(regime), eqTo(regNumber), eqTo(pageSize), eqTo(pageNo))(using any[HeaderCarrier]()))
          .thenReturn(Future.successful(reallocationsOutResponse))

        val service = new GamblingService(mockConnector)
        val result = service.getReallocationsOut(regime, regNumber, pageSize, pageNo).futureValue

        result mustEqual reallocationsOutResponse
      }

      "must delegate with the correct regime code when a different regime is provided" in {
        val mockConnector = mock[GamblingConnector]
        val otherRegime = "mgd"
        when(mockConnector.getReallocationsOut(eqTo(otherRegime), eqTo(regNumber), eqTo(pageSize), eqTo(pageNo))(using any[HeaderCarrier]()))
          .thenReturn(Future.successful(reallocationsOutResponse))

        val service = new GamblingService(mockConnector)
        val result = service.getReallocationsOut(otherRegime, regNumber, pageSize, pageNo).futureValue

        result mustEqual reallocationsOutResponse
      }

      "must propagate failures from the connector" in {
        val mockConnector = mock[GamblingConnector]
        val exception = new RuntimeException("upstream failure")
        when(mockConnector.getReallocationsOut(eqTo(regime), eqTo(regNumber), eqTo(pageSize), eqTo(pageNo))(using any[HeaderCarrier]()))
          .thenReturn(Future.failed(exception))

        val service = new GamblingService(mockConnector)
        val result = service.getReallocationsOut(regime, regNumber, pageSize, pageNo).failed.futureValue

        result mustEqual exception
      }
    }

    "getAssessmentsWithoutReturns" - {

      val assessmentsWithoutReturnsResponse = Assessments(
        periodStartDate = Some(LocalDate.of(2024, 1, 1)),
        periodEndDate   = Some(LocalDate.of(2024, 12, 31)),
        total           = Some(BigDecimal("45.60")),
        totalRecords    = Some(1),
        items = Seq(
          AssessmentItem(Some(LocalDate.of(2024, 8, 1)), Some(LocalDate.of(2024, 7, 1)), Some(LocalDate.of(2024, 9, 1)), Some(BigDecimal("45.60")))
        )
      )

      "must delegate to the connector with the correct arguments and return its result" in {
        val mockConnector = mock[GamblingConnector]
        when(mockConnector.getAssessmentsWithoutReturns(eqTo(regime), eqTo(regNumber), eqTo(pageSize), eqTo(pageNo))(using any[HeaderCarrier]()))
          .thenReturn(Future.successful(assessmentsWithoutReturnsResponse))

        val service = new GamblingService(mockConnector)
        val result = service.getAssessmentsWithoutReturns(regime, regNumber, pageSize, pageNo).futureValue

        result mustEqual assessmentsWithoutReturnsResponse
      }

      "must delegate with the correct regime code when a different regime is provided" in {
        val mockConnector = mock[GamblingConnector]
        val otherRegime = "mgd"
        when(mockConnector.getAssessmentsWithoutReturns(eqTo(otherRegime), eqTo(regNumber), eqTo(pageSize), eqTo(pageNo))(using any[HeaderCarrier]()))
          .thenReturn(Future.successful(assessmentsWithoutReturnsResponse))

        val service = new GamblingService(mockConnector)
        val result = service.getAssessmentsWithoutReturns(otherRegime, regNumber, pageSize, pageNo).futureValue

        result mustEqual assessmentsWithoutReturnsResponse
      }

      "must propagate failures from the connector" in {
        val mockConnector = mock[GamblingConnector]
        val exception = new RuntimeException("upstream failure")
        when(mockConnector.getAssessmentsWithoutReturns(eqTo(regime), eqTo(regNumber), eqTo(pageSize), eqTo(pageNo))(using any[HeaderCarrier]()))
          .thenReturn(Future.failed(exception))

        val service = new GamblingService(mockConnector)
        val result = service.getAssessmentsWithoutReturns(regime, regNumber, pageSize, pageNo).failed.futureValue

        result mustEqual exception
      }
    }

    "getOtherAssessments" - {

      val otherAssessmentsResponse = Assessments(
        periodStartDate = Some(LocalDate.of(2024, 1, 1)),
        periodEndDate   = Some(LocalDate.of(2024, 12, 31)),
        total           = Some(BigDecimal("45.60")),
        totalRecords    = Some(1),
        items = Seq(
          AssessmentItem(Some(LocalDate.of(2024, 8, 1)), Some(LocalDate.of(2024, 7, 1)), Some(LocalDate.of(2024, 9, 1)), Some(BigDecimal("45.60")))
        )
      )

      "must delegate to the connector with the correct arguments and return its result" in {
        val mockConnector = mock[GamblingConnector]
        when(mockConnector.getOtherAssessments(eqTo(regime), eqTo(regNumber), eqTo(pageSize), eqTo(pageNo))(using any[HeaderCarrier]()))
          .thenReturn(Future.successful(otherAssessmentsResponse))

        val service = new GamblingService(mockConnector)
        val result = service.getOtherAssessments(regime, regNumber, pageSize, pageNo).futureValue

        result mustEqual otherAssessmentsResponse
      }

      "must delegate with the correct regime code when a different regime is provided" in {
        val mockConnector = mock[GamblingConnector]
        val otherRegime = "mgd"
        when(mockConnector.getOtherAssessments(eqTo(otherRegime), eqTo(regNumber), eqTo(pageSize), eqTo(pageNo))(using any[HeaderCarrier]()))
          .thenReturn(Future.successful(otherAssessmentsResponse))

        val service = new GamblingService(mockConnector)
        val result = service.getOtherAssessments(otherRegime, regNumber, pageSize, pageNo).futureValue

        result mustEqual otherAssessmentsResponse
      }

      "must propagate failures from the connector" in {
        val mockConnector = mock[GamblingConnector]
        val exception = new RuntimeException("upstream failure")
        when(mockConnector.getOtherAssessments(eqTo(regime), eqTo(regNumber), eqTo(pageSize), eqTo(pageNo))(using any[HeaderCarrier]()))
          .thenReturn(Future.failed(exception))

        val service = new GamblingService(mockConnector)
        val result = service.getOtherAssessments(regime, regNumber, pageSize, pageNo).failed.futureValue

        result mustEqual exception
      }
    }

    "getPenalties" - {

      val penaltiesResponse = Penalties(
        periodStartDate = Some(LocalDate.of(2024, 1, 1)),
        periodEndDate   = Some(LocalDate.of(2024, 12, 31)),
        total           = BigDecimal("-500.00"),
        totalRecords    = 1,
        items = Seq(
          PenaltyItem(LocalDate.of(2024, 8, 1), 1980, BigDecimal("-500.00"), LocalDate.of(2024, 1, 1), LocalDate.of(2024, 3, 31))
        )
      )

      "must delegate to the connector with the correct arguments and return its result" in {
        val mockConnector = mock[GamblingConnector]
        when(mockConnector.getPenalties(eqTo(regime), eqTo(regNumber), eqTo(pageSize), eqTo(pageNo))(using any[HeaderCarrier]()))
          .thenReturn(Future.successful(penaltiesResponse))

        val service = new GamblingService(mockConnector)
        val result = service.getPenalties(regime, regNumber, pageSize, pageNo).futureValue

        result mustEqual penaltiesResponse
      }

      "must delegate with the correct regime code when a different regime is provided" in {
        val mockConnector = mock[GamblingConnector]
        val otherRegime = "mgd"
        when(mockConnector.getPenalties(eqTo(otherRegime), eqTo(regNumber), eqTo(pageSize), eqTo(pageNo))(using any[HeaderCarrier]()))
          .thenReturn(Future.successful(penaltiesResponse))

        val service = new GamblingService(mockConnector)
        val result = service.getPenalties(otherRegime, regNumber, pageSize, pageNo).futureValue

        result mustEqual penaltiesResponse
      }

      "must propagate failures from the connector" in {
        val mockConnector = mock[GamblingConnector]
        val exception = new RuntimeException("upstream failure")
        when(mockConnector.getPenalties(eqTo(regime), eqTo(regNumber), eqTo(pageSize), eqTo(pageNo))(using any[HeaderCarrier]()))
          .thenReturn(Future.failed(exception))

        val service = new GamblingService(mockConnector)
        val result = service.getPenalties(regime, regNumber, pageSize, pageNo).failed.futureValue

        result mustEqual exception
      }
    }

    "getPayments" - {

      val paymentsResponse = Payments(
        periodStartDate = Some(LocalDate.of(2023, 11, 1)),
        periodEndDate   = Some(LocalDate.of(2025, 1, 27)),
        total           = BigDecimal("-291.64"),
        totalRecords    = 1,
        items           = Seq(PaymentItem(LocalDate.of(2024, 7, 23), "E", BigDecimal("-291.64")))
      )

      "must delegate to the connector with the correct arguments and return its result" in {
        val mockConnector = mock[GamblingConnector]
        when(mockConnector.getPayments(eqTo(regime), eqTo(regNumber), eqTo(pageSize), eqTo(pageNo))(using any[HeaderCarrier]()))
          .thenReturn(Future.successful(paymentsResponse))

        val service = new GamblingService(mockConnector)
        val result = service.getPayments(regime, regNumber, pageSize, pageNo).futureValue

        result mustEqual paymentsResponse
      }

      "must delegate with the correct regime code when a different regime is provided" in {
        val mockConnector = mock[GamblingConnector]
        val otherRegime = "mgd"
        when(mockConnector.getPayments(eqTo(otherRegime), eqTo(regNumber), eqTo(pageSize), eqTo(pageNo))(using any[HeaderCarrier]()))
          .thenReturn(Future.successful(paymentsResponse))

        val service = new GamblingService(mockConnector)
        val result = service.getPayments(otherRegime, regNumber, pageSize, pageNo).futureValue

        result mustEqual paymentsResponse
      }

      "must propagate failures from the connector" in {
        val mockConnector = mock[GamblingConnector]
        val exception = new RuntimeException("upstream failure")
        when(mockConnector.getPayments(eqTo(regime), eqTo(regNumber), eqTo(pageSize), eqTo(pageNo))(using any[HeaderCarrier]()))
          .thenReturn(Future.failed(exception))

        val service = new GamblingService(mockConnector)
        val result = service.getPayments(regime, regNumber, pageSize, pageNo).failed.futureValue

        result mustEqual exception
      }
    }

    "getActualRepayments" - {

      val actualRepaymentsResponse = ActualRepayments(
        periodStartDate = Some(LocalDate.of(2024, 1, 1)),
        periodEndDate   = Some(LocalDate.of(2024, 12, 31)),
        total           = BigDecimal("150.00"),
        totalRecords    = 1,
        items           = Seq(ActualRepaymentItem(LocalDate.of(2024, 7, 1), BigDecimal("150.00")))
      )

      "must delegate to the connector with the correct arguments and return its result" in {
        val mockConnector = mock[GamblingConnector]
        when(mockConnector.getActualRepayments(eqTo(regime), eqTo(regNumber), eqTo(pageSize), eqTo(pageNo))(using any[HeaderCarrier]()))
          .thenReturn(Future.successful(actualRepaymentsResponse))

        val service = new GamblingService(mockConnector)
        val result = service.getActualRepayments(regime, regNumber, pageSize, pageNo).futureValue

        result mustEqual actualRepaymentsResponse
      }

      "must delegate with the correct regime code when a different regime is provided" in {
        val mockConnector = mock[GamblingConnector]
        val otherRegime = "mgd"
        when(mockConnector.getActualRepayments(eqTo(otherRegime), eqTo(regNumber), eqTo(pageSize), eqTo(pageNo))(using any[HeaderCarrier]()))
          .thenReturn(Future.successful(actualRepaymentsResponse))

        val service = new GamblingService(mockConnector)
        val result = service.getActualRepayments(otherRegime, regNumber, pageSize, pageNo).futureValue

        result mustEqual actualRepaymentsResponse
      }

      "must propagate failures from the connector" in {
        val mockConnector = mock[GamblingConnector]
        val exception = new RuntimeException("upstream failure")
        when(mockConnector.getActualRepayments(eqTo(regime), eqTo(regNumber), eqTo(pageSize), eqTo(pageNo))(using any[HeaderCarrier]()))
          .thenReturn(Future.failed(exception))

        val service = new GamblingService(mockConnector)
        val result = service.getActualRepayments(regime, regNumber, pageSize, pageNo).failed.futureValue

        result mustEqual exception
      }
    }

    "getReallocationsDetails" - {

      val reallocationsDetails = ReallocationsDetails(
        periodStartDate        = Option(LocalDate.of(2024, 1, 1)),
        periodEndDate          = Option(LocalDate.of(2024, 12, 31)),
        reallocationsInAmount  = BigDecimal(45.60),
        reallocationsOutAmount = BigDecimal(-55.60),
        total                  = BigDecimal(-10.00)
      )

      "must fetch reallocations details" in {
        val mockConnector = mock[GamblingConnector]
        when(mockConnector.getReallocationsDetails(eqTo(regime), eqTo(regNumber))(using any[HeaderCarrier]()))
          .thenReturn(Future.successful(reallocationsDetails))

        val service = new GamblingService(mockConnector)
        val result = service.getReallocationsDetails(regime, regNumber).futureValue

        result mustEqual reallocationsDetails
      }
    }

    "getRepaymentInterestRepaid" - {

      val repaymentInterestRepaidResponse = RepaymentInterestRepaid(
        periodStartDate = Some(LocalDate.of(2024, 1, 1)),
        periodEndDate   = Some(LocalDate.of(2024, 12, 31)),
        total           = BigDecimal("45.60"),
        totalRecords    = 1,
        items           = Seq(RepaymentInterestRepaidItem(LocalDate.of(2024, 7, 1), BigDecimal("45.60")))
      )

      "must delegate to the connector with the correct arguments and return its result" in {
        val mockConnector = mock[GamblingConnector]
        when(mockConnector.getRepaymentInterestRepaid(eqTo(regime), eqTo(regNumber), eqTo(pageSize), eqTo(pageNo))(using any[HeaderCarrier]()))
          .thenReturn(Future.successful(repaymentInterestRepaidResponse))

        val service = new GamblingService(mockConnector)
        val result = service.getRepaymentInterestRepaid(regime, regNumber, pageSize, pageNo).futureValue

        result mustEqual repaymentInterestRepaidResponse
      }

      "must delegate with the correct regime code when a different regime is provided" in {
        val mockConnector = mock[GamblingConnector]
        val otherRegime = "mgd"
        when(mockConnector.getRepaymentInterestRepaid(eqTo(otherRegime), eqTo(regNumber), eqTo(pageSize), eqTo(pageNo))(using any[HeaderCarrier]()))
          .thenReturn(Future.successful(repaymentInterestRepaidResponse))

        val service = new GamblingService(mockConnector)
        val result = service.getRepaymentInterestRepaid(otherRegime, regNumber, pageSize, pageNo).futureValue

        result mustEqual repaymentInterestRepaidResponse
      }

      "must propagate failures from the connector" in {
        val mockConnector = mock[GamblingConnector]
        val exception = new RuntimeException("upstream failure")
        when(mockConnector.getRepaymentInterestRepaid(eqTo(regime), eqTo(regNumber), eqTo(pageSize), eqTo(pageNo))(using any[HeaderCarrier]()))
          .thenReturn(Future.failed(exception))

        val service = new GamblingService(mockConnector)
        val result = service.getRepaymentInterestRepaid(regime, regNumber, pageSize, pageNo).failed.futureValue

        result mustEqual exception
      }
    }

    "getStatementOverview" - {

      val statementOverviewResponse = StatementOverview(
        gtrPeriodStartDate = Some(LocalDate.of(2024, 1, 1)),
        gtrPeriodEndDate   = Some(LocalDate.of(2024, 12, 31)),
        total              = BigDecimal("-700.19"),
        balance            = BigDecimal("-200.00"),
        amountDeclared     = BigDecimal("-180.00"),
        assessments        = BigDecimal("-290.80"),
        penalties          = BigDecimal("-109.80"),
        adjustments        = BigDecimal("10.30"),
        reallocations      = BigDecimal("-9.20"),
        otherAssessments   = BigDecimal("-20.90"),
        interest           = BigDecimal("-109.80"),
        payments           = BigDecimal("100.21"),
        repayments         = Some(BigDecimal("109.80"))
      )

      "must delegate to the connector with the correct arguments and return its result" in {
        val mockConnector = mock[GamblingConnector]
        when(mockConnector.getStatementOverview(eqTo(regime), eqTo(regNumber))(using any[HeaderCarrier]()))
          .thenReturn(Future.successful(statementOverviewResponse))

        val service = new GamblingService(mockConnector)
        val result = service.getStatementOverview(regime, regNumber).futureValue

        result mustEqual statementOverviewResponse
      }

      "must delegate with the correct regime code when a different regime is provided" in {
        val mockConnector = mock[GamblingConnector]
        val otherRegime = "mgd"
        when(mockConnector.getStatementOverview(eqTo(otherRegime), eqTo(regNumber))(using any[HeaderCarrier]()))
          .thenReturn(Future.successful(statementOverviewResponse))

        val service = new GamblingService(mockConnector)
        val result = service.getStatementOverview(otherRegime, regNumber).futureValue

        result mustEqual statementOverviewResponse
      }

      "must propagate failures from the connector" in {
        val mockConnector = mock[GamblingConnector]
        val exception = new RuntimeException("upstream failure")
        when(mockConnector.getStatementOverview(eqTo(regime), eqTo(regNumber))(using any[HeaderCarrier]()))
          .thenReturn(Future.failed(exception))

        val service = new GamblingService(mockConnector)
        val result = service.getStatementOverview(regime, regNumber).failed.futureValue

        result mustEqual exception
      }
    }

    "getRepaymentsSummary" - {

      val repaymentsSummary = RepaymentsSummary(
        periodStartDate                = Some(LocalDate.of(2024, 1, 1)),
        periodEndDate                  = Some(LocalDate.of(2024, 12, 31)),
        actualRepaymentsAmount         = BigDecimal(71.84),
        repaymentsInterestRepaidAmount = BigDecimal(-35.76),
        total                          = BigDecimal(36.08)
      )

      "must fetch repayments summary" in {
        val mockConnector = mock[GamblingConnector]
        when(mockConnector.getRepaymentsSummary(eqTo(regime), eqTo(regNumber))(using any[HeaderCarrier]()))
          .thenReturn(Future.successful(repaymentsSummary))

        val service = new GamblingService(mockConnector)
        val result = service.getRepaymentsSummary(regime, regNumber).futureValue

        result mustEqual repaymentsSummary
      }
    }

    "getInterestDrilldown" - {

      val interestId = "INT-001"

      val interestDrilldownResponse = InterestDrilldown(
        periodStartDate = Some(LocalDate.of(2024, 1, 1)),
        periodEndDate   = Some(LocalDate.of(2024, 12, 31)),
        total           = BigDecimal("123.45"),
        totalRecords    = 1,
        descriptionCode = 2650,
        items = Seq(
          InterestDrilldownItem(
            interestOn = BigDecimal("1000.00"),
            dateFrom   = LocalDate.of(2024, 1, 1),
            dateTo     = LocalDate.of(2024, 3, 31),
            noOfDays   = BigDecimal(90),
            rate       = BigDecimal("2.5"),
            amount     = BigDecimal("123.45")
          )
        )
      )

      "must delegate to the connector with the correct arguments and return its result" in {
        val mockConnector = mock[GamblingConnector]
        when(
          mockConnector.getInterestDrilldown(eqTo(regime), eqTo(regNumber), eqTo(interestId), eqTo(pageSize), eqTo(pageNo))(using
            any[HeaderCarrier]()
          )
        )
          .thenReturn(Future.successful(interestDrilldownResponse))

        val service = new GamblingService(mockConnector)
        val result = service.getInterestDrilldown(regime, regNumber, interestId, pageSize, pageNo).futureValue

        result mustEqual interestDrilldownResponse
      }

      "must delegate with the correct regime code when a different regime is provided" in {
        val mockConnector = mock[GamblingConnector]
        val otherRegime = "mgd"
        when(
          mockConnector.getInterestDrilldown(eqTo(otherRegime), eqTo(regNumber), eqTo(interestId), eqTo(pageSize), eqTo(pageNo))(using
            any[HeaderCarrier]()
          )
        )
          .thenReturn(Future.successful(interestDrilldownResponse))

        val service = new GamblingService(mockConnector)
        val result = service.getInterestDrilldown(otherRegime, regNumber, interestId, pageSize, pageNo).futureValue

        result mustEqual interestDrilldownResponse
      }

      "must propagate failures from the connector" in {
        val mockConnector = mock[GamblingConnector]
        val exception = new RuntimeException("upstream failure")
        when(
          mockConnector.getInterestDrilldown(eqTo(regime), eqTo(regNumber), eqTo(interestId), eqTo(pageSize), eqTo(pageNo))(using
            any[HeaderCarrier]()
          )
        )
          .thenReturn(Future.failed(exception))

        val service = new GamblingService(mockConnector)
        val result = service.getInterestDrilldown(regime, regNumber, interestId, pageSize, pageNo).failed.futureValue

        result mustEqual exception
      }
    }

    "getInterestOverview" - {

      val interestOverview = InterestOverview(
        periodStartDate         = Some(LocalDate.of(2024, 1, 1)),
        periodEndDate           = Some(LocalDate.of(2024, 12, 31)),
        interestAmount          = BigDecimal(-81.84),
        interestAccruingAmount  = BigDecimal(-25.76),
        repaymentInterestAmount = BigDecimal(41.23),
        total                   = BigDecimal(66.37)
      )

      "must fetch interest overview" in {
        val mockConnector = mock[GamblingConnector]
        when(mockConnector.getInterestOverview(eqTo(regime), eqTo(regNumber))(using any[HeaderCarrier]()))
          .thenReturn(Future.successful(interestOverview))

        val service = new GamblingService(mockConnector)
        val result = service.getInterestOverview(regime, regNumber).futureValue

        result mustEqual interestOverview
      }
    }
  }
}
