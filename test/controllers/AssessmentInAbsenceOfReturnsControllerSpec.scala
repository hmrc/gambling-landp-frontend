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
import models.assessments.{AssessmentItem, Assessments}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.GamblingService

import java.time.LocalDate
import scala.concurrent.Future

class AssessmentInAbsenceOfReturnsControllerSpec extends SpecBase with MockitoSugar {

  private val regNumber = "XWM00003102200"

  private def url = routes.AssessmentInAbsenceOfReturnsController.onPageLoad().url

  private val singleRecord = AssessmentItem(
    dateRaised      = Some(LocalDate.of(2024, 2, 10)),
    periodStartDate = Some(LocalDate.of(2024, 1, 1)),
    periodEndDate   = Some(LocalDate.of(2024, 3, 31)),
    amount          = Some(BigDecimal("1500.00"))
  )

  private val singlePageResponse = Assessments(
    periodStartDate = Some(LocalDate.of(2024, 1, 1)),
    periodEndDate   = Some(LocalDate.of(2024, 12, 31)),
    total           = Some(BigDecimal("1500.00")),
    totalRecords    = Some(1),
    items           = Seq(singleRecord)
  )

  private val multiPageResponse = singlePageResponse.copy(totalRecords = Some(25))

  private val emptyResponse = Assessments(
    periodStartDate = Some(LocalDate.of(2024, 1, 1)),
    periodEndDate   = Some(LocalDate.of(2024, 12, 31)),
    total           = Some(BigDecimal(0)),
    totalRecords    = Some(0),
    items           = Seq.empty
  )

  "AssessmentInAbsenceOfReturnsController" - {

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

    "must return OK for a valid regime and render the heading, intro paragraph" in {
      val mockService = mock[GamblingService]
      when(mockService.getAssessmentsWithoutReturns(any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(singlePageResponse))

      val app = applicationBuilder()
        .overrides(bind[GamblingService].toInstance(mockService))
        .build()

      running(app) {
        val request = FakeRequest(GET, url)
          .withSession(SessionKeys.regime -> "gbd", SessionKeys.regNumber -> regNumber)
        val result = route(app, request).value

        status(result) mustEqual OK
        contentAsString(result) must include("Assessments in absence of return")
        contentAsString(result) must include("Assessments in the absence of a return")
        contentAsString(result) must include("Assessments made where you have not yet submitted a return, or where a return has not been accepted by HMRC.")
        contentAsString(result) must include("Submit returns for these periods immediately, as your payment is late. Once you have submitted a return, the assessment for that period will be withdrawn.")
        contentAsString(result) must include("You may not have yet received formal notification of recent assessments.")
      }
    }

    "must render the empty-state message when the service returns no items" in {
      val mockService = mock[GamblingService]
      when(mockService.getAssessmentsWithoutReturns(any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(emptyResponse))

      val app = applicationBuilder()
        .overrides(bind[GamblingService].toInstance(mockService))
        .build()

      running(app) {
        val request = FakeRequest(GET, url)
          .withSession(SessionKeys.regime -> "gbd", SessionKeys.regNumber -> regNumber)
        val result = route(app, request).value

        status(result) mustEqual OK
        contentAsString(result) must include("You are up-to-date on your returns.")
      }
    }

    "must render the table when records are present" in {
      val mockService = mock[GamblingService]
      when(mockService.getAssessmentsWithoutReturns(any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(singlePageResponse))

      val app = applicationBuilder()
        .overrides(bind[GamblingService].toInstance(mockService))
        .build()

      running(app) {
        val request = FakeRequest(GET, url)
          .withSession(SessionKeys.regime -> "gbd", SessionKeys.regNumber -> regNumber)
        val result = route(app, request).value

        status(result) mustEqual OK
        contentAsString(result) must include("govuk-table")
      }
    }

    "must render pagination and summary paragraphs when there are multiple pages" in {
      val mockService = mock[GamblingService]
      when(mockService.getAssessmentsWithoutReturns(any(), any(), any(), any())(any()))
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
        body must include("govuk-pagination")
        body must include("The total of the")
        body must include("Displaying")
      }
    }

    "must not render pagination when there is only one page" in {
      val mockService = mock[GamblingService]
      when(mockService.getAssessmentsWithoutReturns(any(), any(), any(), any())(any()))
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
      when(mockService.getAssessmentsWithoutReturns(any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(multiPageResponse))

      val app = applicationBuilder()
        .overrides(bind[GamblingService].toInstance(mockService))
        .build()

      running(app) {
        val request = FakeRequest(GET, routes.AssessmentInAbsenceOfReturnsController.onPageLoad(10, 99).url)
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
        when(mockService.getAssessmentsWithoutReturns(any(), any(), any(), any())(any()))
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