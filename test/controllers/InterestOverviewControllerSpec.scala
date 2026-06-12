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
import models.interest.InterestOverview
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.GamblingService

import java.time.LocalDate
import scala.concurrent.Future

class InterestOverviewControllerSpec extends SpecBase with MockitoSugar {

  private val regNumber = "XWM00003102200"

  val interestOverview = InterestOverview(
    periodStartDate         = Some(LocalDate.of(2024, 1, 1)),
    periodEndDate           = Some(LocalDate.of(2024, 12, 31)),
    interestAmount          = BigDecimal(-81.84),
    interestAccruingAmount  = BigDecimal(-25.76),
    repaymentInterestAmount = BigDecimal(41.23),
    total                   = BigDecimal(66.37)
  )

  "InterestOverviewController" - {

    def url = routes.InterestOverviewController.onPageLoad().url

    "must redirect to Unauthorised when regime is missing from session" in {
      val app = applicationBuilder().build()

      running(app) {
        val request = FakeRequest(GET, url).withSession(SessionKeys.regNumber -> regNumber)
        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised when regNumber is missing from session" in {
      val app = applicationBuilder().build()

      running(app) {
        val request = FakeRequest(GET, url).withSession(SessionKeys.regime -> "gbd")
        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to page not found when session contains an unrecognised regime" in {
      val app = applicationBuilder().build()

      running(app) {
        val request = FakeRequest(GET, url)
          .withSession(SessionKeys.regime -> "unknown", SessionKeys.regNumber -> regNumber)
        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.PageNotFoundController.onPageLoad().url
      }
    }

    "must return OK and render the heading for a valid regime and contains the expected messages in the content" in {
      val mockService = mock[GamblingService]
      when(mockService.getInterestOverview(any(), any())(any()))
        .thenReturn(Future.successful(interestOverview))

      val app = applicationBuilder()
        .overrides(bind[GamblingService].toInstance(mockService))
        .build()

      running(app) {
        val request = FakeRequest(GET, url)
          .withSession(SessionKeys.regime -> "gbd", SessionKeys.regNumber -> regNumber)
        val result = route(app, request).value

        status(result) mustEqual OK
        contentAsString(result) must include("Breakdown of interest")
        contentAsString(result) must include("The amount of unpaid interest on your account.")
      }
    }

    "must return OK and render the page with expected messages in the content when actual ALL interest amounts are 0" in {
      val emptySummary =
        interestOverview.copy(interestAmount          = BigDecimal(0),
                              interestAccruingAmount  = BigDecimal(0),
                              repaymentInterestAmount = BigDecimal(0),
                              total                   = BigDecimal(0)
                             )
      val mockService = mock[GamblingService]
      when(mockService.getInterestOverview(any(), any())(any()))
        .thenReturn(Future.successful(emptySummary))

      val app = applicationBuilder()
        .overrides(bind[GamblingService].toInstance(mockService))
        .build()

      running(app) {
        val request = FakeRequest(GET, url)
          .withSession(SessionKeys.regime -> "gbd", SessionKeys.regNumber -> regNumber)
        val result = route(app, request).value

        status(result) mustEqual OK
        contentAsString(result) must include("Breakdown of interest")
        contentAsString(result) must include("The amount of unpaid interest on your account.")
        contentAsString(result) must include("There is no unpaid interest on your account.")
        contentAsString(result) must not include "govuk-table"
        contentAsString(result) must not include "Interest"
        contentAsString(result) must not include "Accruing interest"
        contentAsString(result) must not include "Repayment interest"
      }
    }

    "must include the intro paragraph in the page content" in {
      val mockService = mock[GamblingService]
      when(mockService.getInterestOverview(any(), any())(any()))
        .thenReturn(Future.successful(interestOverview))

      val app = applicationBuilder()
        .overrides(bind[GamblingService].toInstance(mockService))
        .build()

      running(app) {
        val request = FakeRequest(GET, url)
          .withSession(SessionKeys.regime -> "gbd", SessionKeys.regNumber -> regNumber)
        val result = route(app, request).value

        status(result) mustEqual OK
        contentAsString(result) must include("The amount of unpaid interest on your account.")
      }
    }

    "must render a link to the 'Interest' page when the amount is non-zero" in {
      val mockService = mock[GamblingService]
      when(mockService.getInterestOverview(any(), any())(any())).thenReturn(Future.successful(interestOverview))

      val app = applicationBuilder()
        .overrides(bind[GamblingService].toInstance(mockService))
        .build()

      running(app) {
        val request = FakeRequest(GET, url)
          .withSession(SessionKeys.regime -> "gbd", SessionKeys.regNumber -> regNumber)
        val result = route(app, request).value

        status(result) mustEqual OK
        // contentAsString(result) must include(routes. Interest Controller.onPageLoad().url)   TODO !!!!!!!!!!!!!!
      }
    }

    "must render plain text (no link) for 'Interest' when the amount is zero" in {
      val zeroInterest = interestOverview.copy(interestAmount = BigDecimal(0))
      val mockService = mock[GamblingService]
      when(mockService.getInterestOverview(any(), any())(any()))
        .thenReturn(Future.successful(zeroInterest))

      val app = applicationBuilder()
        .overrides(bind[GamblingService].toInstance(mockService))
        .build()

      running(app) {
        val request = FakeRequest(GET, url)
          .withSession(SessionKeys.regime -> "gbd", SessionKeys.regNumber -> regNumber)
        val result = route(app, request).value

        status(result) mustEqual OK
        //        contentAsString(result) must not include routes.Interest  Controller.onPageLoad().url  TODO !!!!!!!!!!!!!1
      }
    }

    "must render a link to the 'Interest accruing' page when the amount is non-zero" in {
      val mockService = mock[GamblingService]
      when(mockService.getInterestOverview(any(), any())(any())).thenReturn(Future.successful(interestOverview))

      val app = applicationBuilder()
        .overrides(bind[GamblingService].toInstance(mockService))
        .build()

      running(app) {
        val request = FakeRequest(GET, url)
          .withSession(SessionKeys.regime -> "gbd", SessionKeys.regNumber -> regNumber)
        val result = route(app, request).value

        status(result) mustEqual OK
//        contentAsString(result) must include(routes. Interest accruing Controller.onPageLoad().url) TODO !!!!!!!!!!!!!!!1
      }
    }

    "must render plain text (no link) for 'Interest accruing' when the amount is zero" in {
      val zeroInterest = interestOverview.copy(interestAccruingAmount = BigDecimal(0))
      val mockService = mock[GamblingService]
      when(mockService.getInterestOverview(any(), any())(any()))
        .thenReturn(Future.successful(zeroInterest))

      val app = applicationBuilder()
        .overrides(bind[GamblingService].toInstance(mockService))
        .build()

      running(app) {
        val request = FakeRequest(GET, url)
          .withSession(SessionKeys.regime -> "gbd", SessionKeys.regNumber -> regNumber)
        val result = route(app, request).value

        status(result) mustEqual OK
//        contentAsString(result) must not include routes.Interest accruing Controller.onPageLoad().url  TODO !!!!!!!!!!!!!1
      }
    }

    "must render a link to the 'Repayment interest' page when the amount is non-zero for regime MGD" in {
      val mockService = mock[GamblingService]
      when(mockService.getInterestOverview(any(), any())(any())).thenReturn(Future.successful(interestOverview))

      val app = applicationBuilder()
        .overrides(bind[GamblingService].toInstance(mockService))
        .build()

      running(app) {
        val request = FakeRequest(GET, url)
          .withSession(SessionKeys.regime -> "mgd", SessionKeys.regNumber -> regNumber)
        val result = route(app, request).value

        status(result) mustEqual OK
        //        contentAsString(result) must include(routes. Repayment interest Controller.onPageLoad().url) TODO !!!!!!!!!!!!!!!1
      }
    }

    "must render plain text (no link) for 'Repayment interest' when the amount is zero for regime MGD" in {
      val zeroInterest = interestOverview.copy(repaymentInterestAmount = BigDecimal(0))
      val mockService = mock[GamblingService]
      when(mockService.getInterestOverview(any(), any())(any()))
        .thenReturn(Future.successful(zeroInterest))

      val app = applicationBuilder()
        .overrides(bind[GamblingService].toInstance(mockService))
        .build()

      running(app) {
        val request = FakeRequest(GET, url)
          .withSession(SessionKeys.regime -> "gbd", SessionKeys.regNumber -> regNumber)
        val result = route(app, request).value

        status(result) mustEqual OK
        //        contentAsString(result) must not include routes. Repayment interest Controller.onPageLoad().url  TODO !!!!!!!!!!!!!1
      }
    }

    "must support all valid regime codes" in {
      val regimes = Seq("gbd", "pbd", "rgd", "mgd")

      regimes.foreach { code =>
        val mockService = mock[GamblingService]
        when(mockService.getInterestOverview(any(), any())(any()))
          .thenReturn(Future.successful(interestOverview))

        val app = applicationBuilder()
          .overrides(bind[GamblingService].toInstance(mockService))
          .build()

        running(app) {
          val request = FakeRequest(GET, url)
            .withSession(SessionKeys.regime -> code, SessionKeys.regNumber -> regNumber)
          val result = route(app, request).value

          status(result) mustEqual OK
        }
      }
    }

    "Repayment interest not shown for regime codes != MGD" in {
      val regimesExcludingMGD = Seq("gbd", "pbd", "rgd")

      regimesExcludingMGD.foreach { code =>
        val mockService = mock[GamblingService]
        when(mockService.getInterestOverview(any(), any())(any()))
          .thenReturn(Future.successful(interestOverview))

        val app = applicationBuilder()
          .overrides(bind[GamblingService].toInstance(mockService))
          .build()

        running(app) {
          val request = FakeRequest(GET, url)
            .withSession(SessionKeys.regime -> code, SessionKeys.regNumber -> regNumber)
          val result = route(app, request).value

          status(result) mustEqual OK
          contentAsString(result) must not include "Repayment interest"
          contentAsString(result) must include("Interest")
          contentAsString(result) must include("Accruing interest")
        }
      }
    }

    "Repayment interest is shown for regime code MGD" in {
      val mockService = mock[GamblingService]
      when(mockService.getInterestOverview(any(), any())(any()))
        .thenReturn(Future.successful(interestOverview))

      val app = applicationBuilder()
        .overrides(bind[GamblingService].toInstance(mockService))
        .build()

      running(app) {
        val request = FakeRequest(GET, url)
          .withSession(SessionKeys.regime -> "mgd", SessionKeys.regNumber -> regNumber)
        val result = route(app, request).value

        status(result) mustEqual OK
        contentAsString(result) must include("Repayment interest")
        contentAsString(result) must include("Interest")
        contentAsString(result) must include("Accruing interest")
      }
    }
  }
}
