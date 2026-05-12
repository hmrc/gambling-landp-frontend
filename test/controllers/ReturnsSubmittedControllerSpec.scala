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
import models.returns.{AmountDeclared, ReturnsSubmitted}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.GamblingService

import java.time.LocalDate
import scala.concurrent.Future

class ReturnsSubmittedControllerSpec extends SpecBase with MockitoSugar {

  private val regNumber = "XWM00003102200"

  private def returnsUrl = routes.ReturnsSubmittedController.onPageLoad().url

  private val amountDeclared = AmountDeclared(
    descriptionCode = Some(1),
    periodStartDate = Some(LocalDate.of(2024, 1, 1)),
    periodEndDate   = Some(LocalDate.of(2024, 3, 31)),
    amount          = Some(BigDecimal("1500.00"))
  )

  private val singlePageResponse = ReturnsSubmitted(
    periodStartDate    = Some(LocalDate.of(2024, 1, 1)),
    periodEndDate      = Some(LocalDate.of(2024, 12, 31)),
    amountDeclared     = Seq(amountDeclared),
    total              = Some(BigDecimal("1500.00")),
    totalPeriodRecords = Some(1)
  )

  private val multiPageResponse = singlePageResponse.copy(totalPeriodRecords = Some(25))

  private val emptyResponse = ReturnsSubmitted(
    periodStartDate    = Some(LocalDate.of(2024, 1, 1)),
    periodEndDate      = Some(LocalDate.of(2024, 12, 31)),
    amountDeclared     = Seq.empty,
    total              = Some(BigDecimal(0)),
    totalPeriodRecords = Some(0)
  )

  "ReturnsSubmittedController" - {

    "must redirect to Unauthorised when regime is missing from session" in {
      val app = applicationBuilder().build()

      running(app) {
        val request = FakeRequest(GET, returnsUrl)
        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised when regNumber is missing from session" in {
      val app = applicationBuilder().build()

      running(app) {
        val request = FakeRequest(GET, returnsUrl).withSession(SessionKeys.regime -> "gbd")
        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to page not found when session contains an unrecognised regime" in {
      val app = applicationBuilder().build()

      running(app) {
        val request = FakeRequest(GET, returnsUrl)
          .withSession(SessionKeys.regime -> "unknown", SessionKeys.regNumber -> regNumber)
        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.PageNotFoundController.onPageLoad().url
      }
    }

    "must return OK and render the heading for a valid regime" in {
      val mockService = mock[GamblingService]
      when(mockService.getReturnsSubmitted(any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(singlePageResponse))

      val app = applicationBuilder()
        .overrides(bind[GamblingService].toInstance(mockService))
        .build()

      running(app) {
        val request = FakeRequest(GET, returnsUrl)
          .withSession(SessionKeys.regime -> "gbd", SessionKeys.regNumber -> regNumber)
        val result = route(app, request).value

        status(result) mustEqual OK
        contentAsString(result) must include("Returns submitted")
      }
    }

    "must include the regime duty name in the page content" in {
      val mockService = mock[GamblingService]
      when(mockService.getReturnsSubmitted(any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(singlePageResponse))

      val app = applicationBuilder()
        .overrides(bind[GamblingService].toInstance(mockService))
        .build()

      running(app) {
        val request = FakeRequest(GET, returnsUrl)
          .withSession(SessionKeys.regime -> "gbd", SessionKeys.regNumber -> regNumber)
        val result = route(app, request).value

        status(result) mustEqual OK
        contentAsString(result) must include("General Betting Duty")
      }
    }

    "must render the empty-state message when the service returns no items" in {
      val mockService = mock[GamblingService]
      when(mockService.getReturnsSubmitted(any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(emptyResponse))

      val app = applicationBuilder()
        .overrides(bind[GamblingService].toInstance(mockService))
        .build()

      running(app) {
        val request = FakeRequest(GET, returnsUrl)
          .withSession(SessionKeys.regime -> "gbd", SessionKeys.regNumber -> regNumber)
        val result = route(app, request).value

        status(result) mustEqual OK
        contentAsString(result) must include("You have not submitted any returns.")
      }
    }

    "must render the table when returns are present on a single page" in {
      val mockService = mock[GamblingService]
      when(mockService.getReturnsSubmitted(any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(singlePageResponse))

      val app = applicationBuilder()
        .overrides(bind[GamblingService].toInstance(mockService))
        .build()

      running(app) {
        val request = FakeRequest(GET, returnsUrl)
          .withSession(SessionKeys.regime -> "gbd", SessionKeys.regNumber -> regNumber)
        val result = route(app, request).value
        val body = contentAsString(result)

        status(result) mustEqual OK
        body must include("govuk-table")
        body must not include "The total of the"
        body must not include "Displaying"
      }
    }

    "must render the summary paragraphs and table when there are multiple pages" in {
      val mockService = mock[GamblingService]
      when(mockService.getReturnsSubmitted(any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(multiPageResponse))

      val app = applicationBuilder()
        .overrides(bind[GamblingService].toInstance(mockService))
        .build()

      running(app) {
        val request = FakeRequest(GET, returnsUrl)
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
      when(mockService.getReturnsSubmitted(any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(multiPageResponse))

      val app = applicationBuilder()
        .overrides(bind[GamblingService].toInstance(mockService))
        .build()

      running(app) {
        val request = FakeRequest(GET, returnsUrl)
          .withSession(SessionKeys.regime -> "gbd", SessionKeys.regNumber -> regNumber)
        val result = route(app, request).value

        status(result) mustEqual OK
        contentAsString(result) must include("govuk-pagination")
      }
    }

    "must not render pagination when there is only one page" in {
      val mockService = mock[GamblingService]
      when(mockService.getReturnsSubmitted(any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(singlePageResponse))

      val app = applicationBuilder()
        .overrides(bind[GamblingService].toInstance(mockService))
        .build()

      running(app) {
        val request = FakeRequest(GET, returnsUrl)
          .withSession(SessionKeys.regime -> "gbd", SessionKeys.regNumber -> regNumber)
        val result = route(app, request).value

        status(result) mustEqual OK
        contentAsString(result) must not include "govuk-pagination"
      }
    }

    "must support all valid regime codes" in {
      val regimes = Seq(
        "gbd" -> "General Betting Duty",
        "pbd" -> "Pool Betting Duty",
        "rgd" -> "Remote Gaming Duty",
        "mgd" -> "Machine Games Duty"
      )

      regimes.foreach { case (code, expectedDutyName) =>
        val mockService = mock[GamblingService]
        when(mockService.getReturnsSubmitted(any(), any(), any(), any())(any()))
          .thenReturn(Future.successful(singlePageResponse))

        val app = applicationBuilder()
          .overrides(bind[GamblingService].toInstance(mockService))
          .build()

        running(app) {
          val request = FakeRequest(GET, returnsUrl)
            .withSession(SessionKeys.regime -> code, SessionKeys.regNumber -> regNumber)
          val result = route(app, request).value

          status(result) mustEqual OK
          contentAsString(result) must include(expectedDutyName)
        }
      }
    }
  }
}
