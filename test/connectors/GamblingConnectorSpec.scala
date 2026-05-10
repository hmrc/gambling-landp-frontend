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

import com.github.tomakehurst.wiremock.client.WireMock.*
import models.reallocations.{ReallocationsIn, ReallocationsInAmount}
import models.returns.{AmountDeclared, ReturnsSubmitted}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.running
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.test.WireMockSupport

import java.time.LocalDate

class GamblingConnectorSpec extends AnyFreeSpec with Matchers with WireMockSupport with ScalaFutures with IntegrationPatience {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val regime = "gbd"
  private val regNumber = "XWM001"
  private val pageSize = 10
  private val pageNo = 1

  private val responseJson =
    s"""
       |{
       |  "periodStartDate": "2024-01-01",
       |  "periodEndDate": "2024-12-31",
       |  "amountDeclared": [
       |    {
       |      "descriptionCode": 1,
       |      "periodStartDate": "2024-01-01",
       |      "periodEndDate": "2024-03-31",
       |      "amount": 1000.50
       |    }
       |  ],
       |  "total": 1000.50,
       |  "totalPeriodRecords": 1
       |}
       |""".stripMargin

  private val expectedResponse = ReturnsSubmitted(
    periodStartDate    = Some(LocalDate.of(2024, 1, 1)),
    periodEndDate      = Some(LocalDate.of(2024, 12, 31)),
    amountDeclared     = Seq(AmountDeclared(Some(1), Some(LocalDate.of(2024, 1, 1)), Some(LocalDate.of(2024, 3, 31)), Some(BigDecimal("1000.5")))),
    total              = Some(BigDecimal("1000.5")),
    totalPeriodRecords = Some(1)
  )

  private def buildApp() =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.gambling.protocol" -> "http",
        "microservice.services.gambling.host"     -> wireMockHost,
        "microservice.services.gambling.port"     -> wireMockPort
      )
      .build()

  "GamblingConnector" - {

    "getReturnsSubmitted" - {

      "must return a deserialized ReturnsSubmitted for a 200 response" in {
        stubFor(
          get(urlEqualTo(s"/gambling/returns-submitted/$regime/$regNumber?pageSize=$pageSize&PageNo=$pageNo"))
            .willReturn(okJson(responseJson))
        )

        val app = buildApp()
        running(app) {
          val connector = app.injector.instanceOf[GamblingConnector]
          val result = connector.getReturnsSubmitted(regime, regNumber, pageSize, pageNo).futureValue

          result mustEqual expectedResponse
        }
      }

      "must forward the correct regime and registration number in the URL" in {
        val otherRegime = "pbd"
        val otherRegNumber = "XWM999"

        stubFor(
          get(urlEqualTo(s"/gambling/returns-submitted/$otherRegime/$otherRegNumber?pageSize=$pageSize&PageNo=$pageNo"))
            .willReturn(okJson(responseJson))
        )

        val app = buildApp()
        running(app) {
          val connector = app.injector.instanceOf[GamblingConnector]
          val result = connector.getReturnsSubmitted(otherRegime, otherRegNumber, pageSize, pageNo).futureValue

          result mustEqual expectedResponse
        }
      }

      "must forward custom pageSize and PageNo query parameters" in {
        val customPageSize = 5
        val customPageNo = 3

        stubFor(
          get(urlEqualTo(s"/gambling/returns-submitted/$regime/$regNumber?pageSize=$customPageSize&PageNo=$customPageNo"))
            .willReturn(okJson(responseJson))
        )

        val app = buildApp()
        running(app) {
          val connector = app.injector.instanceOf[GamblingConnector]
          val result = connector.getReturnsSubmitted(regime, regNumber, customPageSize, customPageNo).futureValue

          result mustEqual expectedResponse
        }
      }
    }

    "getReallocationsIn" - {

      val reallocationsResponseJson =
        s"""
           |{
           |  "periodStartDate": "2024-01-01",
           |  "periodEndDate": "2024-12-31",
           |  "total": 30.80,
           |  "totalPeriodRecords": 1,
           |  "reallocationsInAmount": [
           |    {
           |      "dateProcessed": "2024-07-01",
           |      "amount": 30.80
           |    }
           |  ]
           |}
           |""".stripMargin

      val expectedReallocationsResponse = ReallocationsIn(
        periodStartDate       = Some(LocalDate.of(2024, 1, 1)),
        periodEndDate         = Some(LocalDate.of(2024, 12, 31)),
        total                 = Some(BigDecimal("30.8")),
        totalPeriodRecords    = Some(1),
        reallocationsInAmount = Seq(ReallocationsInAmount(Some(LocalDate.of(2024, 7, 1)), Some(BigDecimal("30.8"))))
      )

      "must return a deserialized ReallocationsIn for a 200 response" in {
        stubFor(
          get(urlEqualTo(s"/gambling/reallocations-in/$regime/$regNumber?pageSize=$pageSize&PageNo=$pageNo"))
            .willReturn(okJson(reallocationsResponseJson))
        )

        val app = buildApp()
        running(app) {
          val connector = app.injector.instanceOf[GamblingConnector]
          val result = connector.getReallocationsIn(regime, regNumber, pageSize, pageNo).futureValue

          result mustEqual expectedReallocationsResponse
        }
      }

      "must forward the correct regime and registration number in the URL" in {
        val otherRegime = "pbd"
        val otherRegNumber = "XWM999"

        stubFor(
          get(urlEqualTo(s"/gambling/reallocations-in/$otherRegime/$otherRegNumber?pageSize=$pageSize&PageNo=$pageNo"))
            .willReturn(okJson(reallocationsResponseJson))
        )

        val app = buildApp()
        running(app) {
          val connector = app.injector.instanceOf[GamblingConnector]
          val result = connector.getReallocationsIn(otherRegime, otherRegNumber, pageSize, pageNo).futureValue

          result mustEqual expectedReallocationsResponse
        }
      }

      "must forward custom pageSize and PageNo query parameters" in {
        val customPageSize = 5
        val customPageNo = 3

        stubFor(
          get(urlEqualTo(s"/gambling/reallocations-in/$regime/$regNumber?pageSize=$customPageSize&PageNo=$customPageNo"))
            .willReturn(okJson(reallocationsResponseJson))
        )

        val app = buildApp()
        running(app) {
          val connector = app.injector.instanceOf[GamblingConnector]
          val result = connector.getReallocationsIn(regime, regNumber, customPageSize, customPageNo).futureValue

          result mustEqual expectedReallocationsResponse
        }
      }
    }
  }
}
