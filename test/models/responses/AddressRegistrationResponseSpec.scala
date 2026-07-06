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
  }
}
