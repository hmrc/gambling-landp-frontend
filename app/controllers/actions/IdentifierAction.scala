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

import com.google.inject.{ImplementedBy, Inject}
import config.FrontendAppConfig
import controllers.routes
import models.{AuthContext, Regime, SessionKeys}
import models.requests.IdentifierRequest
import play.api.Logging
import play.api.mvc.Results.*
import play.api.mvc.*
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.HeaderCarrier
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

    authorised()
      .retrieve(Retrievals.affinityGroup.and(Retrievals.allEnrolments).and(Retrievals.credentials)) {

        case Some(affinityGroup @ (AffinityGroup.Organisation | AffinityGroup.Agent)) ~ enrolments ~ Some(credentials) =>
          val authContext = AuthContext(affinityGroup, enrolments, credentials.providerId)
          val isAuthorisedForRegime = (request.session.get(SessionKeys.regime), request.session.get(SessionKeys.regNumber)) match {
            case (Some(regime), Some(regNumber)) =>
              AuthenticatedIdentifierAction.isAuthorisedForRegime(authContext, regime, regNumber)
            case _ =>
              false
          }

          if (!isAuthorisedForRegime) {
            logger.warn(s"user is not authorised for regime")
            Future.successful(Redirect(routes.AccessDeniedController.onPageLoad()))
          } else {
            block(IdentifierRequest(request, authContext.providerId))
          }

        case _ =>
          Future.successful(Redirect(routes.AccessDeniedController.onPageLoad()))

      } recover {
      case _: NoActiveSession =>
        Redirect(config.loginUrl, Map("continue" -> Seq(config.loginContinueUrl)))
      case _: AuthorisationException =>
        Redirect(routes.UnauthorisedController.onPageLoad())
    }
  }
}

object AuthenticatedIdentifierAction {

  private case class RegimeConfig(
    orgEnrolmentKey: String,
    orgIdentifierKey: String,
    agentEnrolmentKey: String,
    agentIdentifierKey: String
  )

  private val regimeConfig: Map[Regime, RegimeConfig] = Map(
    Regime.MGD -> RegimeConfig("HMRC-MGD-ORG", "HMRCMGDRN", "HMRC-MGD-AGNT", "HMRCMGDAGENTREF"),
    Regime.GBD -> RegimeConfig("HMRC-GTS-GBD", "HMRCGTSGBRN", "HMRC-GTS-AGNT", "HMRCGTSAGENTREF"),
    Regime.PBD -> RegimeConfig("HMRC-GTS-PBD", "HMRCGTSGBRN", "HMRC-GTS-AGNT", "HMRCGTSAGENTREF"),
    Regime.RGD -> RegimeConfig("HMRC-GTS-RGD", "HMRCGTSGBRN", "HMRC-GTS-AGNT", "HMRCGTSAGENTREF")
  )

  private def activeIdentifierValue(
    enrolments: Enrolments,
    enrolmentKey: String,
    identifierKey: String
  ): Option[String] =
    enrolments
      .getEnrolment(enrolmentKey)
      .filter(_.isActivated)
      .flatMap(_.getIdentifier(identifierKey))
      .map(_.value)

  private def isAuthorisedForRegime(authContext: AuthContext, regime: String, regNumber: String): Boolean =
    Regime.fromString(regime).flatMap(regimeConfig.get).exists { config =>
      authContext.affinityGroup match {
        case AffinityGroup.Organisation =>
          activeIdentifierValue(authContext.enrolments, config.orgEnrolmentKey, config.orgIdentifierKey)
            .contains(regNumber)
        case AffinityGroup.Agent =>
          activeIdentifierValue(authContext.enrolments, config.agentEnrolmentKey, config.agentIdentifierKey).isDefined
        case _ => false
      }
    }
}

/** Lightweight auth check used at entry points (e.g. AccountRedirectController) that establish the session. Verifies the user is logged in as an
  * Organisation or Agent — does NOT check session regime/regNumber.
  */
@ImplementedBy(classOf[AuthenticatedLoginAction])
trait LoginAction extends ActionBuilder[IdentifierRequest, AnyContent] with ActionFunction[Request, IdentifierRequest]

class AuthenticatedLoginAction @Inject() (
  override val authConnector: AuthConnector,
  config: FrontendAppConfig,
  val parser: BodyParsers.Default
)(implicit val executionContext: ExecutionContext)
    extends LoginAction
    with AuthorisedFunctions
    with Logging {

  override def invokeBlock[A](request: Request[A], block: IdentifierRequest[A] => Future[Result]): Future[Result] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    authorised()
      .retrieve(Retrievals.affinityGroup.and(Retrievals.credentials)) {

        case Some(AffinityGroup.Organisation | AffinityGroup.Agent) ~ Some(credentials) =>
          block(IdentifierRequest(request, credentials.providerId))

        case _ =>
          Future.successful(Redirect(routes.AccessDeniedController.onPageLoad()))

      } recover {
      case _: NoActiveSession =>
        Redirect(config.loginUrl, Map("continue" -> Seq(config.loginContinueUrl)))
      case _: AuthorisationException =>
        Redirect(routes.UnauthorisedController.onPageLoad())
    }
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
