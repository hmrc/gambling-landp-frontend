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

package controllers.actions

import models.Regime
import play.api.Logging

import java.util.regex.Pattern

object GRNValidator extends Logging {
  private val REF_NO_LENGTH = 7
  private val regNumberPatternGTR: Pattern = "^X[A-Z]{1}[A-Z]{1}[0-9]{11}$".r.pattern
  private val regNumberPatternMGD: Pattern = "^X[A-Za-z]M[0-9]{11}$".r.pattern

  private val WEIGHT_0 = 0
  private val WEIGHT_9 = 9
  private val WEIGHT_10 = 10
  private val WEIGHT_11 = 11
  private val WEIGHT_12 = 12
  private val WEIGHT_13 = 13
  private val WEIGHT_8 = 8
  private val WEIGHT_7 = 7
  private val WEIGHT_6 = 6
  private val WEIGHT_5 = 5
  private val WEIGHT_4 = 4
  private val WEIGHT_3 = 3
  private val WEIGHT_2 = 2

  private val weights =
    List(WEIGHT_0,
         WEIGHT_0,
         WEIGHT_9,
         WEIGHT_10,
         WEIGHT_11,
         WEIGHT_12,
         WEIGHT_13,
         WEIGHT_8,
         WEIGHT_7,
         WEIGHT_6,
         WEIGHT_5,
         WEIGHT_4,
         WEIGHT_3,
         WEIGHT_2
        )
  private val checkChars = "ABCDEFGHXJKLMNYPQRSTZVW"

  def validateRegNoRegime(regime: Regime, regNum: String): Boolean = {
    validateRegNum(regime, regNum) && validateRegime(regime, regNum)
  }

  def validateRegNum(regime: Regime, regNumber: String): Boolean = {
    val regNum = regNumber.toUpperCase().trim
    if (regNum.length == 14) {
      if (regime.equals(Regime.MGD)) {
        if (regNumberPatternMGD.matcher(regNum).matches()) {
          true
        } else {
          logger.warn(s"validateRegNum MGD '$regNum' does not match regEx")
          false
        }
      } else {
        if (regNumberPatternGTR.matcher(regNum).matches()) {
          val char3 = (regNum.charAt(2).toInt - 32) * WEIGHT_9
          val sum = List.range(3, 14).map(x => weights(x) * regNum.charAt(x).asDigit).sum + char3
          val checkChar = checkChars.charAt(sum % 23)
          if (regNum.charAt(1).equals(checkChar)) {
            true
          } else {
            logger.warn(s"validateRegNum GTR '$regNum' has invalid check char ${regNum.charAt(1)}, should be=$checkChar")
            false
          }
        } else {
          logger.warn(s"validateRegNum GTR '$regNum' does not match regNumberPatternGTR")
          false
        }
      }
    } else {
      logger.warn(s"validateRegNum '$regNum' is not 14 chars")
      false
    }
  }

  def validateRegime(regime: Regime, regNumber: String): Boolean =
    val regNum = regNumber.toUpperCase().trim
    if (!regime.equals(Regime.MGD)) {
      if (regNumberPatternGTR.matcher(regNum).matches()) {
        val calculatedRegime = regimeFromRegNo(regNum.takeRight(REF_NO_LENGTH).toLong)
        if (!calculatedRegime.equals(regime.code)) {
          logger.warn(s"$regNum is not within the expected range for $regime")
          false
        } else {
          true
        }
      } else {
        logger.warn(s"validateRegime '$regNum' does not match regEx")
        false
      }
    } else {
      true
    }

  private def regimeFromRegNo(ref: Long) = {
    if (ref >= 3000000 && ref <= 3199999) {
      "gbd"
    } else if (ref >= 3200000 && ref <= 3399999) {
      "pbd"
    } else if (ref >= 3400000 && ref <= 3599999) {
      "rgd"
    } else {
      ""
    }
  }
}
