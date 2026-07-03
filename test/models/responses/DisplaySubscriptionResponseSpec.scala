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

package models.responses

import base.SpecBase

class DisplaySubscriptionResponseSpec extends SpecBase {

  "DisplaySubscriptionResponse" - {
    ".hasOrganisationContactDetailsMaybe" - {
      "must return false when only individual primary contact is present" in {
        val result = testIndividualDisplaySubscriptionResponse.hasOrganisationContactDetailsMaybe

        result mustBe Some(false)
      }

      "must return true when only organisation primary contact is present" in {
        val result =
          testOrganisationDisplaySubscriptionResponse(Some(testTradingName)).hasOrganisationContactDetailsMaybe

        result mustBe Some(true)
      }

      "must return None when neither individual nor organisation primary contact is present" in {
        val result = testInvalidSubscriptionResponseNeither.hasOrganisationContactDetailsMaybe

        result mustBe None
      }

      "must return None when both individual and organisation primary contact are present" in {
        val result = testInvalidSubscriptionResponseBoth.hasOrganisationContactDetailsMaybe

        result mustBe None
      }
    }
  }
}
