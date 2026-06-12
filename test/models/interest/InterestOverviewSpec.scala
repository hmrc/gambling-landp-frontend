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

package models.interest

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.*

import java.time.LocalDate

class InterestOverviewSpec extends AnyFreeSpec with Matchers {

  private val interestOverview = InterestOverview(
    periodStartDate         = Some(LocalDate.of(2024, 1, 1)),
    periodEndDate           = Some(LocalDate.of(2024, 12, 31)),
    interestAmount          = BigDecimal(-81.84),
    interestAccruingAmount  = BigDecimal(-25.76),
    repaymentInterestAmount = BigDecimal(41.23),
    total                   = BigDecimal(66.37)
  )

  "InterestOverview" - {

    "must serialise to JSON" in {
      val json = Json.toJson(interestOverview)
      (json \ "periodStartDate").as[String] mustEqual "2024-01-01"
      (json \ "periodEndDate").as[String] mustEqual "2024-12-31"
      (json \ "interestAmount").as[Double] mustEqual -81.84
      (json \ "interestAccruingAmount").as[Double] mustEqual -25.76
      (json \ "repaymentInterestAmount").as[Double] mustEqual 41.23
      (json \ "total").as[Double] mustEqual 66.37
    }

    "must deserialise from JSON" in {
      val json = Json.obj(
        "periodStartDate"         -> "2024-01-01",
        "periodEndDate"           -> "2024-12-31",
        "interestAmount"          -> -81.84,
        "interestAccruingAmount"  -> -25.76,
        "repaymentInterestAmount" -> 41.23,
        "total"                   -> 66.37
      )
      json.as[InterestOverview] mustEqual interestOverview
    }

    "must round-trip through JSON" in {
      val json = Json.toJson(interestOverview)
      json.as[InterestOverview] mustEqual interestOverview
    }
  }
}
