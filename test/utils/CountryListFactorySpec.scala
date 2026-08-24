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

package utils

import base.SpecBase
import config.FrontendAppConfig
import org.mockito.Mockito.when
import play.api.Environment

class CountryListFactorySpec extends SpecBase {

  val mockEnvironment: Environment     = mock[Environment]
  val mockAppConfig: FrontendAppConfig = mock[FrontendAppConfig]

  "CountryListFactory" - {

    "getDescriptionFromCode" - {

      "must return the country description when the code exists" in {
        val factory = new CountryListFactory(app.environment, app.injector.instanceOf[FrontendAppConfig])

        factory.getDescriptionFromCode("GB") mustBe Some("United Kingdom")
      }

      "must return None when the code does not exist" in {
        val factory = new CountryListFactory(app.environment, app.injector.instanceOf[FrontendAppConfig])

        factory.getDescriptionFromCode("XX") mustBe None
      }

      "must return None when countries.json cannot be found" in {
        when(mockAppConfig.countryCodeJson).thenReturn("non-existent-file.json")
        when(mockEnvironment.resourceAsStream("non-existent-file.json")).thenReturn(None)

        val factory = new CountryListFactory(mockEnvironment, mockAppConfig)

        factory.getDescriptionFromCode("GB") mustBe None
      }
    }
  }
}
