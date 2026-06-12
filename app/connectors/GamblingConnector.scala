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

package connectors

import config.FrontendAppConfig
import models.StatementOverview
import models.assessments.Assessments
import models.interest.{InterestAccruingDetails, InterestOverview}
import models.payments.Payments
import models.penalties.Penalties
import models.reallocations.{Reallocations, ReallocationsDetails}
import models.repayments.{ActualRepayments, RepaymentInterestRepaid, RepaymentsSummary}
import models.returns.ReturnsSubmitted
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GamblingConnector @Inject() (
  config: FrontendAppConfig,
  httpClient: HttpClientV2
)(implicit ec: ExecutionContext) {

  private val baseUrl = url"${config.gamblingBackendBaseUrl}/gambling"

  def getReturnsSubmitted(regime: String, regNumber: String, pageSize: Int, pageNo: Int)(using hc: HeaderCarrier): Future[ReturnsSubmitted] =
    httpClient
      .get(url"$baseUrl/returns-submitted/$regime/$regNumber?pageSize=$pageSize&pageNo=$pageNo")
      .execute[ReturnsSubmitted]

  def getReallocationsIn(regime: String, regNumber: String, pageSize: Int, pageNo: Int)(using hc: HeaderCarrier): Future[Reallocations] =
    httpClient
      .get(url"$baseUrl/reallocations-in/$regime/$regNumber?pageSize=$pageSize&pageNo=$pageNo")
      .execute[Reallocations]

  def getReallocationsOut(regime: String, regNumber: String, pageSize: Int, pageNo: Int)(using hc: HeaderCarrier): Future[Reallocations] =
    httpClient
      .get(url"$baseUrl/reallocations-out/$regime/$regNumber?pageSize=$pageSize&pageNo=$pageNo")
      .execute[Reallocations]

  def getReallocationsDetails(regime: String, regNumber: String)(using hc: HeaderCarrier): Future[ReallocationsDetails] =
    httpClient
      .get(url"$baseUrl/reallocations-details/$regime/$regNumber")
      .execute[ReallocationsDetails]

  def getOtherAssessments(regime: String, regNumber: String, pageSize: Int, pageNo: Int)(using hc: HeaderCarrier): Future[Assessments] =
    httpClient
      .get(url"$baseUrl/other-assessments/$regime/$regNumber?pageSize=$pageSize&pageNo=$pageNo")
      .execute[Assessments]

  def getAssessmentsWithoutReturns(regime: String, regNumber: String, pageSize: Int, pageNo: Int)(using hc: HeaderCarrier): Future[Assessments] =
    httpClient
      .get(url"$baseUrl/assessments-without-returns/$regime/$regNumber?pageSize=$pageSize&pageNo=$pageNo")
      .execute[Assessments]

  def getPenalties(regime: String, regNumber: String, pageSize: Int, pageNo: Int)(using hc: HeaderCarrier): Future[Penalties] =
    httpClient
      .get(url"$baseUrl/penalties/$regime/$regNumber?pageSize=$pageSize&pageNo=$pageNo")
      .execute[Penalties]

  def getPayments(regime: String, regNumber: String, pageSize: Int, pageNo: Int)(using hc: HeaderCarrier): Future[Payments] =
    httpClient
      .get(url"$baseUrl/payments/$regime/$regNumber?pageSize=$pageSize&pageNo=$pageNo")
      .execute[Payments]

  def getRepaymentsSummary(regime: String, regNumber: String)(using hc: HeaderCarrier): Future[RepaymentsSummary] =
    httpClient
      .get(url"$baseUrl/repayment-summary/$regime/$regNumber")
      .execute[RepaymentsSummary]

  def getActualRepayments(regime: String, regNumber: String, pageSize: Int, pageNo: Int)(using hc: HeaderCarrier): Future[ActualRepayments] =
    httpClient
      .get(url"$baseUrl/actual-repayments/$regime/$regNumber?pageSize=$pageSize&pageNo=$pageNo")
      .execute[ActualRepayments]

  def getRepaymentInterestRepaid(regime: String, regNumber: String, pageSize: Int, pageNo: Int)(using
    hc: HeaderCarrier
  ): Future[RepaymentInterestRepaid] =
    httpClient
      .get(url"$baseUrl/repayment-interest-repaid/$regime/$regNumber?pageSize=$pageSize&pageNo=$pageNo")
      .execute[RepaymentInterestRepaid]

  def getStatementOverview(regime: String, regNumber: String)(using hc: HeaderCarrier): Future[StatementOverview] =
    httpClient
      .get(url"$baseUrl/statement-overview/$regime/$regNumber")
      .execute[StatementOverview]

  def getInterestAccruingDetails(regime: String, regNumber: String, interestId: String, pageSize: Int, pageNo: Int)(using
    hc: HeaderCarrier
  ): Future[InterestAccruingDetails] =
    httpClient
      .get(url"$baseUrl/interest-accruing-drilldown/$regime/$regNumber/$interestId?pageSize=$pageSize&pageNo=$pageNo")
      .execute[InterestAccruingDetails]

  def getInterestOverview(regime: String, regNumber: String)(using hc: HeaderCarrier): Future[InterestOverview] =
    httpClient
      .get(url"$baseUrl/interest-overview/$regime/$regNumber")
      .execute[InterestOverview]
}
