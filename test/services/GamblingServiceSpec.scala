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
import models.assessments.{AssessmentItem, Assessments, Penalties, PenaltyItem}
import models.payments.{PaymentItem, Payments}
import models.reallocations.{ReallocationItem, Reallocations, ReallocationsDetails}
import models.repayments.{ActualRepaymentItem, ActualRepayments}
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
        items           = Seq(PaymentItem(LocalDate.of(2024, 7, 23), "2680", BigDecimal("-291.64")))
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
  }
}
