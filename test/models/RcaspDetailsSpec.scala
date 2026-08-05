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
import models.changeDetails.{IndividualRcaspDetailsForComparison, OrganisationRcaspDetailsForComparison}

class RcaspDetailsSpec extends SpecBase {
  "RcaspDetails" - {
    ".forComparison" - {
      "given an IndividualRcaspDetails" - {
        "must return an IndividualRcaspDetailsForComparison" - {
          "when phone number is present" in {
            val result = individualRcaspDetailsViewUpdate.forComparison

            result mustBe Some(
              IndividualRcaspDetailsForComparison(
                isRcaspUser = false,
                firstName = testIndividualName.firstName,
                lastName = testIndividualName.lastName,
                nino = testNiNumber,
                address = testAddressUkRcaspAddress,
                email = testEmail,
                phone = Some(testPhone)
              )
            )
          }

          "when phone number is absent" in {
            val result = individualRcaspDetailsViewUpdate
              .copy(PrimaryContactDetails = Some(rcaspContactDetails.copy(PhoneNumber = None)))
              .forComparison

            result mustBe Some(
              IndividualRcaspDetailsForComparison(
                isRcaspUser = false,
                firstName = testIndividualName.firstName,
                lastName = testIndividualName.lastName,
                nino = testNiNumber,
                address = testAddressUkRcaspAddress,
                email = testEmail,
                phone = None
              )
            )
          }
        }

        "must return None when PrimaryContactDetails is missing" in {
          val result = individualRcaspDetailsViewUpdate.copy(PrimaryContactDetails = None).forComparison

          result mustBe None
        }

        "must return None when TINDetails is missing" in {
          val result = individualRcaspDetailsViewUpdate.copy(TINDetails = None).forComparison

          result mustBe None
        }

        "must return None when TINDetails contains an empty list" in {
          val result = individualRcaspDetailsViewUpdate.copy(TINDetails = Some(List.empty)).forComparison

          result mustBe None
        }
      }

      "given an OrganisationRcaspDetails" - {
        "must return an OrganisationRcaspDetailsForComparison" - {
          "when all fields are present" in {
            val result = organisationRcaspDetailsViewUpdate.forComparison

            result mustBe Some(
              OrganisationRcaspDetailsForComparison(
                isRcaspUser = false,
                rcaspName = testOrgName,
                tradingName = testTradingName,
                utr = Some(testUtr.uniqueTaxPayerReference),
                address = testAddressUkRcaspAddress,
                primaryContactDetails = Some(rcaspContactDetails),
                secondaryContactDetails = Some(rcaspContactDetails.copy(ContactName = "Prof Turo"))
              )
            )
          }

          "when some optional fields are absent" in {
            val result = organisationRcaspDetailsViewUpdate
              .copy(
                PrimaryContactDetails = Some(rcaspContactDetails.copy(PhoneNumber = None)),
                SecondaryContactDetails = None
              )
              .forComparison

            result mustBe Some(
              OrganisationRcaspDetailsForComparison(
                isRcaspUser = false,
                rcaspName = testOrgName,
                tradingName = testTradingName,
                utr = Some(testUtr.uniqueTaxPayerReference),
                address = testAddressUkRcaspAddress,
                primaryContactDetails = Some(rcaspContactDetails.copy(PhoneNumber = None)),
                secondaryContactDetails = None
              )
            )
          }

          "when only registered business fields are present" in {
            val result = organisationRcaspDetailsViewUpdate
              .copy(
                IsRCASPUser = true,
                PrimaryContactDetails = None,
                SecondaryContactDetails = None
              )
              .forComparison

            result mustBe Some(
              OrganisationRcaspDetailsForComparison(
                isRcaspUser = true,
                rcaspName = testOrgName,
                tradingName = testTradingName,
                utr = None,
                address = testAddressUkRcaspAddress,
                primaryContactDetails = None,
                secondaryContactDetails = None
              )
            )
          }
        }

        "must return None when TINDetails is missing" in {
          val result = organisationRcaspDetailsViewUpdate.copy(TINDetails = None).forComparison

          result mustBe None
        }

        "must return None when TINDetails contains an empty list" in {
          val result = organisationRcaspDetailsViewUpdate.copy(TINDetails = Some(List.empty)).forComparison

          result mustBe None
        }
      }
    }
  }
}
