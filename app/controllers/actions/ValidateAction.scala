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

import models.{Regime, SessionKeys}
import play.api.Logging
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionFilter, Request, Result}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ValidateActionImpl @Inject() (implicit val executionContext: ExecutionContext) extends ValidateAction with Logging {

  override protected def filter[A](request: Request[A]): Future[Option[Result]] = {

    (request.session.get(SessionKeys.regime), request.session.get(SessionKeys.regNumber)) match {
      case (Some(regimeCode), Some(regNumber)) =>
        Regime.fromString(regimeCode) match {
          case None =>
            Future.successful(Some(Redirect(controllers.routes.AccessDeniedController.onPageLoad())))
          case Some(validRegime) =>
            if (GRNValidator.validateRegNoRegime(validRegime, regNumber)) {
              Future.successful(None)
            } else {
              logger.warn("Invalid regime")
              Future.successful(Some(Redirect(controllers.routes.AccessDeniedController.onPageLoad())))
            }
        }
      case _ =>
        logger.warn("no regime or regNumber found")
        Future.successful(Some(Redirect(controllers.routes.AccessDeniedController.onPageLoad())))
    }
  }
}

trait ValidateAction extends ActionFilter[Request]

object GRNValidator extends Logging {
  private val REF_NO_LENGTH = 7
  private val regEx = "X[A-Z]{1}[A-Z]{1}[0-9]{11}"

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
    List(WEIGHT_9, WEIGHT_10, WEIGHT_11, WEIGHT_12, WEIGHT_13, WEIGHT_8, WEIGHT_7, WEIGHT_6, WEIGHT_5, WEIGHT_4, WEIGHT_3, WEIGHT_2)
  private val checkChars = List("A", "B", "C", "D", "E", "F", "G", "H", "X", "J", "K", "L", "M", "N", "Y", "P", "Q", "R", "S", "T", "Z", "V", "W")

  def validateRegNoRegime(regime: Regime, regNum: String): Boolean = {
    validateRegNum(regNum) && validateRegime(regime, regNum)
  }

  def validateRegNum(regNumber: String): Boolean = {
    val regNum = regNumber.toUpperCase().trim
    if (regNum.length == 14) {
      if (regNum.matches(regEx)) {
        val char3 = (regNum.substring(2, 3).toCharArray.head.toInt - 32) * WEIGHT_9
        val sum = List.range(1, 11).map(x => weights(x) * regNum.substring(x + 2, x + 3).toInt).sum + char3
        val checkChar = checkChars(sum % 23)
        if (regNum.substring(1, 2).equals(checkChar)) {
          true
        } else {
          logger.warn(s"validateRegNum '$regNum' has invalid check char ${regNum.substring(1, 2)}, should be=$checkChar")
          false
        }
      } else {
        logger.warn(s"validateRegNum '$regNum' does not match regEx")
        false
      }
    } else {
      logger.warn(s"validateRegNum '$regNum' is not 14 chars")
      false
    }
  }

  def validateRegime(regime: Regime, regNumber: String): Boolean =
    val regNum = regNumber.toUpperCase().trim
    if (!regime.equals(Regime.MGD)) {
      if (regNum.matches(regEx)) {
        val calculatedRegime = regimeFromRegNo(regNum.takeRight(REF_NO_LENGTH).toLong)
        if (!calculatedRegime.equals(regime.code)) {
          logger.warn(s"validateRegime Regime does not match RegNum $regime calc=$calculatedRegime $regNum")
          false
        } else {
          logger.info(s"validateRegime Regime matches RegNum '$regime':'$calculatedRegime' '$regNum'")
          true
        }
      } else {
        logger.warn(s"validateRegime RegNum is invalid '$regNum'")
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
