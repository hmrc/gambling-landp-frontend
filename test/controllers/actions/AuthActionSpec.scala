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

import base.SpecBase
import com.google.inject.Inject
import config.FrontendAppConfig
import controllers.routes
import models.SessionKeys
import play.api.mvc.{Action, AnyContent, BodyParsers, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Retrieval, ~}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class AuthActionSpec extends SpecBase {

  class Harness(authAction: IdentifierAction) {
    def onPageLoad(): Action[AnyContent] = authAction(_ => Results.Ok)
  }

  private val orgEnrolment = Enrolment("HMRC-MGD-ORG", Seq(EnrolmentIdentifier("HMRCMGDRN", "REG123")), "Activated")
  private val testCredentials = Some(Credentials("cred-id", "GovernmentGateway"))

  private def orgConnector(enrolments: Enrolments) =
    new FakeSuccessAuthConnector(Some(AffinityGroup.Organisation), enrolments, testCredentials)

  private def agentConnector(enrolments: Enrolments) =
    new FakeSuccessAuthConnector(Some(AffinityGroup.Agent), enrolments, testCredentials)

  "Auth Action" - {

    "when the user hasn't logged in" - {

      "must redirect the user to log in" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new AuthenticatedIdentifierAction(new FakeFailingAuthConnector(new MissingBearerToken), appConfig, bodyParsers)
          val controller = new Harness(authAction)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value must startWith(appConfig.loginUrl)
        }
      }
    }

    "the user's session has expired" - {

      "must redirect the user to log in" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new AuthenticatedIdentifierAction(new FakeFailingAuthConnector(new BearerTokenExpired), appConfig, bodyParsers)
          val controller = new Harness(authAction)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value must startWith(appConfig.loginUrl)
        }
      }
    }

    "the user doesn't have sufficient enrolments" - {

      "must redirect the user to the unauthorised page" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new AuthenticatedIdentifierAction(new FakeFailingAuthConnector(new InsufficientEnrolments), appConfig, bodyParsers)
          val controller = new Harness(authAction)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe routes.UnauthorisedController.onPageLoad().url
        }
      }
    }

    "the user doesn't have sufficient confidence level" - {

      "must redirect the user to the unauthorised page" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new AuthenticatedIdentifierAction(new FakeFailingAuthConnector(new InsufficientConfidenceLevel), appConfig, bodyParsers)
          val controller = new Harness(authAction)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe routes.UnauthorisedController.onPageLoad().url
        }
      }
    }

    "the user used an unaccepted auth provider" - {

      "must redirect the user to the unauthorised page" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new AuthenticatedIdentifierAction(new FakeFailingAuthConnector(new UnsupportedAuthProvider), appConfig, bodyParsers)
          val controller = new Harness(authAction)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe routes.UnauthorisedController.onPageLoad().url
        }
      }
    }

    "the user has an unsupported affinity group" - {

      "must redirect the user to the unauthorised page" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new AuthenticatedIdentifierAction(new FakeFailingAuthConnector(new UnsupportedAffinityGroup), appConfig, bodyParsers)
          val controller = new Harness(authAction)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad().url)
        }
      }
    }

    "the user has an unsupported credential role" - {

      "must redirect the user to the unauthorised page" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new AuthenticatedIdentifierAction(new FakeFailingAuthConnector(new UnsupportedCredentialRole), appConfig, bodyParsers)
          val controller = new Harness(authAction)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad().url)
        }
      }
    }

    "regime or regNumber missing from session" - {

      "must redirect to access denied when both are missing" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new AuthenticatedIdentifierAction(orgConnector(Enrolments(Set(orgEnrolment))), appConfig, bodyParsers)
          val controller = new Harness(authAction)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe routes.AccessDeniedController.onPageLoad().url
        }
      }

      "must redirect to unauthorised when regime is missing" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new AuthenticatedIdentifierAction(orgConnector(Enrolments(Set(orgEnrolment))), appConfig, bodyParsers)
          val controller = new Harness(authAction)
          val result = controller.onPageLoad()(FakeRequest().withSession(SessionKeys.regNumber -> "REG123"))

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe routes.AccessDeniedController.onPageLoad().url
        }
      }

      "must redirect to unauthorised when regNumber is missing" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new AuthenticatedIdentifierAction(orgConnector(Enrolments(Set(orgEnrolment))), appConfig, bodyParsers)
          val controller = new Harness(authAction)
          val result = controller.onPageLoad()(FakeRequest().withSession(SessionKeys.regime -> "mgd"))

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe routes.AccessDeniedController.onPageLoad().url
        }
      }
    }

    "no credentials returned by auth" - {

      "must redirect to access denied" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new AuthenticatedIdentifierAction(
            new FakeSuccessAuthConnector(Some(AffinityGroup.Organisation), Enrolments(Set.empty), None),
            appConfig,
            bodyParsers
          )
          val controller = new Harness(authAction)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe routes.AccessDeniedController.onPageLoad().url
        }
      }
    }

    "regime and regNumber in session (isAuthorisedForRegime check)" - {

      Seq(
        ("gbd", "HMRC-GTS-GBD"),
        ("pbd", "HMRC-GTS-PBD"),
        ("rgd", "HMRC-GTS-RGD"),
        ("mgd", "HMRC-MGD-ORG")
      ).foreach { case (regime, enrolmentKey) =>
        val (identifierKey, regNumber) = if (regime == "mgd") ("HMRCMGDRN", "MGD123") else ("HMRCGTSGBRN", "GTS123")

        s"must invoke the block when regime=$regime and regNumber in session match the $enrolmentKey enrolment" in {

          val application = applicationBuilder(userAnswers = None).build()

          running(application) {
            val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
            val appConfig = application.injector.instanceOf[FrontendAppConfig]
            val enrolment = Enrolment(enrolmentKey, Seq(EnrolmentIdentifier(identifierKey, regNumber)), "Activated")

            val authAction = new AuthenticatedIdentifierAction(orgConnector(Enrolments(Set(enrolment))), appConfig, bodyParsers)
            val controller = new Harness(authAction)
            val result = controller.onPageLoad()(
              FakeRequest().withSession(SessionKeys.regime -> regime, SessionKeys.regNumber -> regNumber)
            )

            status(result) mustBe OK
          }
        }

        s"must redirect to unauthorised when regNumber does not match $enrolmentKey enrolment" in {

          val application = applicationBuilder(userAnswers = None).build()

          running(application) {
            val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
            val appConfig = application.injector.instanceOf[FrontendAppConfig]
            val enrolment = Enrolment(enrolmentKey, Seq(EnrolmentIdentifier(identifierKey, regNumber)), "Activated")

            val authAction = new AuthenticatedIdentifierAction(orgConnector(Enrolments(Set(enrolment))), appConfig, bodyParsers)
            val controller = new Harness(authAction)
            val result = controller.onPageLoad()(
              FakeRequest().withSession(SessionKeys.regime -> regime, SessionKeys.regNumber -> "WRONG999")
            )

            status(result) mustBe SEE_OTHER
            redirectLocation(result).value mustBe routes.AccessDeniedController.onPageLoad().url
          }
        }
      }

      "must redirect to unauthorised when regime in session does not match the enrolment" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig = application.injector.instanceOf[FrontendAppConfig]
          val gbdEnrolment = Enrolment("HMRC-GTS-GBD", Seq(EnrolmentIdentifier("HMRCGTSGBRN", "GTS123")), "Activated")

          val authAction = new AuthenticatedIdentifierAction(orgConnector(Enrolments(Set(gbdEnrolment))), appConfig, bodyParsers)
          val controller = new Harness(authAction)
          val result = controller.onPageLoad()(
            FakeRequest().withSession(SessionKeys.regime -> "pbd", SessionKeys.regNumber -> "GTS123")
          )

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe routes.AccessDeniedController.onPageLoad().url
        }
      }

      "must redirect to unauthorised when regime in session is not a recognised regime code" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig = application.injector.instanceOf[FrontendAppConfig]
          val enrolment = Enrolment("HMRC-GTS-GBD", Seq(EnrolmentIdentifier("HMRCGTSGBRN", "GTS123")), "Activated")

          val authAction = new AuthenticatedIdentifierAction(orgConnector(Enrolments(Set(enrolment))), appConfig, bodyParsers)
          val controller = new Harness(authAction)
          val result = controller.onPageLoad()(
            FakeRequest().withSession(SessionKeys.regime -> "invalid", SessionKeys.regNumber -> "GTS123")
          )

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe routes.AccessDeniedController.onPageLoad().url
        }
      }

      "must invoke the block for an agent when regime and regNumber are in session" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig = application.injector.instanceOf[FrontendAppConfig]
          val gtsAgentEnrolment = Enrolment("HMRC-GTS-AGNT", Seq(EnrolmentIdentifier("HMRCGTSAGENTREF", "AGENT456")), "Activated")

          val authAction = new AuthenticatedIdentifierAction(agentConnector(Enrolments(Set(gtsAgentEnrolment))), appConfig, bodyParsers)
          val controller = new Harness(authAction)
          val result = controller.onPageLoad()(
            FakeRequest().withSession(SessionKeys.regime -> "gbd", SessionKeys.regNumber -> "CLIENT123")
          )

          status(result) mustBe OK
        }
      }

      "must redirect to unauthorised for an agent when the agent enrolment is inactive" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig = application.injector.instanceOf[FrontendAppConfig]
          val gtsAgentEnrolment = Enrolment("HMRC-GTS-AGNT", Seq(EnrolmentIdentifier("HMRCGTSAGENTREF", "AGENT456")), "NotActivated")

          val authAction = new AuthenticatedIdentifierAction(agentConnector(Enrolments(Set(gtsAgentEnrolment))), appConfig, bodyParsers)
          val controller = new Harness(authAction)
          val result = controller.onPageLoad()(
            FakeRequest().withSession(SessionKeys.regime -> "gbd", SessionKeys.regNumber -> "CLIENT123")
          )

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe routes.AccessDeniedController.onPageLoad().url
        }
      }
    }
  }
}

class FakeFailingAuthConnector @Inject() (exceptionToReturn: Throwable) extends AuthConnector {
  val serviceUrl: String = ""

  override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
    Future.failed(exceptionToReturn)
}

class FakeSuccessAuthConnector @Inject() (
  affinityGroup: Option[AffinityGroup],
  enrolments: Enrolments,
  credentials: Option[Credentials]
) extends AuthConnector {
  val serviceUrl: String = ""

  override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
    Future.successful(new ~(new ~(affinityGroup, enrolments), credentials).asInstanceOf[A])
}
