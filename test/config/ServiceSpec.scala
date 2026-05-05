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

package config

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class ServiceSpec extends AnyFreeSpec with Matchers {

  private val service = Service(host = "localhost", port = "9000", protocol = "http")

  "Service.baseUrl" - {

    "must construct the base URL from protocol, host and port" in {
      service.baseUrl mustEqual "http://localhost:9000"
    }
  }

  "Service.toString" - {

    "must return the base URL" in {
      service.toString mustEqual "http://localhost:9000"
    }
  }

  "Service.convertToString" - {

    "must implicitly convert a Service to its base URL string" in {
      val url: String = Service.convertToString(service)
      url mustEqual "http://localhost:9000"
    }
  }

  "Service.configLoader" - {

    "must load a Service from configuration" in {
      import com.typesafe.config.ConfigFactory
      val config = ConfigFactory.parseString(
        """
          |myservice {
          |  host     = "myhost"
          |  port     = "8080"
          |  protocol = "https"
          |}
          |""".stripMargin
      )
      val loaded = Service.configLoader.load(config, "myservice")
      loaded mustEqual Service("myhost", "8080", "https")
    }
  }
}
