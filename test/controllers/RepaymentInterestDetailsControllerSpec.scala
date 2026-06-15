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
import models.interest.{InterestDetailItem, InterestDetails}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.GamblingService

import java.time.LocalDate
import scala.concurrent.Future

class RepaymentInterestDetailsControllerSpec extends SpecBase with MockitoSugar {

  private val regNumber = "XWM00003102200"

  private def url = routes.RepaymentInterestDetailsController.onPageLoad().url

  private val returnChargeItem = InterestDetailItem(
    descriptionCode = 1960,
    amount          = BigDecimal(-182.19),
    interestId      = "SAFE-CHG-00001",
    periodStartDate = LocalDate.of(2009, 9, 1),
    periodEndDate   = LocalDate.of(2016, 7, 31)
  )

  private val singlePageResponse = InterestDetails(
    periodStartDate = Some(LocalDate.of(2009, 9, 1)),
    periodEndDate   = Some(LocalDate.of(2016, 7, 31)),
    total           = BigDecimal(-182.19),
    totalRecords    = 1,
    items           = Seq(returnChargeItem)
  )

  private val multiPageResponse = singlePageResponse.copy(totalRecords = 25)

  "RepaymentInterestDetailsController" - {

    "must redirect to Unauthorised when regime is missing from session" in {
      val app = applicationBuilder().build()

      running(app) {
        val request = FakeRequest(GET, url)
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

    "must return OK and render the heading for a valid regime" in {
      val mockService = mock[GamblingService]
      when(mockService.getRepaymentInterestDetails(any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(singlePageResponse))

      val app = applicationBuilder()
        .overrides(bind[GamblingService].toInstance(mockService))
        .build()

      running(app) {
        val request = FakeRequest(GET, url)
          .withSession(SessionKeys.regime -> "gbd", SessionKeys.regNumber -> regNumber)
        val result = route(app, request).value

        status(result) mustEqual OK
        contentAsString(result) must include("Repayment interest")
      }
    }

    "must render the table when items are present" in {
      val mockService = mock[GamblingService]
      when(mockService.getRepaymentInterestDetails(any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(singlePageResponse))

      val app = applicationBuilder()
        .overrides(bind[GamblingService].toInstance(mockService))
        .build()

      running(app) {
        val request = FakeRequest(GET, url)
          .withSession(SessionKeys.regime -> "gbd", SessionKeys.regNumber -> regNumber)
        val result = route(app, request).value
        val body = contentAsString(result)

        status(result) mustEqual OK
        body must include("govuk-table")
        body must not include "The total of the"
        body must not include "Displaying"
      }
    }

    "must render the description as central assessment interest for code 1960" in {
      val mockService = mock[GamblingService]
      when(mockService.getRepaymentInterestDetails(any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(singlePageResponse))

      val app = applicationBuilder()
        .overrides(bind[GamblingService].toInstance(mockService))
        .build()

      running(app) {
        val request = FakeRequest(GET, url)
          .withSession(SessionKeys.regime -> "gbd", SessionKeys.regNumber -> regNumber)
        val result = route(app, request).value

        status(result) mustEqual OK
        val body = contentAsString(result)
        body must (include("Interest on central assessment from 1 Sep 2009 to 31 Jul 2016") or include(
          "Interest on central assessment from 1 Sept 2009 to 31 Jul 2016"
        ))
      }
    }

    "must render the bold total row on a single page" in {
      val mockService = mock[GamblingService]
      when(mockService.getRepaymentInterestDetails(any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(singlePageResponse))

      val app = applicationBuilder()
        .overrides(bind[GamblingService].toInstance(mockService))
        .build()

      running(app) {
        val request = FakeRequest(GET, url)
          .withSession(SessionKeys.regime -> "gbd", SessionKeys.regNumber -> regNumber)
        val result = route(app, request).value
        val body = contentAsString(result)

        status(result) mustEqual OK
        body must include("<strong>Total</strong>")
      }
    }

    "must render the summary paragraphs and table when there are multiple pages" in {
      val mockService = mock[GamblingService]
      when(mockService.getRepaymentInterestDetails(any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(multiPageResponse))

      val app = applicationBuilder()
        .overrides(bind[GamblingService].toInstance(mockService))
        .build()

      running(app) {
        val request = FakeRequest(GET, url)
          .withSession(SessionKeys.regime -> "gbd", SessionKeys.regNumber -> regNumber)
        val result = route(app, request).value
        val body = contentAsString(result)

        status(result) mustEqual OK
        body must include("The total of the")
        body must include("Displaying")
        body must include("govuk-table")
      }
    }

    "must render pagination when there are multiple pages" in {
      val mockService = mock[GamblingService]
      when(mockService.getRepaymentInterestDetails(any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(multiPageResponse))

      val app = applicationBuilder()
        .overrides(bind[GamblingService].toInstance(mockService))
        .build()

      running(app) {
        val request = FakeRequest(GET, url)
          .withSession(SessionKeys.regime -> "gbd", SessionKeys.regNumber -> regNumber)
        val result = route(app, request).value

        status(result) mustEqual OK
        contentAsString(result) must include("govuk-pagination")
      }
    }

    "must not render pagination when there is only one page" in {
      val mockService = mock[GamblingService]
      when(mockService.getRepaymentInterestDetails(any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(singlePageResponse))

      val app = applicationBuilder()
        .overrides(bind[GamblingService].toInstance(mockService))
        .build()

      running(app) {
        val request = FakeRequest(GET, url)
          .withSession(SessionKeys.regime -> "gbd", SessionKeys.regNumber -> regNumber)
        val result = route(app, request).value

        status(result) mustEqual OK
        contentAsString(result) must not include "govuk-pagination"
      }
    }

    "must return Not Found with page not found content when pageNo exceeds totalPages" in {
      val mockService = mock[GamblingService]
      when(mockService.getRepaymentInterestDetails(any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(multiPageResponse))

      val app = applicationBuilder()
        .overrides(bind[GamblingService].toInstance(mockService))
        .build()

      running(app) {
        val request = FakeRequest(GET, routes.RepaymentInterestDetailsController.onPageLoad(10, 99).url)
          .withSession(SessionKeys.regime -> "gbd", SessionKeys.regNumber -> regNumber)
        val result = route(app, request).value

        status(result) mustEqual NOT_FOUND
        contentAsString(result) must include("Page not found")
      }
    }

    "must support all valid regime codes" in {
      val regimes = Seq("gbd", "pbd", "rgd", "mgd")

      regimes.foreach { code =>
        val mockService = mock[GamblingService]
        when(mockService.getRepaymentInterestDetails(any(), any(), any(), any())(any()))
          .thenReturn(Future.successful(singlePageResponse))

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

    "must not render descriptions as drilldown links" in {
      val mockService = mock[GamblingService]
      when(mockService.getRepaymentInterestDetails(any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(singlePageResponse))

      val app = applicationBuilder()
        .overrides(bind[GamblingService].toInstance(mockService))
        .build()

      running(app) {
        val request = FakeRequest(GET, url)
          .withSession(SessionKeys.regime -> "gbd", SessionKeys.regNumber -> regNumber)
        val result = route(app, request).value
        val body = contentAsString(result)

        status(result) mustEqual OK
        body must not include "interest-accruing"
      }
    }
  }
}
