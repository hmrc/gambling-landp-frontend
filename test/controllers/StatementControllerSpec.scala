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

import base.SpecBase
import models.SessionKeys
import models.assessments.Assessments
import models.payments.Payments
import models.penalties.Penalties
import models.reallocations.ReallocationsDetails
import models.repayments.RepaymentsSummary
import models.returns.ReturnsSubmitted
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.GamblingService
import views.html.AccountOverview

import java.time.LocalDate
import scala.concurrent.Future

class StatementControllerSpec extends SpecBase with MockitoSugar {

  private val regNumber = "XWM00003102200"
  private val regime = "mgd"

  private val reallocationsDetails = ReallocationsDetails(
    periodStartDate        = Option(LocalDate.of(2024, 1, 1)),
    periodEndDate          = Option(LocalDate.of(2024, 12, 31)),
    reallocationsInAmount  = BigDecimal(45.60),
    reallocationsOutAmount = BigDecimal(-55.60),
    total                  = BigDecimal(-10.00)
  )

  private val returnsSubmitted = ReturnsSubmitted(
    periodStartDate    = None,
    periodEndDate      = None,
    total              = Some(BigDecimal(300)),
    totalPeriodRecords = Some(3),
    amountDeclared     = Seq.empty
  )

  private val assessmentsWithoutReturns = Assessments(
    periodStartDate = None,
    periodEndDate   = None,
    total           = Some(BigDecimal(30.90)),
    totalRecords    = Some(1),
    items           = Seq.empty
  )

  private val otherAssessments = Assessments(
    periodStartDate = None,
    periodEndDate   = None,
    total           = Some(BigDecimal(-20.90)),
    totalRecords    = Some(1),
    items           = Seq.empty
  )

  private val penalties = Penalties(
    periodStartDate = None,
    periodEndDate   = None,
    total           = BigDecimal(-200),
    totalRecords    = 2,
    items           = Seq.empty
  )

  private val payments = Payments(
    periodStartDate = None,
    periodEndDate   = None,
    total           = BigDecimal(-291.64),
    totalRecords    = 3,
    items           = Seq.empty
  )

  private val repaymentsSummary = RepaymentsSummary(
    periodStartDate                = Option(LocalDate.of(2024, 1, 1)),
    periodEndDate                  = Option(LocalDate.of(2024, 12, 31)),
    actualRepaymentsAmount         = BigDecimal(35.30),
    repaymentsInterestRepaidAmount = BigDecimal(-55.60),
    total                          = BigDecimal(-20.30)
  )

  "Statement Controller" - {

    "must return OK and render the view with the regNumber from sessions and reallocations reallocationsDetails" in {
      val mockService = mock[GamblingService]
      when(mockService.getReallocationsDetails(any(), any())(any()))
        .thenReturn(Future.successful(reallocationsDetails))
      when(mockService.getReturnsSubmitted(any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(returnsSubmitted))
      when(mockService.getAssessmentsWithoutReturns(any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(assessmentsWithoutReturns))
      when(mockService.getOtherAssessments(any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(otherAssessments))
      when(mockService.getPenalties(any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(penalties))
      when(mockService.getPayments(any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(payments))
      when(mockService.getRepaymentsSummary(any(), any())(any()))
        .thenReturn(Future.successful(repaymentsSummary))

      val application = applicationBuilder(userAnswers = None)
        .overrides(bind[GamblingService].toInstance(mockService))
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.StatementController.onPageLoad().url)
          .withSession(SessionKeys.regNumber -> regNumber, SessionKeys.regime -> regime)

        val result = route(application, request).value

        val view = application.injector.instanceOf[AccountOverview]

        status(result) mustEqual OK

        val returnsTotal = returnsSubmitted.total.getOrElse(BigDecimal(0))
        val assessmentWithoutReturnsTotal = assessmentsWithoutReturns.total.getOrElse(BigDecimal(0))
        val otherAssessmentsTotal = otherAssessments.total.getOrElse(BigDecimal(0))
        val expectedBalance =
          returnsTotal + assessmentWithoutReturnsTotal + reallocationsDetails.total + otherAssessmentsTotal + penalties.total + payments.total + repaymentsSummary.total

        val body = contentAsString(result)
        body must include("<strong>Total</strong>")
        body must include("Payments")
        body mustEqual view(
          regNumber,
          returnsTotal,
          assessmentWithoutReturnsTotal,
          reallocationsDetails.total,
          otherAssessmentsTotal,
          penalties.total,
          payments.total,
          repaymentsSummary.total,
          expectedBalance
        )(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Unauthorised when regNumber is absent from the session" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, routes.StatementController.onPageLoad().url)
          .withSession(SessionKeys.regime -> regime)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised when regime is absent from the session" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, routes.StatementController.onPageLoad().url)
          .withSession(SessionKeys.regNumber -> regNumber)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }
  }
}
