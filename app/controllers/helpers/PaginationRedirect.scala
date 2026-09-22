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

package controllers.helpers

import config.FrontendAppConfig
import models.PaginationParams
import play.api.i18n.Messages
import play.api.mvc.Results.{NotFound, Redirect}
import play.api.mvc.{Call, Request, Result}
import views.html.PageNotFoundView

object PaginationRedirect {

  def redirect(
    pagination: PaginationParams,
    parent: Option[Call],
    page: Int => Call,
    pageNotFoundView: PageNotFoundView,
    appConfig: FrontendAppConfig
  )(implicit request: Request[?], messages: Messages): Option[Result] =
    if (pagination.totalRecords == 0) {
      parent match {
        case Some(parent) => Some(Redirect(parent))
//        case None         => Some(NotFound(pageNotFoundView(appConfig.hmrcOnlineServiceDesk)))
        case None => None
      }
    } else if (
      pagination.pageNo < 1 ||
      pagination.pageNo > pagination.totalPages
    ) {
      Some(Redirect(page(pagination.totalPages)))
    } else {
      None
    }

}
