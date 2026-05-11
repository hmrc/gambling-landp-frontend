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
import models.reallocations.{ReallocationsIn, ReallocationsOut}
import models.returns.ReturnsSubmitted
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.HttpReads.Implicits.*
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
      .get(url"$baseUrl/returns-submitted/$regime/$regNumber?pageSize=$pageSize&PageNo=$pageNo")
      .execute[ReturnsSubmitted]

  def getReallocationsIn(regime: String, regNumber: String, pageSize: Int, pageNo: Int)(using hc: HeaderCarrier): Future[ReallocationsIn] =
    httpClient
      .get(url"$baseUrl/reallocations-in/$regime/$regNumber?pageSize=$pageSize&PageNo=$pageNo")
      .execute[ReallocationsIn]

  def getReallocationsOut(regime: String, regNumber: String, pageSize: Int, pageNo: Int)(using hc: HeaderCarrier): Future[ReallocationsOut] =
    httpClient
      .get(url"$baseUrl/reallocations-out/$regime/$regNumber?pageSize=$pageSize&PageNo=$pageNo")
      .execute[ReallocationsOut]
}
