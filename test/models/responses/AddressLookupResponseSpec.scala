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
import models.errors.{ConversionError, InvalidCountryCode}
import models.{AddressAndUPRN, AddressUk}

class AddressLookupResponseSpec extends SpecBase {

  def createAddressLookupResponse(lines: List[String]) =
    AddressLookupResponse(
      id = "test id",
      uprn = testUPRN,
      address = AddressRecord(
        lines = lines,
        town = "Testington",
        postcode = testPostcode,
        country = CountryRecord(code = "GB", name = "United Kingdom")
      )
    )

  "AddressLookupResponse" - {
    "toDomainAddressAndUprn method" - {
      "should successfully convert the address api response to a domain model" - {
        "when lines has one value" in {
          val testAddressLookupResponse = createAddressLookupResponse(lines = List("Line 1"))

          val result = testAddressLookupResponse.toDomainAddressAndUprn

          result mustBe Right(
            AddressAndUPRN(
              address = AddressUk(
                addressLine1 = "Line 1",
                addressLine2 = None,
                addressLine3 = None,
                townOrCity = "Testington",
                postCode = testPostcode
              ),
              UPRN = testUPRN
            )
          )
        }
        "when lines has two values" in {
          val testAddressLookupResponse = createAddressLookupResponse(lines = List("Line 1", "Line 2"))

          val result = testAddressLookupResponse.toDomainAddressAndUprn

          result mustBe Right(
            AddressAndUPRN(
              address = AddressUk(
                addressLine1 = "Line 1",
                addressLine2 = Some("Line 2"),
                addressLine3 = None,
                townOrCity = "Testington",
                postCode = testPostcode
              ),
              UPRN = testUPRN
            )
          )
        }
        "when lines has three values" in {
          val testAddressLookupResponse = createAddressLookupResponse(lines = List("Line 1", "Line 2", "Line 3"))

          val result = testAddressLookupResponse.toDomainAddressAndUprn

          result mustBe Right(
            AddressAndUPRN(
              address = AddressUk(
                addressLine1 = "Line 1",
                addressLine2 = Some("Line 2"),
                addressLine3 = Some("Line 3"),
                townOrCity = "Testington",
                postCode = testPostcode
              ),
              UPRN = testUPRN
            )
          )
        }
        "when lines has four values, only populate the first three because lines has a max size of three in the API spec" in {
          val testAddressLookupResponse =
            createAddressLookupResponse(lines = List("Line 1", "Line 2", "Line 3", "Line 4"))

          val result = testAddressLookupResponse.toDomainAddressAndUprn

          result mustBe Right(
            AddressAndUPRN(
              address = AddressUk(
                addressLine1 = "Line 1",
                addressLine2 = Some("Line 2"),
                addressLine3 = Some("Line 3"),
                townOrCity = "Testington",
                postCode = testPostcode
              ),
              UPRN = testUPRN
            )
          )
        }
      }

      "must return a InvalidCountryCode if the address code does not equal an accepted UK code" in {
        val result = nonUkAddressLookupResponse.toDomainAddressAndUprn

        result.swap.toOption.get mustEqual InvalidCountryCode
      }
      "must return a ConversionError if the address lines are empty" in {
        val result = createAddressLookupResponse(List.empty).toDomainAddressAndUprn

        result.swap.toOption.get mustEqual ConversionError
      }
    }
  }
}
