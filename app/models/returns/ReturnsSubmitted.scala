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

import play.api.libs.json.{Json, Reads}

import java.time.LocalDate

case class AmountDeclared(
  descriptionCode: Option[Int],
  periodStartDate: Option[LocalDate],
  periodEndDate: Option[LocalDate],
  amount: Option[BigDecimal]
)

object AmountDeclared {
  implicit val localDateReads: Reads[LocalDate] = Reads.localDateReads("yyyy-MM-dd")
  implicit val reads: Reads[AmountDeclared] = Json.reads[AmountDeclared]
}

case class ReturnsSubmitted(
  periodStartDate: Option[LocalDate],
  periodEndDate: Option[LocalDate],
  total: Option[BigDecimal],
  totalPeriodRecords: Option[Int],
  amountDeclared: Seq[AmountDeclared]
)

object ReturnsSubmitted {
  import AmountDeclared.localDateReads
  implicit val reads: Reads[ReturnsSubmitted] = Json.reads[ReturnsSubmitted]
}
