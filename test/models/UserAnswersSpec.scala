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
import models.OrganisationOrIndividual.{Individual, Organisation}
import models.changeDetails.{IndividualRcaspDetailsForComparison, OrganisationRcaspDetailsForComparison}
import pages.UkAddressInUserAnswers
import pages.combined.OrganisationOrIndividualPage
import pages.individual.*
import pages.organisation.*

class UserAnswersSpec extends SpecBase {

  "UserAnswers" - {
    "getRegisteredBusinessOrganisationNameMaybe method" - {
      "must return the cached business name when the user said it was correct" in {
        val ua = emptyUserAnswers
          .withPage(CachedBusinessDetailsPage, cachedBusinessDetails)
          .withPage(RegisteredBusinessIsThisYourBusinessNamePage, true)

        ua.getRegisteredBusinessOrganisationNameMaybe mustBe Some("Timmy Ltd")
      }
      "must return the declared business name when the user said the cached one was incorrect" in {
        val ua = emptyUserAnswers
          .withPage(RegisteredBusinessIsThisYourBusinessNamePage, false)
          .withPage(OverwritableOrganisationName, testOrgName)

        ua.getRegisteredBusinessOrganisationNameMaybe mustBe Some(testOrgName)
      }
      "must return the declared business name when the user did not answer if the cached one was correct" in {
        val ua = emptyUserAnswers
          .withPage(OverwritableOrganisationName, testOrgName)

        ua.getRegisteredBusinessOrganisationNameMaybe mustBe Some(testOrgName)
      }
      "must return None when no cached details exist but the user said it was correct" in {
        val ua = emptyUserAnswers
          .withPage(RegisteredBusinessIsThisYourBusinessNamePage, true)

        ua.getRegisteredBusinessOrganisationNameMaybe mustBe None
      }
      "must return None when no other business name exists but the user said the cached version was incorrect" in {
        val ua = emptyUserAnswers
          .withPage(RegisteredBusinessIsThisYourBusinessNamePage, false)

        ua.getRegisteredBusinessOrganisationNameMaybe mustBe None
      }
    }

    "retrieveRcaspName method" - {
      "must return the name inside IndividualNamePage has said they are an individual" in {
        val ua = emptyUserAnswers
          .withPage(OrganisationOrIndividualPage, Individual)
          .withPage(IndividualNamePage, testIndividualName)

        ua.retrieveRcaspName mustBe Some("Timmy Jimmison")
      }
      "must return OverwritableOrganisationName when the user has said they are an organisation" in {
        val ua = emptyUserAnswers
          .withPage(OrganisationOrIndividualPage, Organisation)
          .withPage(OverwritableOrganisationName, testOrgName)

        ua.retrieveRcaspName mustBe Some(testOrgName)
      }
      "must return OverwritableOrganisationName when the user has not answered OrganisationOrIndividualPage" in {
        val ua = emptyUserAnswers.withPage(OverwritableOrganisationName, testOrgName)

        ua.retrieveRcaspName mustBe Some(testOrgName)
      }
      "must return None when the user has said they are an individual but no name exists" in {
        val ua = emptyUserAnswers.withPage(OrganisationOrIndividualPage, Individual)

        ua.retrieveRcaspName mustBe None
      }
      "must return None when the user has said they are an organisation but no name exists" in {
        val ua = emptyUserAnswers.withPage(OrganisationOrIndividualPage, Organisation)

        ua.retrieveRcaspName mustBe None
      }
      "must return None when the user has not answered OrganisationOrIndividualPage and no name exists" in {
        emptyUserAnswers.retrieveRcaspName mustBe None
      }

    }

    ".getRcaspDetailsForComparison" - {
      "when OrganisationOrIndividual is Individual" - {
        "must return an IndividualRcaspDetailsForComparison" - {
          "when all fields are present" in {
            val ua = emptyUserAnswers
              .withPage(ReportForRegisteredBusinessPage, false)
              .withPage(OrganisationOrIndividualPage, Individual)
              .withPage(IndividualNamePage, testIndividualName)
              .withPage(NiNumberPage, testNiNumber)
              .withPage(UkAddressInUserAnswers, testAddressUk)
              .withPage(IndividualEmailPage, testEmail)
              .withPage(IndividualPhonePage, testPhone)

            ua.getRcaspDetailsForComparison mustBe Some(
              IndividualRcaspDetailsForComparison(
                isRcaspUser = false,
                firstName = testIndividualName.firstName,
                lastName = testIndividualName.lastName,
                nino = testNiNumber,
                address = testAddressUk.toRcaspAddress,
                email = testEmail,
                phone = Some(testPhone)
              )
            )
          }

          "when phone number is absent" in {
            val ua = emptyUserAnswers
              .withPage(ReportForRegisteredBusinessPage, false)
              .withPage(OrganisationOrIndividualPage, Individual)
              .withPage(IndividualNamePage, testIndividualName)
              .withPage(NiNumberPage, testNiNumber)
              .withPage(UkAddressInUserAnswers, testAddressUk)
              .withPage(IndividualEmailPage, testEmail)

            ua.getRcaspDetailsForComparison mustBe Some(
              IndividualRcaspDetailsForComparison(
                isRcaspUser = false,
                firstName = testIndividualName.firstName,
                lastName = testIndividualName.lastName,
                nino = testNiNumber,
                address = testAddressUk.toRcaspAddress,
                email = testEmail,
                phone = None
              )
            )
          }
        }

        "must return None when a mandatory field for comparison is missing" in {
          val ua = emptyUserAnswers
            .withPage(ReportForRegisteredBusinessPage, false)
            .withPage(OrganisationOrIndividualPage, Individual)
            .withPage(NiNumberPage, testNiNumber)
            .withPage(UkAddressInUserAnswers, testAddressUk)
            .withPage(IndividualEmailPage, testEmail)
            .withPage(IndividualPhonePage, testPhone)

          ua.getRcaspDetailsForComparison mustBe None
        }
      }

      "when OrganisationOrIndividual is Organisation" - {
        "must return an OrganisationRcaspDetailsForComparison" - {
          "when all fields are present" in {
            val ua = emptyUserAnswers
              .withPage(ReportForRegisteredBusinessPage, false)
              .withPage(OrganisationOrIndividualPage, Organisation)
              .withPage(OverwritableOrganisationName, testOrgName)
              .withPage(TradingNamePage, testTradingName)
              .withPage(UtrPage, testUtr.uniqueTaxPayerReference)
              .withPage(UkAddressInUserAnswers, testAddressUk)
              .withPage(OrganisationFirstContactNamePage, testIndividualName.fullName)
              .withPage(OrganisationFirstContactEmailPage, testEmail)
              .withPage(OrganisationFirstContactPhoneNumberPage, testPhone)
              .withPage(OrganisationSecondContactNamePage, testIndividualName.fullName)
              .withPage(OrganisationSecondContactEmailPage, testEmail)
              .withPage(OrganisationSecondContactPhoneNumberPage, testPhone)

            ua.getRcaspDetailsForComparison mustBe Some(
              OrganisationRcaspDetailsForComparison(
                isRcaspUser = false,
                rcaspName = testOrgName,
                tradingName = testTradingName,
                utr = testUtr.uniqueTaxPayerReference,
                address = testAddressUk.toRcaspAddress,
                primaryContactDetails = Some(rcaspContactDetails),
                secondaryContactDetails = Some(rcaspContactDetails)
              )
            )
          }

          "when all optional fields are absent" in {
            val ua = emptyUserAnswers
              .withPage(ReportForRegisteredBusinessPage, false)
              .withPage(OrganisationOrIndividualPage, Organisation)
              .withPage(OverwritableOrganisationName, testOrgName)
              .withPage(UtrPage, testUtr.uniqueTaxPayerReference)
              .withPage(UkAddressInUserAnswers, testAddressUk)
              .withPage(OrganisationFirstContactNamePage, testIndividualName.fullName)
              .withPage(OrganisationFirstContactEmailPage, testEmail)

            ua.getRcaspDetailsForComparison mustBe Some(
              OrganisationRcaspDetailsForComparison(
                isRcaspUser = false,
                rcaspName = testOrgName,
                tradingName = testOrgName,
                utr = testUtr.uniqueTaxPayerReference,
                address = testAddressUk.toRcaspAddress,
                primaryContactDetails = Some(rcaspContactDetails.copy(PhoneNumber = None)),
                secondaryContactDetails = None
              )
            )
          }

          "when only phone numbers are absent" in {
            val ua = emptyUserAnswers
              .withPage(ReportForRegisteredBusinessPage, false)
              .withPage(OrganisationOrIndividualPage, Organisation)
              .withPage(OverwritableOrganisationName, testOrgName)
              .withPage(TradingNamePage, testTradingName)
              .withPage(UtrPage, testUtr.uniqueTaxPayerReference)
              .withPage(UkAddressInUserAnswers, testAddressUk)
              .withPage(OrganisationFirstContactNamePage, testIndividualName.fullName)
              .withPage(OrganisationFirstContactEmailPage, testEmail)
              .withPage(OrganisationSecondContactNamePage, testIndividualName.fullName)
              .withPage(OrganisationSecondContactEmailPage, testEmail)

            ua.getRcaspDetailsForComparison mustBe Some(
              OrganisationRcaspDetailsForComparison(
                isRcaspUser = false,
                rcaspName = testOrgName,
                tradingName = testTradingName,
                utr = testUtr.uniqueTaxPayerReference,
                address = testAddressUk.toRcaspAddress,
                primaryContactDetails = Some(rcaspContactDetails.copy(PhoneNumber = None)),
                secondaryContactDetails = Some(rcaspContactDetails.copy(PhoneNumber = None))
              )
            )
          }
        }

        "must return None when a mandatory field for comparison is missing" in {
          val ua = emptyUserAnswers
            .withPage(ReportForRegisteredBusinessPage, false)
            .withPage(OrganisationOrIndividualPage, Organisation)
            .withPage(OverwritableOrganisationName, testOrgName)
            .withPage(TradingNamePage, testTradingName)
            .withPage(UtrPage, testUtr.uniqueTaxPayerReference)
            .withPage(OrganisationFirstContactNamePage, testIndividualName.fullName)
            .withPage(OrganisationFirstContactEmailPage, testEmail)

          ua.getRcaspDetailsForComparison mustBe None
        }
      }

      "when OrganisationOrIndividual is None (registered business)" - {
        "must return an OrganisationRcaspDetailsForComparison" - {
          "when all fields are present and using address from CachedBusinessDetails" in {
            val ua = emptyUserAnswers
              .withPage(ReportForRegisteredBusinessPage, true)
              .withPage(OverwritableOrganisationName, testOrgName)
              .withPage(TradingNamePage, testTradingName)
              .withPage(UtrPage, testUtr.uniqueTaxPayerReference)
              .withPage(RegisteredBusinessIsTheAddressCorrectPage, true)
              .withPage(CachedBusinessDetailsPage, cachedBusinessDetails)

            ua.getRcaspDetailsForComparison mustBe Some(
              OrganisationRcaspDetailsForComparison(
                isRcaspUser = true,
                rcaspName = testOrgName,
                tradingName = testTradingName,
                utr = testUtr.uniqueTaxPayerReference,
                address = testAddressUk.toRcaspAddress,
                primaryContactDetails = None,
                secondaryContactDetails = None
              )
            )
          }

          "when trading name is absent and not using address from CachedBusinessDetails" in {
            val ua = emptyUserAnswers
              .withPage(ReportForRegisteredBusinessPage, true)
              .withPage(OverwritableOrganisationName, testOrgName)
              .withPage(UtrPage, testUtr.uniqueTaxPayerReference)
              .withPage(RegisteredBusinessIsTheAddressCorrectPage, false)
              .withPage(UkAddressInUserAnswers, testAddressUk)

            ua.getRcaspDetailsForComparison mustBe Some(
              OrganisationRcaspDetailsForComparison(
                isRcaspUser = true,
                rcaspName = testOrgName,
                tradingName = testOrgName,
                utr = testUtr.uniqueTaxPayerReference,
                address = testAddressUk.toRcaspAddress,
                primaryContactDetails = None,
                secondaryContactDetails = None
              )
            )
          }
        }

        "must return None when a mandatory field for comparison is missing" in {
          val ua = emptyUserAnswers
            .withPage(ReportForRegisteredBusinessPage, true)
            .withPage(OverwritableOrganisationName, testOrgName)
            .withPage(TradingNamePage, testTradingName)
            .withPage(RegisteredBusinessIsTheAddressCorrectPage, false)

          ua.getRcaspDetailsForComparison mustBe None
        }
      }
    }
  }
}
