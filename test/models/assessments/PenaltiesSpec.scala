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

import models.penalties.{Penalties, PenaltyItem}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.*

import java.time.LocalDate

class PenaltiesSpec extends AnyFreeSpec with Matchers {

  private val item = PenaltyItem(
    dateRaised      = LocalDate.of(2024, 8, 1),
    descriptionCode = 1980,
    amount          = BigDecimal("-500.00"),
    periodStartDate = LocalDate.of(2024, 1, 1),
    periodEndDate   = LocalDate.of(2024, 3, 31)
  )

  private val penalties = Penalties(
    periodStartDate = Some(LocalDate.of(2024, 1, 1)),
    periodEndDate   = Some(LocalDate.of(2024, 12, 31)),
    total           = BigDecimal("-500.00"),
    totalRecords    = 1,
    items           = Seq(item)
  )

  "PenaltyItem" - {

    "must serialise to JSON" in {
      val json = Json.toJson(item)
      (json \ "dateRaised").as[String] mustEqual "2024-08-01"
      (json \ "descriptionCode").as[Int] mustEqual 1980
      (json \ "amount").as[BigDecimal] mustEqual BigDecimal("-500.00")
      (json \ "periodStartDate").as[String] mustEqual "2024-01-01"
      (json \ "periodEndDate").as[String] mustEqual "2024-03-31"
    }

    "must deserialise from JSON" in {
      val json = Json.obj(
        "dateRaised"      -> "2024-08-01",
        "descriptionCode" -> 1980,
        "amount"          -> -500.00,
        "periodStartDate" -> "2024-01-01",
        "periodEndDate"   -> "2024-03-31"
      )
      json.as[PenaltyItem] mustEqual item
    }

    "must round-trip through JSON" in {
      val json = Json.toJson(item)
      json.as[PenaltyItem] mustEqual item
    }

    "must deserialise with a late payment description code" in {
      val json = Json.obj(
        "dateRaised"      -> "2024-08-01",
        "descriptionCode" -> 1990,
        "amount"          -> -200.00,
        "periodStartDate" -> "2024-01-01",
        "periodEndDate"   -> "2024-03-31"
      )
      json.as[PenaltyItem].descriptionCode mustEqual 1990
    }
  }

  "Penalties" - {

    "must serialise to JSON" in {
      val json = Json.toJson(penalties)
      (json \ "periodStartDate").as[String] mustEqual "2024-01-01"
      (json \ "periodEndDate").as[String] mustEqual "2024-12-31"
      (json \ "total").as[BigDecimal] mustEqual BigDecimal("-500.00")
      (json \ "totalRecords").as[Int] mustEqual 1
      (json \ "items").as[Seq[PenaltyItem]] mustEqual Seq(item)
    }

    "must deserialise from JSON" in {
      val json = Json.obj(
        "periodStartDate" -> "2024-01-01",
        "periodEndDate"   -> "2024-12-31",
        "total"           -> -500.00,
        "totalRecords"    -> 1,
        "items" -> Json.arr(
          Json.obj(
            "dateRaised"      -> "2024-08-01",
            "descriptionCode" -> 1980,
            "amount"          -> -500.00,
            "periodStartDate" -> "2024-01-01",
            "periodEndDate"   -> "2024-03-31"
          )
        )
      )
      json.as[Penalties] mustEqual penalties
    }

    "must round-trip through JSON" in {
      val json = Json.toJson(penalties)
      json.as[Penalties] mustEqual penalties
    }

    "must deserialise with optional period dates absent" in {
      val json = Json.obj(
        "total"        -> 0,
        "totalRecords" -> 0,
        "items"        -> Json.arr()
      )
      val result = json.as[Penalties]
      result.periodStartDate mustBe None
      result.periodEndDate mustBe None
      result.total mustEqual BigDecimal(0)
      result.totalRecords mustEqual 0
      result.items mustBe Seq.empty
    }

    "must deserialise an empty items list" in {
      val json = Json.obj(
        "total"        -> -1000.00,
        "totalRecords" -> 0,
        "items"        -> Json.arr()
      )
      json.as[Penalties].items mustBe Seq.empty
    }
  }
}
