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
import models.interest.{InterestDrilldown, InterestDrilldownItem}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.GamblingService

import java.time.LocalDate
import scala.concurrent.Future

class InterestDrilldownControllerSpec extends SpecBase with MockitoSugar {

  private val regNumber = "XWM00003102200"

  val interestDrilldown: InterestDrilldown = InterestDrilldown(
    periodStartDate = Some(LocalDate.of(2024, 1, 1)),
    periodEndDate   = Some(LocalDate.of(2024, 12, 31)),
    total           = BigDecimal(123.45),
    totalRecords    = 1,
    descriptionCode = 2650,
    items = Seq(
      InterestDrilldownItem(
        interestOn = BigDecimal(1000.00),
        dateFrom   = LocalDate.of(2024, 1, 1),
        dateTo     = LocalDate.of(2024, 3, 31),
        noOfDays   = BigDecimal(90),
        rate       = BigDecimal(2.5),
        amount     = BigDecimal(123.45)
      )
    )
  )

  private val multiPageDetails = interestDrilldown.copy(totalRecords = 25)

  "InterestDrilldownController" - {

    def url = routes.InterestDrilldownController.onPageLoad("INT-001").url

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
      s"must render the heading and paragraph for description code $code ($label)" in {
        val mockService = mock[GamblingService]
        when(mockService.getInterestDrilldown(any(), any(), any(), any(), any())(any()))
          .thenReturn(Future.successful(interestDrilldown.copy(descriptionCode = code)))

        val app = applicationBuilder()
          .overrides(bind[GamblingService].toInstance(mockService))
          .build()

        running(app) {
          val request = FakeRequest(GET, url)
            .withSession(SessionKeys.regime -> "gbd", SessionKeys.regNumber -> regNumber)
          val result = route(app, request).value

          status(result) mustEqual OK
          contentAsString(result) must include(s"Interest on [$label]")
          contentAsString(result) must include(s"The amount of unpaid interest on [$label].")
          contentAsString(result) must include("govuk-table")
        }
      }
    }

    "must not render pagination when there is only one page" in {
      val mockService = mock[GamblingService]
      when(mockService.getInterestDrilldown(any(), any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(interestDrilldown))

      val app = applicationBuilder()
        .overrides(bind[GamblingService].toInstance(mockService))
        .build()

      running(app) {
        val request = FakeRequest(GET, url)
          .withSession(SessionKeys.regime -> "gbd", SessionKeys.regNumber -> regNumber)
        val result = route(app, request).value

        status(result) mustEqual OK
        contentAsString(result) must not include "govuk-pagination"
        contentAsString(result) must not include "The total of the"
        contentAsString(result) must not include "Displaying"
        contentAsString(result) must include("interest-total")
      }
    }

    "must not render the total row in the table when there are multiple pages" in {
      val mockService = mock[GamblingService]
      when(mockService.getInterestDrilldown(any(), any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(multiPageDetails))

      val app = applicationBuilder()
        .overrides(bind[GamblingService].toInstance(mockService))
        .build()

      running(app) {
        val request = FakeRequest(GET, url)
          .withSession(SessionKeys.regime -> "gbd", SessionKeys.regNumber -> regNumber)
        val result = route(app, request).value
        val body = contentAsString(result)

        status(result) mustEqual OK
        body must not include "interest-total"
      }
    }

    "must redirect to page not found when data has 0 items" in {
      val mockService = mock[GamblingService]
      when(mockService.getInterestDrilldown(any(), any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(interestDrilldown.copy(items = Seq.empty, total = BigDecimal(0))))

      val app = applicationBuilder()
        .overrides(bind[GamblingService].toInstance(mockService))
        .build()

      running(app) {
        val request = FakeRequest(GET, url)
          .withSession(SessionKeys.regime -> "gbd", SessionKeys.regNumber -> regNumber)
        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.PageNotFoundController.onPageLoad().url
      }
    }

    "must support all valid regime codes" in {
      val regimes = Seq("gbd", "pbd", "rgd", "mgd")

      regimes.foreach { code =>
        val mockService = mock[GamblingService]
        when(mockService.getInterestDrilldown(any(), any(), any(), any(), any())(any()))
          .thenReturn(Future.successful(interestDrilldown))

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
