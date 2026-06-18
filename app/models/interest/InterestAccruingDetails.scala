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

import play.api.libs.json.*

import java.time.LocalDate

final case class InterestAccruingDetailItem(
  descriptionCode: Int,
  amount: BigDecimal,
  interestId: String,
  periodStartDate: LocalDate,
  periodEndDate: LocalDate
)

object InterestAccruingDetailItem {
  implicit val localDateReads: Reads[LocalDate] = Reads.localDateReads("yyyy-MM-dd")
  implicit val format: OFormat[InterestAccruingDetailItem] = Json.format[InterestAccruingDetailItem]
}

final case class InterestAccruingDetails(
  periodStartDate: Option[LocalDate],
  periodEndDate: Option[LocalDate],
  total: BigDecimal,
  totalRecords: Int,
  items: Seq[InterestAccruingDetailItem]
)

object InterestAccruingDetails {
  import InterestAccruingDetailItem.localDateReads
  implicit val format: OFormat[InterestAccruingDetails] = Json.format[InterestAccruingDetails]
}
