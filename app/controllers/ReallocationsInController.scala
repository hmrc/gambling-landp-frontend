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

import config.FrontendAppConfig
import controllers.actions.IdentifierAction
import models.{PaginationParams, Regime, SessionKeys}
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.GamblingService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.{PageNotFoundView, ReallocationsInView}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ReallocationsInController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  identify: IdentifierAction,
  gamblingService: GamblingService,
  view: ReallocationsInView,
  pageNotFoundView: PageNotFoundView,
  appConfig: FrontendAppConfig
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(pageSize: Int = 10, pageNo: Int = 1): Action[AnyContent] = identify.async { implicit request =>
    (request.session.get(SessionKeys.regime), request.session.get(SessionKeys.regNumber)) match {
      case (Some(regimeCode), Some(regNumber)) =>
        Regime.fromString(regimeCode) match {
          case None =>
            Future.successful(Redirect(routes.PageNotFoundController.onPageLoad()))
          case Some(validRegime) =>
            gamblingService.getReallocationsIn(validRegime.code, regNumber, pageSize, pageNo).map { reallocations =>
              val pagination = PaginationParams(reallocations.totalRecords.getOrElse(0), pageSize, pageNo)
              if (pageNo > pagination.totalPages && pagination.totalPages > 0)
                NotFound(pageNotFoundView(appConfig.hmrcOnlineServiceDesk))
              else
                Ok(view(validRegime, regNumber, pagination, reallocations))
            }
        }
      case _ =>
        logger.warn("no regime or regNumber found")
        Future.successful(Redirect(routes.UnauthorisedController.onPageLoad()))
    }
  }
}
