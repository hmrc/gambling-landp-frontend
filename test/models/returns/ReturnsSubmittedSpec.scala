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

package models.returns

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.*

import java.time.LocalDate

class ReturnsSubmittedSpec extends AnyFreeSpec with Matchers {

  private val amountDeclared = AmountDeclared(
    descriptionCode = Some(1234),
    periodStartDate = Some(LocalDate.of(2024, 1, 10)),
    periodEndDate   = Some(LocalDate.of(2024, 9, 13)),
    amount          = Some(BigDecimal("100.44"))
  )

  private val returnsSubmitted = ReturnsSubmitted(
    periodStartDate    = Some(LocalDate.of(2024, 1, 1)),
    periodEndDate      = Some(LocalDate.of(2024, 12, 31)),
    total              = Some(BigDecimal("100.44")),
    totalPeriodRecords = Some(1),
    amountDeclared     = Seq(amountDeclared)
  )

  "AmountDeclared" - {

    "must serialise to JSON" in {
      val json = Json.toJson(amountDeclared)
      (json \ "descriptionCode").as[Int] mustEqual 1234
      (json \ "periodStartDate").as[String] mustEqual "2024-01-10"
      (json \ "periodEndDate").as[String] mustEqual "2024-09-13"
      (json \ "amount").as[BigDecimal] mustEqual BigDecimal("100.44")
    }

    "must deserialise from JSON" in {
      val json = Json.obj(
        "descriptionCode" -> 1234,
        "periodStartDate" -> "2024-01-10",
        "periodEndDate"   -> "2024-09-13",
        "amount"          -> 100.44
      )
      json.as[AmountDeclared] mustEqual amountDeclared
    }

    "must round-trip through JSON" in {
      val json = Json.toJson(amountDeclared)
      json.as[AmountDeclared] mustEqual amountDeclared
    }

    "must deserialise with optional fields absent" in {
      val json = Json.obj()
      val result = json.as[AmountDeclared]
      result.descriptionCode mustBe None
      result.periodStartDate mustBe None
      result.periodEndDate mustBe None
      result.amount mustBe None
    }
  }

  "ReturnsSubmitted" - {

    "must serialise to JSON" in {
      val json = Json.toJson(returnsSubmitted)
      (json \ "periodStartDate").as[String] mustEqual "2024-01-01"
      (json \ "periodEndDate").as[String] mustEqual "2024-12-31"
      (json \ "totalPeriodRecords").as[Int] mustEqual 1
      (json \ "amountDeclared").as[Seq[AmountDeclared]] mustEqual Seq(amountDeclared)
    }

    "must deserialise from JSON" in {
      val json = Json.obj(
        "periodStartDate"    -> "2024-01-01",
        "periodEndDate"      -> "2024-12-31",
        "total"              -> 100.44,
        "totalPeriodRecords" -> 1,
        "amountDeclared" -> Json.arr(
          Json.obj("descriptionCode" -> 1234, "periodStartDate" -> "2024-01-10", "periodEndDate" -> "2024-09-13", "amount" -> 100.44)
        )
      )
      json.as[ReturnsSubmitted] mustEqual returnsSubmitted
    }

    "must round-trip through JSON" in {
      val json = Json.toJson(returnsSubmitted)
      json.as[ReturnsSubmitted] mustEqual returnsSubmitted
    }

    "must deserialise with optional fields absent" in {
      val json = Json.obj("amountDeclared" -> Json.arr())
      val result = json.as[ReturnsSubmitted]
      result.periodStartDate mustBe None
      result.periodEndDate mustBe None
      result.total mustBe None
      result.totalPeriodRecords mustBe None
      result.amountDeclared mustBe Seq.empty
    }
  }
}
