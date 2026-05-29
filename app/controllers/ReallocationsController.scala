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

import controllers.actions.IdentifierAction
import models.{Regime, SessionKeys}
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.GamblingService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.ReallocationsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ReallocationsController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  identify: IdentifierAction,
  gamblingService: GamblingService,
  view: ReallocationsView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(): Action[AnyContent] = identify.async { implicit request =>
    (request.session.get(SessionKeys.regime), request.session.get(SessionKeys.regNumber)) match {
      case (Some(regimeCode), Some(regNumber)) =>
        Regime.fromString(regimeCode).fold(Future.successful(Redirect(routes.PageNotFoundController.onPageLoad()))) { validRegime =>
          gamblingService.getReallocationsDetails(validRegime.code, regNumber).map(details => Ok(view(details)))
        }
      case _ =>
        logger.warn("no regime or regNumber found")
        Future.successful(Redirect(routes.UnauthorisedController.onPageLoad()))
    }
  }
}
