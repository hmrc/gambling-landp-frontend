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

import play.api.libs.functional.syntax.*
import play.api.libs.json.*

import java.time.LocalDate

final case class ReallocationItem(
  dateProcessed: Option[LocalDate],
  amount: Option[BigDecimal]
)

object ReallocationItem {
  implicit val localDateReads: Reads[LocalDate] = Reads.localDateReads("yyyy-MM-dd")
  implicit val format: OFormat[ReallocationItem] = Json.format[ReallocationItem]
}

final case class Reallocations(
  periodStartDate: Option[LocalDate],
  periodEndDate: Option[LocalDate],
  total: Option[BigDecimal],
  totalRecords: Option[Int],
  items: Seq[ReallocationItem]
)

object Reallocations {
  import ReallocationItem.localDateReads

  implicit val format: OFormat[Reallocations] = Json.format[Reallocations]

  private def reads(itemsKey: String): Reads[Reallocations] = (
    (__ \ "periodStartDate").readNullable[LocalDate] and
      (__ \ "periodEndDate").readNullable[LocalDate] and
      (__ \ "total").readNullable[BigDecimal] and
      (__ \ "totalPeriodRecords").readNullable[Int] and
      (__ \ itemsKey).read[Seq[ReallocationItem]]
  )(Reallocations.apply)

  val readsIn: Reads[Reallocations] = reads("reallocationsInAmount")
  val readsOut: Reads[Reallocations] = reads("reallocationsOutAmount")
}
