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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.GamblingService

import java.time.LocalDate
import scala.concurrent.Future

class ReallocationsControllerSpec extends SpecBase with MockitoSugar {

  private val regNumber = "XWM00003102200"
  private val regime = "mgd"

  private val summary = Reallocations.Summary(
    periodStartDate = Option(LocalDate.of(2024, 1, 1)),
    periodEndDate   = Option(LocalDate.of(2024, 12, 31)),
    inTotal         = BigDecimal(45.60),
    outTotal        = BigDecimal(-55.60)
  )

  "ReallocationsController" - {

    def url = routes.ReallocationsController.onPageLoad().url

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
      when(mockService.getReallocationsSummary(any(), any())(any()))
        .thenReturn(Future.successful(summary))

      val app = applicationBuilder()
        .overrides(bind[GamblingService].toInstance(mockService))
        .build()

      running(app) {
        val request = FakeRequest(GET, url)
          .withSession(SessionKeys.regime -> "gbd", SessionKeys.regNumber -> regNumber)
        val result = route(app, request).value

        status(result) mustEqual OK
        contentAsString(result) must include("Reallocations")
        contentAsString(result) must include("Money moved in or out of your account, or transferred to another tax.")
      }
    }

    "must return OK and render the page with expected messages in the content when rellocations in/out are 0" in {
      val emptySummary = summary.copy(inTotal = BigDecimal(0), outTotal = BigDecimal(0))
      val mockService = mock[GamblingService]
      when(mockService.getReallocationsSummary(any(), any())(any()))
        .thenReturn(Future.successful(emptySummary))

      val app = applicationBuilder()
        .overrides(bind[GamblingService].toInstance(mockService))
        .build()

      running(app) {
        val request = FakeRequest(GET, url)
          .withSession(SessionKeys.regime -> "gbd", SessionKeys.regNumber -> regNumber)
        val result = route(app, request).value

        status(result) mustEqual OK
        contentAsString(result) must include("Reallocations")
        contentAsString(result) must include("Money moved in or out of your account, or transferred to another tax.")
        contentAsString(result) must include("There are no reallocations for this period.")
        contentAsString(result) must not include "govuk-table"
        contentAsString(result) must not include "Reallocations In"
        contentAsString(result) must not include "Reallocations Out"
      }
    }

    "must include the intro paragraph in the page content" in {
      val mockService = mock[GamblingService]
      when(mockService.getReallocationsSummary(any(), any())(any()))
        .thenReturn(Future.successful(summary))

      val app = applicationBuilder()
        .overrides(bind[GamblingService].toInstance(mockService))
        .build()

      running(app) {
        val request = FakeRequest(GET, url)
          .withSession(SessionKeys.regime -> "gbd", SessionKeys.regNumber -> regNumber)
        val result = route(app, request).value

        status(result) mustEqual OK
        contentAsString(result) must include("Money moved in or out of your account, or transferred to another tax.")
      }
    }

    "must support all valid regime codes" in {
      val regimes = Seq("gbd", "pbd", "rgd", "mgd")

      regimes.foreach { code =>
        val mockService = mock[GamblingService]
        when(mockService.getReallocationsSummary(any(), any())(any()))
          .thenReturn(Future.successful(summary))

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
  }
}
