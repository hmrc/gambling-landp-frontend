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

package views

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.data.Form
import play.api.data.Forms.text
import play.api.i18n.{DefaultMessagesApi, Lang, MessagesImpl}

class ViewUtilsSpec extends AnyFreeSpec with Matchers {

  private val messages = MessagesImpl(
    Lang("en"),
    new DefaultMessagesApi(
      Map(
        "en" -> Map(
          "service.name"       -> "Manage your gambling tax",
          "site.govuk"         -> "GOV.UK",
          "error.title.prefix" -> "Error:",
          "site.title"         -> "My Page",
          "site.section"       -> "My Section"
        )
      )
    )
  )

  private val emptyForm: Form[String] = Form("value" -> text)
  private val errorForm: Form[String] = emptyForm.withError("value", "required")

  "ViewUtils.titleNoForm" - {

    "must build the page title without a section" in {
      val result = ViewUtils.titleNoForm("site.title")(messages)
      result mustEqual "My Page - Manage your gambling tax - GOV.UK"
    }

    "must include the section when provided" in {
      val result = ViewUtils.titleNoForm("site.title", Some("site.section"))(messages)
      result mustEqual "My Page - My Section - Manage your gambling tax - GOV.UK"
    }
  }

  "ViewUtils.title" - {

    "must build the page title without error prefix when form has no errors" in {
      val result = ViewUtils.title(emptyForm, "site.title")(messages)
      result mustEqual " My Page - Manage your gambling tax - GOV.UK"
    }

    "must prepend the error prefix when the form has errors" in {
      val result = ViewUtils.title(errorForm, "site.title")(messages)
      result mustEqual "Error: My Page - Manage your gambling tax - GOV.UK"
    }

    "must include section and error prefix together" in {
      val result = ViewUtils.title(errorForm, "site.title", Some("site.section"))(messages)
      result mustEqual "Error: My Page - My Section - Manage your gambling tax - GOV.UK"
    }
  }

  "ViewUtils.errorPrefix" - {

    "must return an empty string when the form has no errors" in {
      ViewUtils.errorPrefix(emptyForm)(messages) mustEqual ""
    }

    "must return the error prefix when the form has field errors" in {
      ViewUtils.errorPrefix(errorForm)(messages) mustEqual "Error:"
    }

    "must return the error prefix when the form has global errors" in {
      val formWithGlobalError = emptyForm.withGlobalError("global.error")
      ViewUtils.errorPrefix(formWithGlobalError)(messages) mustEqual "Error:"
    }
  }
}
