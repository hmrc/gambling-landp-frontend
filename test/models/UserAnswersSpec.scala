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

package models

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{OptionValues, TryValues}
import pages.QuestionPage
import play.api.libs.json.*

import java.time.Instant
import scala.util.{Failure, Success}

object UserAnswersSpec {
  case object TestPage extends QuestionPage[String] {
    override def path: JsPath = JsPath \ "testPage"
  }
  case object NestedPage extends QuestionPage[Int] {
    override def path: JsPath = JsPath \ "nested" \ "value"
  }
}

class UserAnswersSpec extends AnyFreeSpec with Matchers with TryValues with OptionValues {

  import UserAnswersSpec.*

  private val answers = UserAnswers("test-id")

  "UserAnswers.get" - {

    "must return None when the page has not been answered" in {
      answers.get(TestPage) mustBe None
    }

    "must return the stored value after it has been set" in {
      val updated = answers.set(TestPage, "hello").success.value
      updated.get(TestPage) mustBe Some("hello")
    }

    "must return None for a different page even when data exists" in {
      val updated = answers.set(TestPage, "hello").success.value
      updated.get(NestedPage) mustBe None
    }
  }

  "UserAnswers.set" - {

    "must store a string value" in {
      val result = answers.set(TestPage, "world")
      result mustBe a[Success[?]]
      result.success.value.get(TestPage) mustBe Some("world")
    }

    "must store a nested value" in {
      answers.set(NestedPage, 42).success.value.get(NestedPage) mustBe Some(42)
    }

    "must overwrite an existing value" in {
      val updated = answers.set(TestPage, "first").flatMap(_.set(TestPage, "second")).success.value
      updated.get(TestPage) mustBe Some("second")
    }

    "must preserve other page values when setting a new one" in {
      val updated = answers.set(TestPage, "first").flatMap(_.set(NestedPage, 99)).success.value
      updated.get(TestPage) mustBe Some("first")
      updated.get(NestedPage) mustBe Some(99)
    }

    "must return a Failure when the path cannot be set on the data structure" in {
      case object BadIndexPage extends QuestionPage[String] {
        override def path: JsPath = JsPath \ 0
      }
      answers.set(BadIndexPage, "value") mustBe a[Failure[?]]
    }
  }

  "UserAnswers.remove" - {

    "must remove a value that exists" in {
      val withValue = answers.set(TestPage, "to-remove").success.value
      withValue.remove(TestPage).success.value.get(TestPage) mustBe None
    }

    "must succeed (no-op) when the value does not exist" in {
      answers.remove(TestPage) mustBe a[Success[?]]
      answers.remove(TestPage).success.value.get(TestPage) mustBe None
    }

    "must not affect other pages when removing one" in {
      val withBoth = answers.set(TestPage, "keep").flatMap(_.set(NestedPage, 7)).success.value
      val afterRemove = withBoth.remove(NestedPage).success.value
      afterRemove.get(TestPage) mustBe Some("keep")
      afterRemove.get(NestedPage) mustBe None
    }
  }

  "UserAnswers JSON format" - {

    "must round-trip through reads and writes" in {
      val ua = UserAnswers("round-trip", Json.obj("key" -> "value"), Instant.ofEpochMilli(0))
      val json = Json.toJson(ua)(UserAnswers.writes)
      val parsed = json.as[UserAnswers](UserAnswers.reads)
      parsed.id mustEqual ua.id
      parsed.data mustEqual ua.data
      parsed.lastUpdated mustEqual ua.lastUpdated
    }
  }
}
