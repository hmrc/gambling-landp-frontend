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

package controllers

import controllers.actions.LoginAction
import models.{Regime, SessionKeys}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject

class AccountRedirectController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  identify: LoginAction
) extends FrontendBaseController {

  def onPageLoad(regime: String, regNumber: String): Action[AnyContent] = identify { implicit request =>
    Regime.fromString(regime) match {
      case None =>
        Redirect(routes.PageNotFoundController.onPageLoad())
      case Some(validRegime) =>
        Redirect(routes.StatementController.onPageLoad())
          .addingToSession(
            SessionKeys.regime    -> validRegime.code,
            SessionKeys.regNumber -> regNumber
          )
    }
  }
}
