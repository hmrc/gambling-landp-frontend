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

package controllers.helpers

import base.SpecBase
import models.PaginationParams
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Call
import play.api.test.Helpers.*

class PaginationRedirectControllerSpec extends SpecBase with MockitoSugar {
  private val parent: Call = Call("GET", "/parent")
  private val page: Int => Call = lastPage => Call("GET", s"/page/$lastPage")

  private def locationOf(result: play.api.mvc.Result): Option[String] =
    result.header.headers.get("Location")

  ".redirect" - {

    "when there are no records" - {

      "must redirect to the parent when a parent is provided" in {
        val pagination = PaginationParams(totalRecords = 0, pageSize = 10, pageNo = 1)

        val result = PaginationRedirect.redirect(pagination, Some(parent), page)

        result.map(_.header.status) mustBe Some(SEE_OTHER)
        result.flatMap(locationOf) mustBe Some("/parent")
      }

      "must return None when no parent is provided" in {
        val pagination = PaginationParams(totalRecords = 0, pageSize = 10, pageNo = 1)

        PaginationRedirect.redirect(pagination, None, page) mustBe None
      }
    }

    "when the requested page is out of range" - {

      "must redirect to the last page when pageNo is greater than totalPages" in {
        val pagination = PaginationParams(totalRecords = 25, pageSize = 10, pageNo = 99)

        val result = PaginationRedirect.redirect(pagination, Some(parent), page)

        result.map(_.header.status) mustBe Some(SEE_OTHER)
        result.flatMap(locationOf) mustBe Some("/page/3")
      }

      "must redirect to the last page when pageNo is less than 1" in {
        val pagination = PaginationParams(totalRecords = 25, pageSize = 10, pageNo = 0)

        val result = PaginationRedirect.redirect(pagination, Some(parent), page)

        result.map(_.header.status) mustBe Some(SEE_OTHER)
        result.flatMap(locationOf) mustBe Some("/page/3")
      }

    }

    "must return None when the page is in range and there are records" in {
      val pagination = PaginationParams(totalRecords = 25, pageSize = 10, pageNo = 2)

      PaginationRedirect.redirect(pagination, Some(parent), page) mustBe None
    }
  }
}
