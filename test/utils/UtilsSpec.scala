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

package utils

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem

class UtilsSpec extends AnyFreeSpec with Matchers {

  "Utils.emptyString" - {
    "must be an empty string" in {
      Utils.emptyString mustEqual ""
    }
  }

  "Utils.firstRadioId" - {
    "must equal value_0" in {
      Utils.firstRadioId mustEqual "value_0"
    }
  }

  "Utils.withIds" - {

    "must assign sequential IDs with the default prefix" in {
      val items = Seq(
        RadioItem(content = Text("One")),
        RadioItem(content = Text("Two")),
        RadioItem(content = Text("Three"))
      )
      val result = Utils.withIds(items)
      result.map(_.id) mustEqual Seq(Some("value_0"), Some("value_1"), Some("value_2"))
    }

    "must assign sequential IDs with a custom prefix" in {
      val items = Seq(
        RadioItem(content = Text("A")),
        RadioItem(content = Text("B"))
      )
      val result = Utils.withIds(items, "choice")
      result.map(_.id) mustEqual Seq(Some("choice_0"), Some("choice_1"))
    }

    "must return an empty sequence when given an empty sequence" in {
      Utils.withIds(Seq.empty) mustEqual Seq.empty
    }

    "must preserve all other RadioItem fields" in {
      val item = RadioItem(content = Text("Option"), value = Some("opt"))
      val Seq(result) = Utils.withIds(Seq(item))
      result.content mustEqual Text("Option")
      result.value mustEqual Some("opt")
      result.id mustEqual Some("value_0")
    }
  }
}
