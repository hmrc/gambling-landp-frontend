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
import org.apache.pekko.stream.Materializer
import play.api.test.FakeRequest
import play.api.test.Helpers.*

class AccountRedirectControllerSpec extends SpecBase {

  private val regNumber = "XWM001"

  "AccountRedirectController" - {

    "must redirect to AccountOverviewController and save regime and regNumber in session for a valid regime" in {
      val app = applicationBuilder().build()

      running(app) {
        implicit val mat: Materializer = app.materializer
        val controller = app.injector.instanceOf[AccountRedirectController]
        val request = FakeRequest(GET, routes.AccountRedirectController.onPageLoad("gbd", regNumber).url)
        val result = call(controller.onPageLoad("gbd", regNumber), request)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.AccountOverviewController.onPageLoad().url
        result.futureValue.newSession.value.get(SessionKeys.regime) mustEqual Some("gbd")
        result.futureValue.newSession.value.get(SessionKeys.regNumber) mustEqual Some(regNumber)
      }
    }

    "must normalise regime code to lowercase before storing in session" in {
      val app = applicationBuilder().build()

      running(app) {
        implicit val mat: Materializer = app.materializer
        val controller = app.injector.instanceOf[AccountRedirectController]
        val request = FakeRequest(GET, routes.AccountRedirectController.onPageLoad("GBD", regNumber).url)
        val result = call(controller.onPageLoad("GBD", regNumber), request)

        status(result) mustEqual SEE_OTHER
        result.futureValue.newSession.value.get(SessionKeys.regime) mustEqual Some("gbd")
      }
    }

    "must support all valid regime codes" in {
      Seq("gbd", "pbd", "rgd", "mgd").foreach { code =>
        val app = applicationBuilder().build()

        running(app) {
          implicit val mat: Materializer = app.materializer
          val controller = app.injector.instanceOf[AccountRedirectController]
          val request = FakeRequest(GET, routes.AccountRedirectController.onPageLoad(code, regNumber).url)
          val result = call(controller.onPageLoad(code, regNumber), request)

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.AccountOverviewController.onPageLoad().url
          result.futureValue.newSession.value.get(SessionKeys.regime) mustEqual Some(code)
        }
      }
    }

    "must redirect to page not found for an unrecognised regime" in {
      val app = applicationBuilder().build()

      running(app) {
        val request = FakeRequest(GET, routes.AccountRedirectController.onPageLoad("unknown", regNumber).url)
        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.PageNotFoundController.onPageLoad().url
      }
    }
  }
}
