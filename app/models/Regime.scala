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

package models

final case class RefRange(min: Long, max: Long) {
  def contains(ref: Long): Boolean = ref >= min && ref <= max
}

sealed trait Regime(val code: String, val messageKey: String, val refRange: Option[RefRange])

object Regime {
  case object GBD extends Regime("gbd", "regime.gbd", Some(RefRange(3000000, 3199999)))
  case object PBD extends Regime("pbd", "regime.pbd", Some(RefRange(3200000, 3399999)))
  case object RGD extends Regime("rgd", "regime.rgd", Some(RefRange(3400000, 3599999)))
  case object MGD extends Regime("mgd", "regime.mgd", None)

  val values: Seq[Regime] = Seq(GBD, PBD, RGD, MGD)

  def fromString(s: String): Option[Regime] =
    values.find(_.code == s.toLowerCase)

  def fromRegNum(ref: Long): Option[Regime] =
    values.find(_.refRange.exists(_.contains(ref)))
}
