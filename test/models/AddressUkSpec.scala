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
import config.Constants.ukCountryCode
import models.countries.CountryUk
import models.requests.AddressDetails

class AddressUkSpec extends SpecBase {

  val ukAddressFull = AddressUk(
    addressLine1 = "123 Main Street",
    addressLine2 = Some("Birmingham"),
    addressLine3 = Some("Hamingburm"),
    townOrCity = "Townington",
    postCode = "B23 2AZ"
  )

  val ukAddressMinimal = AddressUk(
    addressLine1 = "123 Main Street",
    addressLine2 = None,
    addressLine3 = None,
    townOrCity = "Townington",
    postCode = "B23 2AZ"
  )

  val ukAddressAddressLine3 = AddressUk(
    addressLine1 = "123 Main Street",
    addressLine2 = None,
    addressLine3 = Some("Address Line 3"),
    townOrCity = "Townington",
    postCode = "B23 2AZ"
  )

  val ukAddressEmptyStrings = AddressUk(
    addressLine1 = "123 Main Street",
    addressLine2 = Some(""),
    addressLine3 = Some(""),
    townOrCity = "Townington",
    postCode = "B23 2AZ"
  )

  "AddressUk" - {
    "renderHtml" - {
      "must render UK address correctly" in {
        val result = ukAddressFull.renderHtml

        result must include("123 Main Street")
        result must include("Birmingham")
        result must include("Hamingburm")
        result must include("Townington")
        result must include("B23 2AZ")
        result must include("<br>")
        result must include("""<span class="govuk-!-margin-bottom-0">""")
      }

      "must handle minimal address with only required fields" in {
        val result = ukAddressMinimal.renderHtml

        result mustEqual """<span class="govuk-!-margin-bottom-0">123 Main Street</span><br><span class="govuk-!-margin-bottom-0">Townington</span><br>B23 2AZ"""
      }

      "must filter out empty optional fields" in {
        val result = ukAddressEmptyStrings.renderHtml

        result must include("123 Main Street")
        result must not include """<span class="govuk-!-margin-bottom-0"></span>"""
      }
    }

    "toAddressDetails" - {
      "must return address details when given a full address details" in {
        val result                 = ukAddressFull.toAddressDetails
        val expectedAddressDetails = AddressDetails(
          addressLine1 = ukAddressFull.addressLine1,
          addressLine2 = ukAddressFull.addressLine2,
          addressLine3 = ukAddressFull.addressLine3,
          townOrCity = ukAddressFull.townOrCity,
          postalCode = Some(ukAddressFull.postCode),
          countryCode = ukCountryCode
        )
        result mustBe expectedAddressDetails
      }

      "must return address details when given an empty address details" in {
        val result                 = ukAddressMinimal.toAddressDetails
        val expectedAddressDetails = AddressDetails(
          addressLine1 = ukAddressMinimal.addressLine1,
          addressLine2 = ukAddressMinimal.addressLine2,
          addressLine3 = ukAddressMinimal.addressLine3,
          townOrCity = ukAddressMinimal.townOrCity,
          postalCode = Some(ukAddressMinimal.postCode),
          countryCode = ukCountryCode
        )
        result mustBe expectedAddressDetails
      }

      "must return address details with address line 3 shifted to address line 2 if address line 2 is None" in {
        val result                 = ukAddressAddressLine3.toAddressDetails
        val expectedAddressDetails = AddressDetails(
          addressLine1 = ukAddressAddressLine3.addressLine1,
          addressLine2 = ukAddressAddressLine3.addressLine3,
          addressLine3 = None,
          townOrCity = ukAddressAddressLine3.townOrCity,
          postalCode = Some(ukAddressAddressLine3.postCode),
          countryCode = ukCountryCode
        )
        result mustBe expectedAddressDetails
      }
    }

    ".toRcaspAddress" - {
      "must return address details when given a full address details" in {
        val result               = ukAddressFull.toRcaspAddress
        val expectedRcaspAddress = RcaspAddress(
          AddressLine1 = ukAddressFull.addressLine1,
          AddressLine2 = ukAddressFull.addressLine2,
          AddressLine3 = Some(ukAddressFull.townOrCity),
          AddressLine4 = ukAddressFull.addressLine3,
          PostalCode = ukAddressFull.postCode,
          CountryCode = ukCountryCode
        )
        result mustBe expectedRcaspAddress
      }

      "must return address details when given an empty address details" in {
        val result               = ukAddressMinimal.toRcaspAddress
        val expectedRcaspAddress = RcaspAddress(
          AddressLine1 = ukAddressMinimal.addressLine1,
          AddressLine2 = Some(ukAddressMinimal.townOrCity),
          AddressLine3 = None,
          AddressLine4 = None,
          PostalCode = ukAddressMinimal.postCode,
          CountryCode = ukCountryCode
        )
        result mustBe expectedRcaspAddress
      }

      "must return address details when line 3 is defined and line 2 is None" in {
        val result               = ukAddressAddressLine3.toRcaspAddress
        val expectedRcaspAddress = RcaspAddress(
          AddressLine1 = ukAddressAddressLine3.addressLine1,
          AddressLine2 = Some(ukAddressAddressLine3.townOrCity),
          AddressLine3 = ukAddressAddressLine3.addressLine3,
          AddressLine4 = None,
          PostalCode = ukAddressAddressLine3.postCode,
          CountryCode = ukCountryCode
        )
        result mustBe expectedRcaspAddress
      }
    }
  }
}
