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

package models.assessments

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.*

import java.time.LocalDate

class AssessmentsSpec extends AnyFreeSpec with Matchers {

  private val item = AssessmentItem(
    dateRaised = Some(LocalDate.of(2024, 7, 1)),
    periodStartDate = Some(LocalDate.of(2024, 1, 10)),
    periodEndDate = Some(LocalDate.of(2024, 9, 13)),
    amount = Some(BigDecimal("100.44"))
  )

  private val assessments = Assessments(
    periodStartDate = Some(LocalDate.of(2024, 1, 1)),
    periodEndDate = Some(LocalDate.of(2024, 12, 31)),
    total = Some(BigDecimal("100.44")),
    totalRecords = Some(1),
    items = Seq(item)
  )

  "AssessmentItem" - {

    "must serialise to JSON" in {
      val json = Json.toJson(item)
      (json \ "dateRaised").as[String] mustEqual "2024-07-01"
      (json \ "periodStartDate").as[String] mustEqual "2024-01-10"
      (json \ "periodEndDate").as[String] mustEqual "2024-09-13"
      (json \ "amount").as[BigDecimal] mustEqual BigDecimal("100.44")
    }

    "must deserialise from JSON" in {
      val json = Json.obj(
        "dateRaised" -> "2024-07-01",
        "periodStartDate" -> "2024-01-10",
        "periodEndDate" -> "2024-09-13",
        "amount" -> 100.44
      )
      json.as[AssessmentItem] mustEqual item
    }

    "must round-trip through JSON" in {
      val json = Json.toJson(item)
      json.as[AssessmentItem] mustEqual item
    }

    "must deserialise with optional fields absent" in {
      val json = Json.obj()
      val result = json.as[AssessmentItem]
      result.dateRaised mustBe None
      result.periodStartDate mustBe None
      result.periodEndDate mustBe None
      result.amount mustBe None
    }
  }

  "Assessments" - {

    "must serialise to JSON" in {
      val json = Json.toJson(assessments)
      (json \ "periodStartDate").as[String] mustEqual "2024-01-01"
      (json \ "periodEndDate").as[String] mustEqual "2024-12-31"
      (json \ "totalRecords").as[Int] mustEqual 1
      (json \ "items").as[Seq[AssessmentItem]] mustEqual Seq(item)
    }

    "must deserialise from JSON" in {
      val json = Json.obj(
        "periodStartDate" -> "2024-01-01",
        "periodEndDate" -> "2024-12-31",
        "total" -> 100.44,
        "totalRecords" -> 1,
        "items" -> Json.arr(
          Json.obj("dateRaised" -> "2024-07-01", "periodStartDate" -> "2024-01-10", "periodEndDate" -> "2024-09-13", "amount" -> 100.44)
        )
      )
      json.as[Assessments] mustEqual assessments
    }

    "must round-trip through JSON" in {
      val json = Json.toJson(assessments)
      json.as[Assessments] mustEqual assessments
    }

    "must deserialise with optional fields absent" in {
      val json = Json.obj("items" -> Json.arr())
      val result = json.as[Assessments]
      result.periodStartDate mustBe None
      result.periodEndDate mustBe None
      result.total mustBe None
      result.totalRecords mustBe None
      result.items mustBe Seq.empty
    }
  }
}
