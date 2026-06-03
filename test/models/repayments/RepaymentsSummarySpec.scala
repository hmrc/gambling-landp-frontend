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

class RepaymentsSummarySpec extends AnyFreeSpec with Matchers {

  private val repaymentsSummary = RepaymentsSummary(
    periodStartDate                = Some(LocalDate.of(2024, 1, 1)),
    periodEndDate                  = Some(LocalDate.of(2024, 12, 31)),
    actualRepaymentsAmount         = BigDecimal(71.84),
    repaymentsInterestRepaidAmount = BigDecimal(-35.76),
    total                          = BigDecimal(36.08)
  )

  "RepaymentsSummary" - {

    "must serialise to JSON" in {
      val json = Json.toJson(repaymentsSummary)
      (json \ "periodStartDate").as[String] mustEqual "2024-01-01"
      (json \ "periodEndDate").as[String] mustEqual "2024-12-31"
      (json \ "actualRepaymentsAmount").as[BigDecimal] mustEqual 71.84
      (json \ "repaymentsInterestRepaidAmount").as[BigDecimal] mustEqual -35.76
      (json \ "total").as[BigDecimal] mustEqual 36.08
    }

    "must deserialise from JSON" in {
      val json = Json.obj(
        "periodStartDate"                -> "2024-01-01",
        "periodEndDate"                  -> "2024-12-31",
        "actualRepaymentsAmount"         -> 71.84,
        "repaymentsInterestRepaidAmount" -> -35.76,
        "total"                          -> 36.08
      )
      json.as[RepaymentsSummary] mustEqual repaymentsSummary
    }

    "must round-trip through JSON" in {
      val json = Json.toJson(repaymentsSummary)
      json.as[RepaymentsSummary] mustEqual repaymentsSummary
    }
  }
}
