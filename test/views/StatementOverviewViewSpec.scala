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

package views

import base.SpecBase
import models.{Regime, StatementOverview}
import org.jsoup.Jsoup
import play.api.i18n.Messages
import play.api.test.FakeRequest
import views.html.StatementOverviewView

import java.time.LocalDate

class StatementOverviewViewSpec extends SpecBase {

  "StatementOverviewView" - {

    "must render the page with the correct heading and registration number" in new Setup {

      val html = view(regNumber, Regime.MGD, overview)
      val doc = Jsoup.parse(html.body)

      doc.title must include(messages("statement.title"))
      doc.select("[data-testid=account-overview-heading]").text mustBe messages("statement.heading", messages(Regime.MGD.messageKey))
      doc.select("[data-testid=registration-number]").text mustBe messages("statement.registrationNumber", regNumber)
    }

    "must render the date range" in new Setup {

      val html = view(regNumber, Regime.MGD, overview)
      val doc = Jsoup.parse(html.body)

      doc.select("[data-testid=date-range]").isEmpty mustBe false
    }

    "must render the statement overview table" in new Setup {

      val html = view(regNumber, Regime.MGD, overview)
      val doc = Jsoup.parse(html.body)

      doc.select("[data-testid=statement-overview-table]").isEmpty mustBe false
    }

    "must show the no balance message when the total is zero" in new Setup {

      val zeroOverview = overview.copy(total = BigDecimal(0))
      val html = view(regNumber, Regime.MGD, zeroOverview)
      val doc = Jsoup.parse(html.body)

      doc.select("[data-testid=no-balance]").isEmpty mustBe false
      doc.select("[data-testid=no-balance]").text mustBe messages("statement.noBalance")
    }

    "must not show the no balance message when the total is positive" in new Setup {

      val positiveOverview = overview.copy(total = BigDecimal("100"))
      val html = view(regNumber, Regime.MGD, positiveOverview)
      val doc = Jsoup.parse(html.body)

      doc.select("[data-testid=no-balance]").isEmpty mustBe true
    }

    "must show the MGD payment link when the total is negative" in new Setup {

      val negativeOverview = overview.copy(total = BigDecimal("-100"))
      val html = view(regNumber, Regime.MGD, negativeOverview)
      val doc = Jsoup.parse(html.body)
      val paymentLink = doc.select("""a[href="https://www.gov.uk/pay-machine-games-duty"]""")

      paymentLink.isEmpty mustBe false
      paymentLink.text mustBe messages("statement.paymentInfo.link")
    }

    Seq(Regime.GBD, Regime.PBD, Regime.RGD).foreach { regime =>

      s"must show the common payment link for $regime when the total is negative" in new Setup {

        val negativeOverview = overview.copy(total = BigDecimal("-100"))
        val html = view(regNumber, regime, negativeOverview)
        val doc = Jsoup.parse(html.body)
        val paymentLink = doc.select("""a[href="https://www.gov.uk/guidance/pay-general-betting-pool-betting-or-remote-gaming-duty"]""")

        paymentLink.isEmpty mustBe false
        paymentLink.text mustBe messages("statement.paymentInfo.link")
      }
    }

    "must not show the MGD payment link when the total is zero" in new Setup {

      val zeroOverview = overview.copy(total = BigDecimal(0))
      val html = view(regNumber, Regime.MGD, zeroOverview)
      val doc = Jsoup.parse(html.body)

      doc.select("""a[href="https://www.gov.uk/pay-machine-games-duty"]""").isEmpty mustBe true
    }

    "must not show the common payment link when the total is zero" in new Setup {

      val zeroOverview = overview.copy(total = BigDecimal(0))
      val html = view(regNumber, Regime.GBD, zeroOverview)
      val doc = Jsoup.parse(html.body)

      doc.select("""a[href="https://www.gov.uk/guidance/pay-general-betting-pool-betting-or-remote-gaming-duty"]""").isEmpty mustBe true
    }

    "must not show the MGD payment link when the total is positive" in new Setup {

      val positiveOverview = overview.copy(total = BigDecimal("100"))
      val html = view(regNumber, Regime.MGD, positiveOverview)
      val doc = Jsoup.parse(html.body)

      doc.select("""a[href="https://www.gov.uk/pay-machine-games-duty"]""").isEmpty mustBe true
    }

    "must render all statement amounts" in new Setup {

      val html = view(regNumber, Regime.MGD, overview)
      val doc = Jsoup.parse(html.body)

      doc.select("[data-testid=balance-brought-forward]").isEmpty mustBe false
      doc.select("[data-testid=returns-total]").isEmpty mustBe false
      doc.select("[data-testid=assessments-in-absence-returns-total]").isEmpty mustBe false
      doc.select("[data-testid=penalties-total]").isEmpty mustBe false
      doc.select("[data-testid=adjustments-total]").isEmpty mustBe false
      doc.select("[data-testid=reallocations-total]").isEmpty mustBe false
      doc.select("[data-testid=other-assessments-total]").isEmpty mustBe false
      doc.select("[data-testid=interest-total]").isEmpty mustBe false
      doc.select("[data-testid=payments-total]").isEmpty mustBe false
      doc.select("[data-testid=repayments-total]").isEmpty mustBe false
      doc.select("[data-testid=balance-total]").isEmpty mustBe false
    }
  }

  trait Setup {

    val app = applicationBuilder().build()
    val view = app.injector.instanceOf[StatementOverviewView]
    val regNumber = "12345678"
    val overview =
      StatementOverview(
        gtrPeriodStartDate = Some(LocalDate.of(2025, 1, 1)),
        gtrPeriodEndDate = Some(LocalDate.of(2025, 3, 31)),
        balance = BigDecimal("100"),
        amountDeclared = BigDecimal("200"),
        assessments = BigDecimal("300"),
        penalties = BigDecimal("400"),
        adjustments = BigDecimal("500"),
        reallocations = BigDecimal("600"),
        otherAssessments = BigDecimal("700"),
        interest = BigDecimal("800"),
        payments = BigDecimal("900"),
        repayments = Some(BigDecimal("100")),
        total = BigDecimal("100")
      )

    implicit val request: play.api.mvc.Request[?] = FakeRequest()

    implicit val messages: Messages =
      app.injector
        .instanceOf[play.api.i18n.MessagesApi]
        .preferred(request)
  }
}