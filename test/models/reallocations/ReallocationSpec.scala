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

package models.reallocations

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.*

import java.time.LocalDate

class ReallocationSpec extends AnyFreeSpec with Matchers {

  private val item = ReallocationItem(
    dateProcessed = Some(LocalDate.of(2024, 7, 1)),
    amount        = Some(BigDecimal("30.80"))
  )

  private val reallocations = Reallocations(
    periodStartDate = Some(LocalDate.of(2024, 1, 1)),
    periodEndDate   = Some(LocalDate.of(2024, 12, 31)),
    total           = Some(BigDecimal("30.80")),
    totalRecords    = Some(1),
    items           = Seq(item)
  )

  "ReallocationItem" - {

    "must serialise to JSON" in {
      val json = Json.toJson(item)
      (json \ "dateProcessed").as[String] mustEqual "2024-07-01"
      (json \ "amount").as[BigDecimal] mustEqual BigDecimal("30.80")
    }

    "must deserialise from JSON" in {
      val json = Json.obj(
        "dateProcessed" -> "2024-07-01",
        "amount"        -> 30.80
      )
      json.as[ReallocationItem] mustEqual item
    }

    "must round-trip through JSON" in {
      val json = Json.toJson(item)
      json.as[ReallocationItem] mustEqual item
    }

    "must deserialise with optional fields absent" in {
      val json = Json.obj()
      val result = json.as[ReallocationItem]
      result.dateProcessed mustBe None
      result.amount mustBe None
    }
  }

  "Reallocations" - {

    "must serialise to JSON" in {
      val json = Json.toJson(reallocations)
      (json \ "totalRecords").as[Int] mustEqual 1
      (json \ "items").as[Seq[ReallocationItem]] mustEqual Seq(item)
    }

    "must deserialise from JSON" in {
      val json = Json.obj(
        "periodStartDate" -> "2024-01-01",
        "periodEndDate"   -> "2024-12-31",
        "total"           -> 30.80,
        "totalRecords"    -> 1,
        "items"           -> Json.arr(Json.obj("dateProcessed" -> "2024-07-01", "amount" -> 30.80))
      )
      json.as[Reallocations] mustEqual reallocations
    }

    "must round-trip through JSON" in {
      val json = Json.toJson(reallocations)
      json.as[Reallocations] mustEqual reallocations
    }

    "must deserialise with optional fields absent" in {
      val json = Json.obj("items" -> Json.arr())
      val result = json.as[Reallocations]
      result.periodStartDate mustBe None
      result.periodEndDate mustBe None
      result.total mustBe None
      result.totalRecords mustBe None
      result.items mustBe Seq.empty
    }
  }
}
