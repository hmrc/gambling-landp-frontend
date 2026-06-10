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
import models.{Regime, SessionKeys, StatementOverview}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.GamblingService
import uk.gov.hmrc.http.UpstreamErrorResponse
import views.html.StatementOverviewView

import java.time.LocalDate
import scala.concurrent.Future

class StatementControllerSpec extends SpecBase with MockitoSugar {

  private val regNumber = "XWM00003102200"
  private val regime = "mgd"

  private val statementOverview = StatementOverview(
    gtrPeriodStartDate = Option(LocalDate.of(2024, 1, 1)),
    gtrPeriodEndDate   = Option(LocalDate.of(2024, 12, 31)),
    total              = BigDecimal(100.00),
    balance            = BigDecimal(-191.94),
    amountDeclared     = BigDecimal(300),
    assessments        = BigDecimal(30.90),
    penalties          = BigDecimal(-200),
    adjustments        = BigDecimal(0),
    reallocations      = BigDecimal(-10.00),
    otherAssessments   = BigDecimal(-20.90),
    interest           = BigDecimal(0),
    payments           = BigDecimal(-291.64),
    repayments         = Some(BigDecimal(-20.30))
  )

  "Statement Controller" - {

    "must return OK and render the view with the statement overview data" in {
      val mockService = mock[GamblingService]
      when(mockService.getStatementOverview(any(), any())(any()))
        .thenReturn(Future.successful(statementOverview))

      val application = applicationBuilder(userAnswers = None)
        .overrides(bind[GamblingService].toInstance(mockService))
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.StatementController.onPageLoad().url)
          .withSession(SessionKeys.regNumber -> regNumber, SessionKeys.regime -> regime)

        val result = route(application, request).value

        val view = application.injector.instanceOf[StatementOverviewView]

        status(result) mustEqual OK

        val body = contentAsString(result)
        body must include("<strong>Total</strong>")
        body must include("Payments")
        body mustEqual view(
          regNumber,
          Regime.MGD,
          statementOverview
        )(
          request,
          messages(application)
        ).toString
      }
    }

    "must return NOT_FOUND when the backend returns 404" in {
      val mockService = mock[GamblingService]
      when(mockService.getStatementOverview(any(), any())(any()))
        .thenReturn(Future.failed(UpstreamErrorResponse("Statement not found", NOT_FOUND)))

      val application = applicationBuilder(userAnswers = None)
        .overrides(bind[GamblingService].toInstance(mockService))
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.StatementController.onPageLoad().url)
          .withSession(SessionKeys.regNumber -> regNumber, SessionKeys.regime -> regime)

        val result = route(application, request).value

        status(result) mustEqual NOT_FOUND
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
