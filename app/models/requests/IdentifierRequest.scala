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

package models.requests

import models.Regime
import play.api.mvc.{Request, WrappedRequest}

/** A request that has passed authentication and whose session regime/regNumber have been validated against the user's enrolments. */
case class IdentifierRequest[A](request: Request[A], userId: String, regime: Regime, regNumber: String) extends WrappedRequest[A](request)

/** A request that has only passed the lightweight login check (Organisation/Agent signed in). Used at entry points that establish the session, before
  * a regime/regNumber are known.
  */
case class LoginRequest[A](request: Request[A], userId: String) extends WrappedRequest[A](request)
