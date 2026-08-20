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

  "ValidationUtil validateRegNum for GTR Regimes" - {
    "validateRegNum returns TRUE for valid Reg Nums" in {
      GRNValidator.validateRegNum(Regime.GBD, "XPM00003000000") mustBe true // GBD
      GRNValidator.validateRegNum(Regime.GBD, "XCA00003199999") mustBe true // GBD
      GRNValidator.validateRegNum(Regime.GBD, "XWA00003000000") mustBe true // GBD
      GRNValidator.validateRegNum(Regime.GBD, "XLA33333333333") mustBe true // GBD
      GRNValidator.validateRegNum(Regime.PBD, "XNA00003200000") mustBe true // PBD
      GRNValidator.validateRegNum(Regime.PBD, "XWA00003200111") mustBe true // PBD
      GRNValidator.validateRegNum(Regime.PBD, "XNA00003200000") mustBe true // PBD
      GRNValidator.validateRegNum(Regime.RGD, "XEA00003400000") mustBe true // RGD
      GRNValidator.validateRegNum(Regime.RGD, "XWA00003400222") mustBe true // RGD
    }

    "validateRegNum returns FALSE for invalid Check Digit" in {
      GRNValidator.validateRegNum(Regime.GBD, "XZA00003000000") mustBe false
      GRNValidator.validateRegNum(Regime.GBD, "XZA00003199999") mustBe false
    }

    "validateRegNum returns FALSE for too short" in {
      GRNValidator.validateRegNum(Regime.GBD, "XWA0003000000") mustBe false
    }

    "validateRegNum returns FALSE for very short" in {
      GRNValidator.validateRegNum(Regime.GBD, "XWA001") mustBe false
    }

    "validateRegNum returns FALSE for too long" in {
      GRNValidator.validateRegNum(Regime.GBD, "XWA000003000000") mustBe false
    }

    "validateRegNum returns FALSE for does not match regEx" in {
      GRNValidator.validateRegNum(Regime.GBD, "XWA0000300000Z") mustBe false
      GRNValidator.validateRegNum(Regime.GBD, "1WA00003000000") mustBe false
      GRNValidator.validateRegNum(Regime.GBD, "XW000003000000") mustBe false
    }

    "validateRegNum returns FALSE for Reg Nums with spaces" in {
      GRNValidator.validateRegNum(Regime.GBD, " WA00003000000") mustBe false
      GRNValidator.validateRegNum(Regime.GBD, "X A00003199999") mustBe false
      GRNValidator.validateRegNum(Regime.GBD, "XNA0000 200000") mustBe false
      GRNValidator.validateRegNum(Regime.GBD, "XEA000034000 0") mustBe false
      GRNValidator.validateRegNum(Regime.GBD, "XGM0000312220 ") mustBe false
    }
  }

  "ValidationUtil validateRegNum for MGD Regime" - {
    "validateRegNum returns TRUE for valid Reg Nums" in {
      GRNValidator.validateRegNum(Regime.MGD, "XGM00003122200") mustBe true // MGD
      GRNValidator.validateRegNum(Regime.MGD, "XYM00000000000") mustBe true // MGD
      GRNValidator.validateRegNum(Regime.MGD, "XEM33333333333") mustBe true // MGD
    }

    "validateRegNum returns FALSE for invalid Check Digit" in {
      GRNValidator.validateRegNum(Regime.MGD, "XZA00003000000") mustBe false
      GRNValidator.validateRegNum(Regime.MGD, "XZA00003199999") mustBe false
      GRNValidator.validateRegNum(Regime.MGD, "XAZ00001239456") mustBe false
      GRNValidator.validateRegNum(Regime.MGD, "XAM00001233456") mustBe false
      GRNValidator.validateRegNum(Regime.MGD, "XAM12345678901") mustBe false
    }

    "validateRegNum returns FALSE for too short" in {
      GRNValidator.validateRegNum(Regime.MGD, "XWA0003000000") mustBe false
    }

    "validateRegNum returns FALSE for very short" in {
      GRNValidator.validateRegNum(Regime.MGD, "XWA001") mustBe false
    }

    "validateRegNum returns FALSE for too long" in {
      GRNValidator.validateRegNum(Regime.MGD, "XWA000003000000") mustBe false
    }

    "validateRegNum returns FALSE for does not match regEx" in {
      GRNValidator.validateRegNum(Regime.MGD, "XWA0000300000Z") mustBe false
      GRNValidator.validateRegNum(Regime.MGD, "XWM0000300000Z") mustBe false
      GRNValidator.validateRegNum(Regime.MGD, "1WA00003000000") mustBe false
      GRNValidator.validateRegNum(Regime.MGD, "1WM00003000000") mustBe false
      GRNValidator.validateRegNum(Regime.MGD, "XW000003000000") mustBe false
    }

    "validateRegNum returns FALSE for Reg Nums with spaces" in {
      GRNValidator.validateRegNum(Regime.MGD, " WA00003000000") mustBe false
      GRNValidator.validateRegNum(Regime.MGD, "X A00003199999") mustBe false
      GRNValidator.validateRegNum(Regime.MGD, "XNA0000 200000") mustBe false
      GRNValidator.validateRegNum(Regime.MGD, "XEA000034000 0") mustBe false
      GRNValidator.validateRegNum(Regime.MGD, "XGM0000312220 ") mustBe false
    }
  }

  "GRNValidator validateRegime & validateRegNum for RegNos used by QA team " - {
    "validateRegimeAndRegNo returns TRUE for MGD" in {
      validateRegimeAndRegNo(Regime.MGD, "XZM00013100200") mustBe true
      validateRegimeAndRegNo(Regime.MGD, "XEM00043402007") mustBe true
      validateRegimeAndRegNo(Regime.MGD, "XZM02033001200") mustBe true
      validateRegimeAndRegNo(Regime.MGD, "XVM00033001200") mustBe true
      validateRegimeAndRegNo(Regime.MGD, "XGM00003010200") mustBe true
      validateRegimeAndRegNo(Regime.MGD, "XBM00053001200") mustBe true
      validateRegimeAndRegNo(Regime.MGD, "XLM00043001200") mustBe true
      validateRegimeAndRegNo(Regime.MGD, "XRM00003021200") mustBe true
      validateRegimeAndRegNo(Regime.MGD, "XXM00023001200") mustBe true
      validateRegimeAndRegNo(Regime.MGD, "XFM00003001200") mustBe true
      validateRegimeAndRegNo(Regime.MGD, "XSM00013001200") mustBe true
    }

    "validateRegimeAndRegNo returns TRUE for GBD" in {
      validateRegimeAndRegNo(Regime.GBD, "XMM00003101200") mustBe true
      validateRegimeAndRegNo(Regime.GBD, "XEM02003001200") mustBe true
      validateRegimeAndRegNo(Regime.GBD, "XBA00003000001") mustBe true
      validateRegimeAndRegNo(Regime.GBD, "XFM00003001200") mustBe true
      validateRegimeAndRegNo(Regime.GBD, "XGM00003010200") mustBe true
    }

    "validateRegimeAndRegNo returns TRUE for PBD" in {
      validateRegimeAndRegNo(Regime.PBD, "XSM00013225200") mustBe true
      validateRegimeAndRegNo(Regime.PBD, "XQM02103225200") mustBe true
      validateRegimeAndRegNo(Regime.PBD, "XKM00023202007") mustBe true
      validateRegimeAndRegNo(Regime.PBD, "XVM00013221200") mustBe true
      validateRegimeAndRegNo(Regime.PBD, "XJM00013201200") mustBe true
    }

    "validateRegimeAndRegNo returns TRUE for RGD" in {
      validateRegimeAndRegNo(Regime.RGD, "XBM00013410200") mustBe true
      validateRegimeAndRegNo(Regime.RGD, "XLM00003410200") mustBe true
      validateRegimeAndRegNo(Regime.RGD, "XLM00013402007") mustBe true
      validateRegimeAndRegNo(Regime.RGD, "XCM00023421200") mustBe true
      validateRegimeAndRegNo(Regime.RGD, "XWM00003421200") mustBe true
      validateRegimeAndRegNo(Regime.RGD, "XXM00063421200") mustBe true
    }
  }

  "GRNValidator validateRegNum for RegNos that we know are VALID in production" - {
    "validateRegimeAndRegNo returns TRUE for GTR" in {
      GRNValidator.validateRegNum(Regime.GBD, "XYM00003001213") mustBe true // GBD
      GRNValidator.validateRegNum(Regime.GBD, "XTM00003000512") mustBe true // GBD
      GRNValidator.validateRegNum(Regime.GBD, "XKM00003000195") mustBe true // GBD
      GRNValidator.validateRegNum(Regime.PBD, "XKM00003200218") mustBe true // PBD
      GRNValidator.validateRegNum(Regime.PBD, "XSM00003200104") mustBe true // PBD
      GRNValidator.validateRegNum(Regime.PBD, "XSM00003200290") mustBe true // PBD
      GRNValidator.validateRegNum(Regime.RGD, "XGM00003400594") mustBe true // RGD
      GRNValidator.validateRegNum(Regime.RGD, "XQM00003400116") mustBe true // RGD
      GRNValidator.validateRegNum(Regime.RGD, "XVM00003400600") mustBe true // RGD
    }
    "validateRegimeAndRegNo returns TRUE for MGD" in {
      GRNValidator.validateRegNum(Regime.MGD, "XAM00000001414") mustBe true // MGD
      GRNValidator.validateRegNum(Regime.MGD, "XEM00000000640") mustBe true // MGD
      GRNValidator.validateRegNum(Regime.MGD, "XVM00000000495") mustBe true // MGD
      GRNValidator.validateRegNum(Regime.MGD, "XHM00000000785") mustBe true // MGD
    }
  }

  def validateRegimeAndRegNo(regime: Regime, regNumber: String): Boolean =
    GRNValidator.validateRegime(regime, regNumber) & GRNValidator.validateRegNum(regime, regNumber)
}
