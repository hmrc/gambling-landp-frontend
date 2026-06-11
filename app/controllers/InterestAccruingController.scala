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
import models.interest.InterestAccruingDetails
import models.{PaginationParams, Regime, SessionKeys}
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.GamblingService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.{InterestAccruingView, PageNotFoundView}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class InterestAccruingController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  identify: IdentifierAction,
  gamblingService: GamblingService,
  view: InterestAccruingView,
  pageNotFoundView: PageNotFoundView,
  appConfig: FrontendAppConfig
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(interestId: String, pageSize: Int = 10, pageNo: Int = 1): Action[AnyContent] = identify.async { implicit request =>
    (request.session.get(SessionKeys.regime), request.session.get(SessionKeys.regNumber)) match {
      case (Some(regimeCode), Some(regNumber)) =>
        Regime.fromString(regimeCode).fold(Future.successful(Redirect(routes.PageNotFoundController.onPageLoad()))) { validRegime =>
          gamblingService.getInterestAccruing(validRegime.code, regNumber, interestId, pageSize, pageNo).map {
            case interestAccruing @ InterestAccruingDetails(_, _, _, _, _, items) if items.nonEmpty =>
              val pagination = PaginationParams(interestAccruing.totalRecords, pageSize, pageNo)
              if (pagination.isOutOfRange)
                NotFound(pageNotFoundView(appConfig.hmrcOnlineServiceDesk))
              else
                Ok(view(interestId, pagination, interestAccruing))
            case _ => Redirect(routes.PageNotFoundController.onPageLoad())
          }
        }
      case _ =>
        logger.warn("no regime or regNumber found")
        Future.successful(Redirect(routes.UnauthorisedController.onPageLoad()))
    }
  }
}
