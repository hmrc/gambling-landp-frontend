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

import javax.inject.Inject
import models.{Regime, SessionKeys}
import models.requests.IdentifierRequest
import play.api.mvc.*

import scala.concurrent.{ExecutionContext, Future}

class FakeIdentifierAction @Inject() (bodyParsers: PlayBodyParsers) extends IdentifierAction {

  override def invokeBlock[A](request: Request[A], block: IdentifierRequest[A] => Future[Result]): Future[Result] = {
    val regime = request.session.get(SessionKeys.regime).flatMap(Regime.fromString).getOrElse(Regime.GBD)
    val regNumber = request.session.get(SessionKeys.regNumber).getOrElse("regNumber")
    block(IdentifierRequest(request, "id", regime, regNumber))
  }

  override def parser: BodyParser[AnyContent] =
    bodyParsers.default

  override protected def executionContext: ExecutionContext =
    scala.concurrent.ExecutionContext.Implicits.global
}
