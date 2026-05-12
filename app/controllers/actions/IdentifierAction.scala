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

import com.google.inject.Inject
import config.FrontendAppConfig
import controllers.routes
import models.SessionKeys
import models.requests.IdentifierRequest
import play.api.Logging
import play.api.mvc.Results.*
import play.api.mvc.*
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.{HeaderCarrier, UnauthorizedException}
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

trait IdentifierAction extends ActionBuilder[IdentifierRequest, AnyContent] with ActionFunction[Request, IdentifierRequest]

class AuthenticatedIdentifierAction @Inject() (
  override val authConnector: AuthConnector,
  config: FrontendAppConfig,
  val parser: BodyParsers.Default
)(implicit val executionContext: ExecutionContext)
    extends IdentifierAction
    with AuthorisedFunctions
    with Logging {

  override def invokeBlock[A](request: Request[A], block: IdentifierRequest[A] => Future[Result]): Future[Result] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    authorised().retrieve(Retrievals.internalId.and(Retrievals.allEnrolments)) {
      case Some(internalId) ~ enrolments =>
        request.session.get(SessionKeys.regNumber) match {
          case Some(regNumber) if !AuthenticatedIdentifierAction.isValidEnrolment(enrolments, regNumber) =>
            logger.warn(s"regNumber in session does not match auth enrolments")
            Future.successful(Redirect(routes.UnauthorisedController.onPageLoad()))
          case _ =>
            block(IdentifierRequest(request, internalId))
        }
      case None ~ _ =>
        throw new UnauthorizedException("Unable to retrieve internal Id")
    } recover {
      case _: NoActiveSession =>
        Redirect(config.loginUrl, Map("continue" -> Seq(config.loginContinueUrl)))
      case _: AuthorisationException =>
        Redirect(routes.UnauthorisedController.onPageLoad())
    }
  }
}

object AuthenticatedIdentifierAction {
  private val organisationEnrolmentKey = "HMRC-MGD-ORG"
  private val organisationIdentifierKey = "HMRCMGDRN"
  private val agentEnrolmentKey = "HMRC-MGD-AGNT"
  private val agentIdentifierKey = "HMRCMGDAGENTREF"

  def isValidEnrolment(enrolments: Enrolments, regNumber: String): Boolean =
    hasActiveEnrolmentWithValue(enrolments, organisationEnrolmentKey, organisationIdentifierKey, regNumber) ||
      hasActiveEnrolmentWithValue(enrolments, agentEnrolmentKey, agentIdentifierKey, regNumber)

  private def hasActiveEnrolmentWithValue(
    enrolments: Enrolments,
    enrolmentKey: String,
    identifierKey: String,
    value: String
  ): Boolean =
    enrolments.getEnrolment(enrolmentKey).exists { enrolment =>
      enrolment.isActivated && enrolment.getIdentifier(identifierKey).exists(_.value == value)
    }
}

class SessionIdentifierAction @Inject() (
  val parser: BodyParsers.Default
)(implicit val executionContext: ExecutionContext)
    extends IdentifierAction {

  override def invokeBlock[A](request: Request[A], block: IdentifierRequest[A] => Future[Result]): Future[Result] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    hc.sessionId match {
      case Some(session) =>
        block(IdentifierRequest(request, session.value))
      case None =>
        Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
    }
  }
}
