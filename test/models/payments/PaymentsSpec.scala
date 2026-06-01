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

package models.payments

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.*

import java.time.LocalDate

class PaymentsSpec extends AnyFreeSpec with Matchers {

  private val item = PaymentItem(
    transactionDate = LocalDate.of(2024, 7, 23),
    descriptionCode = "2680",
    amount          = BigDecimal("-291.64")
  )

  private val payments = Payments(
    periodStartDate = Some(LocalDate.of(2023, 11, 1)),
    periodEndDate   = Some(LocalDate.of(2025, 1, 27)),
    total           = BigDecimal("-291.64"),
    totalRecords    = 1,
    items           = Seq(item)
  )

  "PaymentItem" - {

    "must serialise to JSON" in {
      val json = Json.toJson(item)
      (json \ "transactionDate").as[String] mustEqual "2024-07-23"
      (json \ "descriptionCode").as[String] mustEqual "2680"
      (json \ "amount").as[BigDecimal] mustEqual BigDecimal("-291.64")
    }

    "must deserialise from JSON" in {
      val json = Json.obj(
        "transactionDate" -> "2024-07-23",
        "descriptionCode" -> "2680",
        "amount"          -> -291.64
      )
      json.as[PaymentItem] mustEqual item
    }

    "must round-trip through JSON" in {
      val json = Json.toJson(item)
      json.as[PaymentItem] mustEqual item
    }

    "must deserialise with a different description code" in {
      val json = Json.obj(
        "transactionDate" -> "2024-07-23",
        "descriptionCode" -> "2690",
        "amount"          -> -291.64
      )
      json.as[PaymentItem].descriptionCode mustEqual "2690"
    }
  }

  "Payments" - {

    "must serialise to JSON" in {
      val json = Json.toJson(payments)
      (json \ "periodStartDate").as[String] mustEqual "2023-11-01"
      (json \ "periodEndDate").as[String] mustEqual "2025-01-27"
      (json \ "total").as[BigDecimal] mustEqual BigDecimal("-291.64")
      (json \ "totalRecords").as[Int] mustEqual 1
      (json \ "items").as[Seq[PaymentItem]] mustEqual Seq(item)
    }

    "must deserialise from JSON" in {
      val json = Json.obj(
        "periodStartDate" -> "2023-11-01",
        "periodEndDate"   -> "2025-01-27",
        "total"           -> -291.64,
        "totalRecords"    -> 1,
        "items" -> Json.arr(
          Json.obj(
            "transactionDate" -> "2024-07-23",
            "descriptionCode" -> "2680",
            "amount"          -> -291.64
          )
        )
      )
      json.as[Payments] mustEqual payments
    }

    "must round-trip through JSON" in {
      val json = Json.toJson(payments)
      json.as[Payments] mustEqual payments
    }

    "must deserialise with optional period dates absent" in {
      val json = Json.obj(
        "total"        -> 0,
        "totalRecords" -> 0,
        "items"        -> Json.arr()
      )
      val result = json.as[Payments]
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
      json.as[Payments].items mustBe Seq.empty
    }

    "must deserialise multiple items" in {
      val json = Json.obj(
        "total"        -> -583.28,
        "totalRecords" -> 2,
        "items" -> Json.arr(
          Json.obj("transactionDate" -> "2024-07-23", "descriptionCode" -> "2680", "amount" -> -291.64),
          Json.obj("transactionDate" -> "2024-08-15", "descriptionCode" -> "2690", "amount" -> -291.64)
        )
      )
      json.as[Payments].items must have size 2
    }
  }
}
