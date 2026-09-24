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

package services

import connectors.GamblingConnector
import models.{ClientListStatus, Regime}
import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

enum AgentClientAuthResult:
  case Authorised, NotAuthorised, NotReady, Failed

@Singleton
class AgentClientAuthService @Inject() (connector: GamblingConnector)(implicit ec: ExecutionContext) extends Logging {

  def authoriseClient(regime: Regime, regNumber: String)(using hc: HeaderCarrier): Future[AgentClientAuthResult] =
    connector
      .startClientListRetrieval(regime.code)
      .flatMap {
        case ClientListStatus.Succeeded =>
          connector.hasClient(regime.code, regNumber).map {
            case true  => AgentClientAuthResult.Authorised
            case false => AgentClientAuthResult.NotAuthorised
          }
        case ClientListStatus.InProgress | ClientListStatus.InitiateDownload =>
          logger.warn(s"""event="client_list_not_ready" regime=${regime.code}""")
          Future.successful(AgentClientAuthResult.NotReady)
        case ClientListStatus.Failed =>
          logger.warn(s"""event="client_list_retrieval_failed" regime=${regime.code}""")
          Future.successful(AgentClientAuthResult.Failed)
      }
      .recover { case NonFatal(e) =>
        logger.error(s"""event="authorise_client_error" regime=${regime.code}""", e)
        AgentClientAuthResult.Failed
      }
}
