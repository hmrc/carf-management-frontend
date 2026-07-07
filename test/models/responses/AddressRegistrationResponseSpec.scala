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
import models.RcaspAddress

class AddressRegistrationResponseSpec extends SpecBase {

  val addressFull = AddressRegistrationResponse(
    addressLine1 = "123 Main Street",
    addressLine2 = Some("Birmingham"),
    addressLine3 = Some("Hamingburm"),
    addressLine4 = Some("Townington"),
    postalCode = Some("B23 2AZ"),
    countryCode = "GB"
  )

  val addressMinimal = AddressRegistrationResponse(
    addressLine1 = "123 Main Street",
    addressLine2 = None,
    addressLine3 = None,
    addressLine4 = None,
    postalCode = None,
    countryCode = "GB"
  )

  val addressLine3NoLine2 = AddressRegistrationResponse(
    addressLine1 = "123 Main Street",
    addressLine2 = None,
    addressLine3 = Some("Address Line 3"),
    addressLine4 = None,
    postalCode = Some("B23 2AZ"),
    countryCode = "GB"
  )

  val addressEmptyStrings = AddressRegistrationResponse(
    addressLine1 = "123 Main Street",
    addressLine2 = Some(""),
    addressLine3 = Some(""),
    addressLine4 = Some(""),
    postalCode = Some(""),
    countryCode = "GB"
  )

  "AddressRegistrationResponse" - {
    "renderHtml" - {
      "must render AddressRegistrationResponse correctly" in {
        val result = addressFull.renderHtml

        result must include("123 Main Street")
        result must include("Birmingham")
        result must include("Hamingburm")
        result must include("Townington")
        result must include("B23 2AZ")
        result must include("<br>")
        result must include("""<span class="govuk-!-margin-bottom-0">""")
      }

      "must handle minimal address with only required fields" in {
        val result = addressMinimal.renderHtml

        result mustEqual """123 Main Street"""
      }

      "must filter out empty optional fields" in {
        val result = addressEmptyStrings.renderHtml

        result must include("123 Main Street")
        result must not include """<span class="govuk-!-margin-bottom-0"></span>"""
      }
    }

    ".toRcaspAddress" - {
      "must return address details when given a full address details" in {
        val result               = addressFull.toRcaspAddress
        val expectedRcaspAddress = RcaspAddress(
          AddressLine1 = addressFull.addressLine1,
          AddressLine2 = addressFull.addressLine2,
          AddressLine3 = addressFull.addressLine3,
          AddressLine4 = addressFull.addressLine4,
          PostalCode = addressFull.postalCode.get,
          CountryCode = addressFull.countryCode
        )
        result mustBe Some(expectedRcaspAddress)
      }

      "must return address details when given an empty address details (excluding postcode)" in {
        val result               = addressMinimal.copy(postalCode = Some("B23 2AZ")).toRcaspAddress
        val expectedRcaspAddress = RcaspAddress(
          AddressLine1 = addressMinimal.addressLine1,
          AddressLine2 = None,
          AddressLine3 = None,
          AddressLine4 = None,
          PostalCode = "B23 2AZ",
          CountryCode = addressMinimal.countryCode
        )
        result mustBe Some(expectedRcaspAddress)
      }

      "must return address details when line 3 is defined and line 2 is None" in {
        val result               = addressLine3NoLine2.toRcaspAddress
        val expectedRcaspAddress = RcaspAddress(
          AddressLine1 = addressLine3NoLine2.addressLine1,
          AddressLine2 = addressLine3NoLine2.addressLine3,
          AddressLine3 = None,
          AddressLine4 = None,
          PostalCode = addressLine3NoLine2.postalCode.get,
          CountryCode = addressLine3NoLine2.countryCode
        )
        result mustBe Some(expectedRcaspAddress)
      }

      "must return None when postalCode is missing" in {
        val result = addressMinimal.toRcaspAddress
        result mustBe None
      }
    }
  }
}
