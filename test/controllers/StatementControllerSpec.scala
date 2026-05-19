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
import models.reallocations.Reallocations
import models.returns.ReturnsSubmitted
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
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

  private val summary = Reallocations.Summary(
    periodStartDate = Option(LocalDate.of(2024, 1, 1)),
    periodEndDate   = Option(LocalDate.of(2024, 12, 31)),
    inTotal         = BigDecimal(45.60),
    outTotal        = BigDecimal(-55.60)
  )

  private val returnsSubmitted = ReturnsSubmitted(
    periodStartDate    = None,
    periodEndDate      = None,
    total              = Some(BigDecimal(300)),
    totalPeriodRecords = Some(3),
    amountDeclared     = Seq.empty
  )

  "Statement Controller" - {

    "must return OK and render the view with the regNumber from sessions and reallocations summary" in {
      val mockService = mock[GamblingService]
      when(mockService.getReallocationsSummary(any(), any())(any()))
        .thenReturn(Future.successful(summary))
      when(mockService.getReturnsSubmitted(any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(returnsSubmitted))

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
        val reallocationsTotal = summary.inTotal + summary.outTotal
        val expectedBalance = returnsTotal + reallocationsTotal
        contentAsString(result) mustEqual view(regNumber, returnsTotal, reallocationsTotal, expectedBalance)(request, messages(application)).toString
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
