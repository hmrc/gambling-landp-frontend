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
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.AccountOverview

class AccountOverviewControllerSpec extends SpecBase {

  private val regNumber = "XWM001"

  "AccountOverview Controller" - {

    "must return OK and render the view with the regNumber from session" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, routes.AccountOverviewController.onPageLoad().url)
          .withSession(SessionKeys.regNumber -> regNumber)

        val result = route(application, request).value

        val view = application.injector.instanceOf[AccountOverview]

        status(result) mustEqual OK

        contentAsString(result) mustEqual view(regNumber)(request, messages(application)).toString
      }
    }

    "must redirect to Unauthorised when regNumber is absent from the session" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, routes.AccountOverviewController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }
  }
}
