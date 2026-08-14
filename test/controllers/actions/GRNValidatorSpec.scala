/*
 * Copyright 2025 HM Revenue & Customs
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
import models.Regime

class GRNValidatorSpec extends SpecBase {

  "ValidationAction validateRegime" - {
    "validateRegime returns TRUE for GBD" in {
      GRNValidator.validateRegime(Regime.GBD, "XBA00003000000") mustBe true
      GRNValidator.validateRegime(Regime.GBD, "XBA00003199999") mustBe true
    }

    "validateRegime returns FALSE for GBD" in {
      GRNValidator.validateRegime(Regime.GBD, "XBA00002999999") mustBe false
      GRNValidator.validateRegime(Regime.GBD, "XBA00003200000") mustBe false
      GRNValidator.validateRegime(Regime.PBD, "XBA00003199999") mustBe false
    }

    "validateRegime returns TRUE for PBD" in {
      GRNValidator.validateRegime(Regime.PBD, "XBA00003200000") mustBe true
      GRNValidator.validateRegime(Regime.PBD, "XBA00003399999") mustBe true
    }

    "validateRegime returns FALSE for PBD" in {
      GRNValidator.validateRegime(Regime.PBD, "XBA00003199999") mustBe false
      GRNValidator.validateRegime(Regime.PBD, "XBA00003400000") mustBe false
      GRNValidator.validateRegime(Regime.GBD, "XBA00003200000") mustBe false
    }

    "validateRegime returns TRUE for RGD" in {
      GRNValidator.validateRegime(Regime.RGD, "XBA00003400000") mustBe true
      GRNValidator.validateRegime(Regime.RGD, "XBA00003599999") mustBe true
    }

    "validateRegime returns FALSE for RGD" in {
      GRNValidator.validateRegime(Regime.RGD, "XBA00003399999") mustBe false
      GRNValidator.validateRegime(Regime.RGD, "XBA00003600000") mustBe false
      GRNValidator.validateRegime(Regime.GBD, "XBA00003400000") mustBe false
    }

    "validateRegime returns TRUE for MGD" in {
      GRNValidator.validateRegime(Regime.MGD, "XBA00000400000") mustBe true
      GRNValidator.validateRegime(Regime.MGD, "XBA00003500000") mustBe true
    }

    "validateRegime returns FALSE for short RegNums" in {
      GRNValidator.validateRegime(Regime.GBD, "XBA0002999999") mustBe false
      GRNValidator.validateRegime(Regime.GBD, "XBA123") mustBe false
      GRNValidator.validateRegime(Regime.PBD, "XBA") mustBe false
    }

    "validateRegime returns FALSE for Reg Nums with spaces" in {
      GRNValidator.validateRegime(Regime.GBD, " WA00003000000") mustBe false
      GRNValidator.validateRegime(Regime.GBD, "X A00003199999") mustBe false
      GRNValidator.validateRegime(Regime.GBD, "XNA0000 200000") mustBe false
      GRNValidator.validateRegime(Regime.GBD, "XEA000034000 0") mustBe false
      GRNValidator.validateRegime(Regime.GBD, "XGM0000312220 ") mustBe false
    }
  }

  "ValidationAction validateRegNum" - {
    "validateRegNum returns TRUE for valid Reg Nums" in {
      GRNValidator.validateRegNum("XWA00003000000") mustBe true // GBD
      GRNValidator.validateRegNum("XHA00003199999") mustBe true // GBD
      GRNValidator.validateRegNum("XNA00003200000") mustBe true // PBD
      GRNValidator.validateRegNum("XEA00003400000") mustBe true // RGD
      GRNValidator.validateRegNum("XGM00003122200") mustBe true // MGD
    }

    "validateRegNum returns FALSE for invalid Check Digit" in {
      GRNValidator.validateRegNum("XZA00003000000") mustBe false
      GRNValidator.validateRegNum("XZA00003199999") mustBe false
    }

    "validateRegNum returns FALSE for too short" in {
      GRNValidator.validateRegNum("XWA0003000000") mustBe false
    }

    "validateRegNum returns FALSE for very short" in {
      GRNValidator.validateRegNum("XWA001") mustBe false
    }

    "validateRegNum returns FALSE for too long" in {
      GRNValidator.validateRegNum("XWA000003000000") mustBe false
    }

    "validateRegNum returns FALSE for does not match regEx" in {
      GRNValidator.validateRegNum("XWA0000300000Z") mustBe false
      GRNValidator.validateRegNum("1WA00003000000") mustBe false
      GRNValidator.validateRegNum("XW000003000000") mustBe false
    }

    "validateRegNum returns FALSE for Reg Nums with spaces" in {
      GRNValidator.validateRegNum(" WA00003000000") mustBe false
      GRNValidator.validateRegNum("X A00003199999") mustBe false
      GRNValidator.validateRegNum("XNA0000 200000") mustBe false
      GRNValidator.validateRegNum("XEA000034000 0") mustBe false
      GRNValidator.validateRegNum("XGM0000312220 ") mustBe false
    }
  }
}
