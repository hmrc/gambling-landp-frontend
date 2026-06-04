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

package models.repayments

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.*

import java.time.LocalDate

class RepaymentInterestRepaidSpec extends AnyFreeSpec with Matchers {

  private val item = RepaymentInterestRepaidItem(
    transactionDate = LocalDate.of(2024, 7, 23),
    amount          = BigDecimal("45.60")
  )

  private val repaymentInterestRepaid = RepaymentInterestRepaid(
    periodStartDate = Some(LocalDate.of(2023, 11, 1)),
    periodEndDate   = Some(LocalDate.of(2025, 1, 27)),
    total           = BigDecimal("45.60"),
    totalRecords    = 1,
    items           = Seq(item)
  )

  "RepaymentInterestRepaidItem" - {

    "must serialise to JSON" in {
      val json = Json.toJson(item)
      (json \ "transactionDate").as[String] mustEqual "2024-07-23"
      (json \ "amount").as[BigDecimal] mustEqual BigDecimal("45.60")
    }

    "must deserialise from JSON" in {
      val json = Json.obj(
        "transactionDate" -> "2024-07-23",
        "amount"          -> 45.60
      )
      json.as[RepaymentInterestRepaidItem] mustEqual item
    }

    "must round-trip through JSON" in {
      val json = Json.toJson(item)
      json.as[RepaymentInterestRepaidItem] mustEqual item
    }
  }

  "RepaymentInterestRepaid" - {

    "must serialise to JSON" in {
      val json = Json.toJson(repaymentInterestRepaid)
      (json \ "periodStartDate").as[String] mustEqual "2023-11-01"
      (json \ "periodEndDate").as[String] mustEqual "2025-01-27"
      (json \ "total").as[BigDecimal] mustEqual BigDecimal("45.60")
      (json \ "totalRecords").as[Int] mustEqual 1
      (json \ "items").as[Seq[RepaymentInterestRepaidItem]] mustEqual Seq(item)
    }

    "must deserialise from JSON" in {
      val json = Json.obj(
        "periodStartDate" -> "2023-11-01",
        "periodEndDate"   -> "2025-01-27",
        "total"           -> 45.60,
        "totalRecords"    -> 1,
        "items" -> Json.arr(
          Json.obj(
            "transactionDate" -> "2024-07-23",
            "amount"          -> 45.60
          )
        )
      )
      json.as[RepaymentInterestRepaid] mustEqual repaymentInterestRepaid
    }

    "must round-trip through JSON" in {
      val json = Json.toJson(repaymentInterestRepaid)
      json.as[RepaymentInterestRepaid] mustEqual repaymentInterestRepaid
    }

    "must deserialise with optional period dates absent" in {
      val json = Json.obj(
        "total"        -> 0,
        "totalRecords" -> 0,
        "items"        -> Json.arr()
      )
      val result = json.as[RepaymentInterestRepaid]
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
      json.as[RepaymentInterestRepaid].items mustBe Seq.empty
    }

    "must deserialise multiple items" in {
      val json = Json.obj(
        "total"        -> 91.20,
        "totalRecords" -> 2,
        "items" -> Json.arr(
          Json.obj("transactionDate" -> "2024-07-23", "amount" -> 45.60),
          Json.obj("transactionDate" -> "2024-08-15", "amount" -> 45.60)
        )
      )
      json.as[RepaymentInterestRepaid].items must have size 2
    }
  }
}
