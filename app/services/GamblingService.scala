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
import models.StatementOverview
import models.assessments.Assessments
import models.interest.InterestOverview
import models.payments.Payments
import models.penalties.Penalties
import models.reallocations.{Reallocations, ReallocationsDetails}
import models.repayments.{ActualRepayments, RepaymentInterestRepaid, RepaymentsSummary}
import models.returns.ReturnsSubmitted
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class GamblingService @Inject() (connector: GamblingConnector) {

  def getReturnsSubmitted(regime: String, regNumber: String, pageSize: Int, pageNo: Int)(implicit hc: HeaderCarrier): Future[ReturnsSubmitted] =
    connector.getReturnsSubmitted(regime, regNumber, pageSize, pageNo)

  def getReallocationsIn(regime: String, regNumber: String, pageSize: Int, pageNo: Int)(implicit hc: HeaderCarrier): Future[Reallocations] =
    connector.getReallocationsIn(regime, regNumber, pageSize, pageNo)

  def getReallocationsOut(regime: String, regNumber: String, pageSize: Int, pageNo: Int)(implicit hc: HeaderCarrier): Future[Reallocations] =
    connector.getReallocationsOut(regime, regNumber, pageSize, pageNo)

  def getReallocationsDetails(regime: String, regNumber: String)(implicit hc: HeaderCarrier): Future[ReallocationsDetails] =
    connector.getReallocationsDetails(regime, regNumber)

  def getPenalties(regime: String, regNumber: String, pageSize: Int, pageNo: Int)(implicit hc: HeaderCarrier): Future[Penalties] =
    connector.getPenalties(regime, regNumber, pageSize, pageNo)

  def getOtherAssessments(regime: String, regNumber: String, pageSize: Int, pageNo: Int)(implicit hc: HeaderCarrier): Future[Assessments] =
    connector.getOtherAssessments(regime, regNumber, pageSize, pageNo)

  def getAssessmentsWithoutReturns(regime: String, regNumber: String, pageSize: Int, pageNo: Int)(implicit hc: HeaderCarrier): Future[Assessments] =
    connector.getAssessmentsWithoutReturns(regime, regNumber, pageSize, pageNo)

  def getPayments(regime: String, regNumber: String, pageSize: Int, pageNo: Int)(implicit hc: HeaderCarrier): Future[Payments] =
    connector.getPayments(regime, regNumber, pageSize, pageNo)

  def getRepaymentsSummary(regime: String, regNumber: String)(implicit hc: HeaderCarrier): Future[RepaymentsSummary] =
    connector.getRepaymentsSummary(regime, regNumber)

  def getActualRepayments(regime: String, regNumber: String, pageSize: Int, pageNo: Int)(implicit hc: HeaderCarrier): Future[ActualRepayments] =
    connector.getActualRepayments(regime, regNumber, pageSize, pageNo)

  def getRepaymentInterestRepaid(regime: String, regNumber: String, pageSize: Int, pageNo: Int)(implicit
    hc: HeaderCarrier
  ): Future[RepaymentInterestRepaid] =
    connector.getRepaymentInterestRepaid(regime, regNumber, pageSize, pageNo)

  def getStatementOverview(regime: String, regNumber: String)(implicit hc: HeaderCarrier): Future[StatementOverview] =
    connector.getStatementOverview(regime, regNumber)

  def getInterestOverview(regime: String, regNumber: String)(implicit hc: HeaderCarrier): Future[InterestOverview] =
    connector.getInterestOverview(regime, regNumber)
}
