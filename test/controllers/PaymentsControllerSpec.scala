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
import models.payments.{PaymentItem, Payments}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.GamblingService

import java.time.LocalDate
import scala.concurrent.Future

class PaymentsControllerSpec extends SpecBase with MockitoSugar {

  private val regNumber = "XWM00003102200"

  private def url = routes.PaymentsController.onPageLoad().url

  private val paymentItem = PaymentItem(
    transactionDate = LocalDate.of(2024, 7, 23),
    descriptionCode = "E",
    amount          = BigDecimal("-291.64")
  )

  private val singlePageResponse = Payments(
    periodStartDate = Some(LocalDate.of(2023, 11, 1)),
    periodEndDate   = Some(LocalDate.of(2025, 1, 27)),
    total           = BigDecimal("-291.64"),
    totalRecords    = 1,
    items           = Seq(paymentItem)
  )

  private val multiPageResponse = singlePageResponse.copy(totalRecords = 25)

  private val emptyResponse = Payments(
    periodStartDate = Some(LocalDate.of(2023, 11, 1)),
    periodEndDate   = Some(LocalDate.of(2025, 1, 27)),
    total           = BigDecimal(0),
    totalRecords    = 0,
    items           = Seq.empty
  )

  "PaymentsController" - {

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
      when(mockService.getPayments(any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(singlePageResponse))

      val app = applicationBuilder()
        .overrides(bind[GamblingService].toInstance(mockService))
        .build()

      running(app) {
        val request = FakeRequest(GET, url)
          .withSession(SessionKeys.regime -> "gbd", SessionKeys.regNumber -> regNumber)
        val result = route(app, request).value

        status(result) mustEqual OK
        contentAsString(result) must include("Payments")
      }
    }

    "must render the empty-state message when the service returns no items" in {
      val mockService = mock[GamblingService]
      when(mockService.getPayments(any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(emptyResponse))

      val app = applicationBuilder()
        .overrides(bind[GamblingService].toInstance(mockService))
        .build()

      running(app) {
        val request = FakeRequest(GET, url)
          .withSession(SessionKeys.regime -> "gbd", SessionKeys.regNumber -> regNumber)
        val result = route(app, request).value

        status(result) mustEqual OK
        contentAsString(result) must include("You have not made any payments to HMRC.")
      }
    }

    "must render the table when payments are present" in {
      val mockService = mock[GamblingService]
      when(mockService.getPayments(any(), any(), any(), any())(any()))
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

    "must render the description for code E as Electronic" in {
      val mockService = mock[GamblingService]
      when(mockService.getPayments(any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(singlePageResponse))

      val app = applicationBuilder()
        .overrides(bind[GamblingService].toInstance(mockService))
        .build()

      running(app) {
        val request = FakeRequest(GET, url)
          .withSession(SessionKeys.regime -> "gbd", SessionKeys.regNumber -> regNumber)
        val result = route(app, request).value

        status(result) mustEqual OK
        contentAsString(result) must include("Electronic")
      }
    }

    "must render the description for code C as Cheque" in {
      val chequeItem = paymentItem.copy(descriptionCode = "C")
      val mockService = mock[GamblingService]
      when(mockService.getPayments(any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(singlePageResponse.copy(items = Seq(chequeItem))))

      val app = applicationBuilder()
        .overrides(bind[GamblingService].toInstance(mockService))
        .build()

      running(app) {
        val request = FakeRequest(GET, url)
          .withSession(SessionKeys.regime -> "gbd", SessionKeys.regNumber -> regNumber)
        val result = route(app, request).value

        status(result) mustEqual OK
        contentAsString(result) must include("Cheque")
      }
    }

    "must render the bold total row on a single page" in {
      val mockService = mock[GamblingService]
      when(mockService.getPayments(any(), any(), any(), any())(any()))
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
      when(mockService.getPayments(any(), any(), any(), any())(any()))
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
      when(mockService.getPayments(any(), any(), any(), any())(any()))
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
      when(mockService.getPayments(any(), any(), any(), any())(any()))
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

    "must not render the total row when there are multiple pages" in {
      val mockService = mock[GamblingService]
      when(mockService.getPayments(any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(multiPageResponse))

      val app = applicationBuilder()
        .overrides(bind[GamblingService].toInstance(mockService))
        .build()

      running(app) {
        val request = FakeRequest(GET, url)
          .withSession(SessionKeys.regime -> "gbd", SessionKeys.regNumber -> regNumber)
        val result = route(app, request).value

        status(result) mustEqual OK
        contentAsString(result) must not include "<strong>Total</strong>"
      }
    }

    "must return Not Found with page not found content when pageNo exceeds totalPages" in {
      val mockService = mock[GamblingService]
      when(mockService.getPayments(any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(multiPageResponse))

      val app = applicationBuilder()
        .overrides(bind[GamblingService].toInstance(mockService))
        .build()

      running(app) {
        val request = FakeRequest(GET, routes.PaymentsController.onPageLoad(10, 99).url)
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
        when(mockService.getPayments(any(), any(), any(), any())(any()))
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
  }
}
