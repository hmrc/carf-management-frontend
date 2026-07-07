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

package models

import base.SpecBase
import models.responses.AddressRegistrationResponse

class RcaspAddressSpec extends SpecBase {
  "RcaspAddress" - {
    ".toAddressRegistrationResponse" - {
      "must populate AddressRegistrationResponse correctly" in {
        val result = rcaspAddress.toAddressRegistrationResponse

        result mustBe AddressRegistrationResponse(
          rcaspAddress.AddressLine1,
          rcaspAddress.AddressLine2,
          rcaspAddress.AddressLine3,
          rcaspAddress.AddressLine4,
          Some(rcaspAddress.PostalCode),
          rcaspAddress.CountryCode
        )
      }
    }

    ".toAddressUk" - {
      "must return an AddressUk when all address lines are present" in {
        val result = rcaspAddress.toAddressUk

        result mustBe Some(
          AddressUk(
            addressLine1 = rcaspAddress.AddressLine1,
            addressLine2 = rcaspAddress.AddressLine2,
            addressLine3 = rcaspAddress.AddressLine3,
            townOrCity = rcaspAddress.AddressLine4.get,
            postCode = rcaspAddress.PostalCode
          )
        )
      }

      "must return an AddressUk when only address lines 1 and 2 are present" in {
        val initial = rcaspAddress.copy(AddressLine3 = None, AddressLine4 = None)
        val result  = initial.toAddressUk

        result mustBe Some(
          AddressUk(
            addressLine1 = initial.AddressLine1,
            addressLine2 = None,
            addressLine3 = None,
            townOrCity = initial.AddressLine2.get,
            postCode = rcaspAddress.PostalCode
          )
        )

      }

      "must return an AddressUk when address line 2 is absent but lines 3-4 are present" in {
        val initial = rcaspAddress.copy(AddressLine2 = None)
        val result  = initial.toAddressUk

        result mustBe Some(
          AddressUk(
            addressLine1 = initial.AddressLine1,
            addressLine2 = initial.AddressLine3,
            addressLine3 = None,
            townOrCity = initial.AddressLine4.get,
            postCode = rcaspAddress.PostalCode
          )
        )
      }

      "must return None when address lines 2-4 are all empty" in {
        val result = rcaspAddressEmptyOptionals.toAddressUk

        result mustBe None
      }

      "must return None when CountryCode is not GB or UK" in {
        val result = rcaspAddress.copy(CountryCode = "US").toAddressUk

        result mustBe None
      }
    }
  }
}
