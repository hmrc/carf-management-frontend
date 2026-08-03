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
import models.ChangeMode
import models.OrganisationOrIndividual.{Individual, Organisation}
import pages.UkAddressInUserAnswers
import pages.changeDetails.ChangeRcaspCachedDetails
import pages.combined.OrganisationOrIndividualPage
import pages.individual.*
import pages.organisation.*
import play.api.i18n.Messages
import viewmodels.Section

class DetailsHelperSpec extends SpecBase {

  val testHelper                  = new DetailsHelper()
  implicit val messages: Messages = messages(app)

  "DetailsHelperSpec" - {
    "haveAnswersChangedFromApi" - {
      "must return None when ChangeRcaspCachedDetails is missing" in {
        val userAnswers = emptyUserAnswers
          .withPage(ReportForRegisteredBusinessPage, false)
          .withPage(OrganisationOrIndividualPage, Individual)
          .withPage(IndividualNamePage, testIndividualName)
          .withPage(NiNumberPage, testNiNumber)
          .withPage(UkAddressInUserAnswers, testAddressUk)
          .withPage(IndividualEmailPage, testEmail)
          .withPage(IndividualHavePhonePage, true)
          .withPage(IndividualPhonePage, testPhone)

        val result = testHelper.haveAnswersChangedFromApi(userAnswers)
        result mustBe None
      }

      "must return None when RcaspDetails .forComparison returns None" in {
        val userAnswers = emptyUserAnswers
          .withPage(ReportForRegisteredBusinessPage, false)
          .withPage(OrganisationOrIndividualPage, Individual)
          .withPage(IndividualNamePage, testIndividualName)
          .withPage(NiNumberPage, testNiNumber)
          .withPage(UkAddressInUserAnswers, testAddressUk)
          .withPage(IndividualEmailPage, testEmail)
          .withPage(IndividualPhonePage, testPhone)
          .withPage(ChangeRcaspCachedDetails, individualRcaspDetailsViewUpdate.copy(TINDetails = None))

        val result = testHelper.haveAnswersChangedFromApi(userAnswers)
        result mustBe None
      }

      "must return None when UserAnswers .getRcaspDetailsForComparison returns None" in {
        val userAnswers = emptyUserAnswers
          .withPage(ReportForRegisteredBusinessPage, false)
          .withPage(OrganisationOrIndividualPage, Individual)
          .withPage(IndividualNamePage, testIndividualName)
          .withPage(UkAddressInUserAnswers, testAddressUk)
          .withPage(IndividualEmailPage, testEmail)
          .withPage(IndividualPhonePage, testPhone)
          .withPage(ChangeRcaspCachedDetails, individualRcaspDetailsViewUpdate)

        val result = testHelper.haveAnswersChangedFromApi(userAnswers)
        result mustBe None
      }

      "when the answers have not changed" - {
        "when OrganisationOrIndividual is Individual" in {
          val userAnswers = emptyUserAnswers
            .withPage(ReportForRegisteredBusinessPage, false)
            .withPage(OrganisationOrIndividualPage, Individual)
            .withPage(IndividualNamePage, testIndividualName)
            .withPage(NiNumberPage, testNiNumber)
            .withPage(UkAddressInUserAnswers, testAddressUk)
            .withPage(IndividualEmailPage, testEmail)
            .withPage(IndividualPhonePage, testPhone)
            .withPage(ChangeRcaspCachedDetails, individualRcaspDetailsViewUpdate)

          val result = testHelper.haveAnswersChangedFromApi(userAnswers)
          result mustBe Some(false)
        }

        "when OrganisationOrIndividual is Organisation" in {
          val userAnswers = emptyUserAnswers
            .withPage(ReportForRegisteredBusinessPage, false)
            .withPage(OrganisationOrIndividualPage, Organisation)
            .withPage(OverwritableOrganisationName, testOrgName)
            .withPage(TradingNamePage, testTradingName)
            .withPage(UtrPage, testUtr.uniqueTaxPayerReference)
            .withPage(UkAddressInUserAnswers, testAddressUk)
            .withPage(OrganisationFirstContactNamePage, testIndividualName.fullName)
            .withPage(OrganisationFirstContactEmailPage, testEmail)
            .withPage(OrganisationFirstContactPhoneNumberPage, testPhone)
            .withPage(OrganisationSecondContactNamePage, "Prof Turo")
            .withPage(OrganisationSecondContactEmailPage, testEmail)
            .withPage(OrganisationSecondContactPhoneNumberPage, testPhone)
            .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate)

          val result = testHelper.haveAnswersChangedFromApi(userAnswers)
          result mustBe Some(false)
        }

        "when OrganisationOrIndividual is None (registered business)" in {
          val cachedRcaspDetails = organisationRcaspDetailsViewUpdate.copy(
            IsRCASPUser = true,
            PrimaryContactDetails = None,
            SecondaryContactDetails = None
          )

          val userAnswers = emptyUserAnswers
            .withPage(ReportForRegisteredBusinessPage, true)
            .withPage(OrganisationOrIndividualPage, Organisation)
            .withPage(OverwritableOrganisationName, testOrgName)
            .withPage(TradingNamePage, testTradingName)
            .withPage(UtrPage, testUtr.uniqueTaxPayerReference)
            .withPage(UkAddressInUserAnswers, testAddressUk)
            .withPage(ChangeRcaspCachedDetails, cachedRcaspDetails)

          val result = testHelper.haveAnswersChangedFromApi(userAnswers)
          result mustBe Some(false)
        }
      }

      "when the answers have changed" - {
        "when the API returns Individual but userAnswers have Organisation" in {
          val cachedRcaspDetails = individualRcaspDetailsViewUpdate

          val userAnswers = emptyUserAnswers
            .withPage(ReportForRegisteredBusinessPage, false)
            .withPage(OrganisationOrIndividualPage, Organisation)
            .withPage(OverwritableOrganisationName, testOrgName)
            .withPage(UtrPage, testUtr.uniqueTaxPayerReference)
            .withPage(UkAddressInUserAnswers, testAddressUk)
            .withPage(OrganisationFirstContactNamePage, testIndividualName.fullName)
            .withPage(OrganisationFirstContactEmailPage, testEmail)
            .withPage(OrganisationFirstContactPhoneNumberPage, testPhone)
            .withPage(ChangeRcaspCachedDetails, cachedRcaspDetails)

          val result = testHelper.haveAnswersChangedFromApi(userAnswers)
          result mustBe Some(true)
        }

        "when the API returns Organisation but userAnswers have Individual" - {
          "when the API returns the registered business" in {
            val cachedRcaspDetails = organisationRcaspDetailsViewUpdate.copy(
              IsRCASPUser = true,
              PrimaryContactDetails = None,
              SecondaryContactDetails = None
            )

            val userAnswers = emptyUserAnswers
              .withPage(ReportForRegisteredBusinessPage, false)
              .withPage(OrganisationOrIndividualPage, Individual)
              .withPage(IndividualNamePage, testIndividualName)
              .withPage(NiNumberPage, testNiNumber)
              .withPage(UkAddressInUserAnswers, testAddressUk)
              .withPage(IndividualEmailPage, testEmail)
              .withPage(ChangeRcaspCachedDetails, cachedRcaspDetails)

            val result = testHelper.haveAnswersChangedFromApi(userAnswers)
            result mustBe Some(true)
          }

          "when the API returns a standard organisation" in {
            val userAnswers = emptyUserAnswers
              .withPage(ReportForRegisteredBusinessPage, false)
              .withPage(OrganisationOrIndividualPage, Individual)
              .withPage(IndividualNamePage, testIndividualName)
              .withPage(NiNumberPage, testNiNumber)
              .withPage(UkAddressInUserAnswers, testAddressUk)
              .withPage(IndividualEmailPage, testEmail)
              .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate)

            val result = testHelper.haveAnswersChangedFromApi(userAnswers)
            result mustBe Some(true)
          }
        }

        "when one field has changed for an Individual" in {
          val userAnswers = emptyUserAnswers
            .withPage(ReportForRegisteredBusinessPage, false)
            .withPage(OrganisationOrIndividualPage, Individual)
            .withPage(IndividualNamePage, testIndividualName)
            .withPage(NiNumberPage, testNiNumber)
            .withPage(UkAddressInUserAnswers, testAddressUk)
            .withPage(IndividualEmailPage, "test@example.com")
            .withPage(IndividualPhonePage, testPhone)
            .withPage(ChangeRcaspCachedDetails, individualRcaspDetailsViewUpdate)

          val result = testHelper.haveAnswersChangedFromApi(userAnswers)
          result mustBe Some(true)
        }

        "when multiple fields have changed for an Individual" in {
          val userAnswers = emptyUserAnswers
            .withPage(ReportForRegisteredBusinessPage, false)
            .withPage(OrganisationOrIndividualPage, Individual)
            .withPage(IndividualNamePage, testIndividualName)
            .withPage(NiNumberPage, "AA123456C")
            .withPage(UkAddressInUserAnswers, testAddressUkAlt)
            .withPage(IndividualEmailPage, "test@example.com")
            .withPage(ChangeRcaspCachedDetails, individualRcaspDetailsViewUpdate)

          val result = testHelper.haveAnswersChangedFromApi(userAnswers)
          result mustBe Some(true)
        }

        "when one field has changed for an Organisation" in {
          val userAnswers = emptyUserAnswers
            .withPage(ReportForRegisteredBusinessPage, false)
            .withPage(OrganisationOrIndividualPage, Organisation)
            .withPage(OverwritableOrganisationName, testOrgName)
            .withPage(TradingNamePage, "New Trading Name")
            .withPage(UtrPage, testUtr.uniqueTaxPayerReference)
            .withPage(UkAddressInUserAnswers, testAddressUk)
            .withPage(OrganisationFirstContactNamePage, testIndividualName.fullName)
            .withPage(OrganisationFirstContactEmailPage, testEmail)
            .withPage(OrganisationFirstContactPhoneNumberPage, testPhone)
            .withPage(OrganisationSecondContactNamePage, "Prof Turo")
            .withPage(OrganisationSecondContactEmailPage, testEmail)
            .withPage(OrganisationSecondContactPhoneNumberPage, testPhone)
            .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate)

          val result = testHelper.haveAnswersChangedFromApi(userAnswers)
          result mustBe Some(true)
        }

        "when multiple fields have changed for an Organisation" in {
          val userAnswers = emptyUserAnswers
            .withPage(ReportForRegisteredBusinessPage, false)
            .withPage(OrganisationOrIndividualPage, Organisation)
            .withPage(OverwritableOrganisationName, "New Org Name")
            .withPage(TradingNamePage, "New Trading Name")
            .withPage(UtrPage, testUtr.uniqueTaxPayerReference)
            .withPage(UkAddressInUserAnswers, testAddressUkAlt)
            .withPage(OrganisationFirstContactNamePage, testIndividualName.fullName)
            .withPage(OrganisationFirstContactEmailPage, "test@example.com")
            .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate)

          val result = testHelper.haveAnswersChangedFromApi(userAnswers)
          result mustBe Some(true)
        }

        "when an Organisation has changed from registered business to standard organisation" in {
          val cachedRcaspDetails = organisationRcaspDetailsViewUpdate.copy(
            IsRCASPUser = true,
            PrimaryContactDetails = None,
            SecondaryContactDetails = None
          )

          val userAnswers = emptyUserAnswers
            .withPage(ReportForRegisteredBusinessPage, false)
            .withPage(OrganisationOrIndividualPage, Organisation)
            .withPage(OverwritableOrganisationName, testOrgName)
            .withPage(UtrPage, testUtr.uniqueTaxPayerReference)
            .withPage(UkAddressInUserAnswers, testAddressUk)
            .withPage(OrganisationFirstContactNamePage, testIndividualName.fullName)
            .withPage(OrganisationFirstContactEmailPage, testEmail)
            .withPage(ChangeRcaspCachedDetails, cachedRcaspDetails)

          val result = testHelper.haveAnswersChangedFromApi(userAnswers)
          result mustBe Some(true)
        }
      }
    }

    "getIndividualSectionMaybe" - {
      "for the add journey" - {
        "must return a section when relevant pages are present" - {
          "when ReportForRegisteredBusiness is answered" in {
            val userAnswers = emptyUserAnswers
              .withPage(ReportForRegisteredBusinessPage, false)
              .withPage(OrganisationOrIndividualPage, Individual)
              .withPage(IndividualNamePage, testIndividualName)
              .withPage(NiNumberPage, testNiNumber)
              .withPage(UkAddressInUserAnswers, testAddressUk)

            val section: Section          = testHelper.getIndividualSectionMaybe(userAnswers, changeJourney = false).get
            val expectedTitle             = ""
            val expectedKeys: Seq[String] = Seq(
              "Is the business you registered as a reporting cryptoasset service provider (RCASP)?",
              "Would you like to add an organisation or individual as an RCASP?",
              "What is the name of the RCASP?",
              "National Insurance number",
              "Main business address"
            )

            compareRowsAndTitleToExpected(expectedTitle, expectedKeys, section)
          }

          "when ReportForRegisteredBusiness is not answered" in {
            val userAnswers = emptyUserAnswers
              .withPage(OrganisationOrIndividualPage, Individual)
              .withPage(IndividualNamePage, testIndividualName)
              .withPage(NiNumberPage, testNiNumber)
              .withPage(UkAddressInUserAnswers, testAddressUk)

            val section: Section          = testHelper.getIndividualSectionMaybe(userAnswers, changeJourney = false).get
            val expectedTitle             = ""
            val expectedKeys: Seq[String] = Seq(
              "Would you like to add an organisation or individual as a reporting cryptoasset service provider (RCASP)?",
              "What is the name of the RCASP?",
              "National Insurance number",
              "Main business address"
            )

            compareRowsAndTitleToExpected(expectedTitle, expectedKeys, section)
          }
        }

        "must return None when pages are missing" in {
          val userAnswers = emptyUserAnswers
            .withPage(IndividualNamePage, testIndividualName)

          val section = testHelper.getIndividualSectionMaybe(userAnswers, changeJourney = false)

          section mustBe None
        }

        "must return None when ReportForRegisteredBusiness is true" in {
          val userAnswers = emptyUserAnswers
            .withPage(ReportForRegisteredBusinessPage, true)
            .withPage(OrganisationOrIndividualPage, Individual)
            .withPage(IndividualNamePage, testIndividualName)
            .withPage(NiNumberPage, testNiNumber)
            .withPage(UkAddressInUserAnswers, testAddressUk)

          val section = testHelper.getIndividualSectionMaybe(userAnswers, changeJourney = false)

          section mustBe None
        }
      }

      "for the change journey" - {
        "must return a section when relevant pages are present" - {
          "when isRcaspUser = true from API" in {
            val userAnswers = emptyUserAnswers
              .withPage(ReportForRegisteredBusinessPage, false)
              .withPage(OrganisationOrIndividualPage, Individual)
              .withPage(IndividualNamePage, testIndividualName)
              .withPage(NiNumberPage, testNiNumber)
              .withPage(UkAddressInUserAnswers, testAddressUk)
              .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate.copy(IsRCASPUser = true))

            val section: Section          = testHelper.getIndividualSectionMaybe(userAnswers, changeJourney = true).get
            val expectedTitle             = ""
            val expectedKeys: Seq[String] = Seq(
              "RCASP ID",
              "Is this reporting cryptoasset service provider (RCASP) the business you registered as?",
              "Is this RCASP an organisation or individual?",
              "What is the name of the RCASP?",
              "National Insurance number",
              "Main business address"
            )

            compareRowsAndTitleToExpected(expectedTitle, expectedKeys, section)
          }

          "when isRcaspUser = false from API" in {
            val userAnswers = emptyUserAnswers
              .withPage(ReportForRegisteredBusinessPage, false)
              .withPage(OrganisationOrIndividualPage, Individual)
              .withPage(IndividualNamePage, testIndividualName)
              .withPage(NiNumberPage, testNiNumber)
              .withPage(UkAddressInUserAnswers, testAddressUk)
              .withPage(ChangeRcaspCachedDetails, individualRcaspDetailsViewUpdate)

            val section: Section          = testHelper.getIndividualSectionMaybe(userAnswers, changeJourney = true).get
            val expectedTitle             = ""
            val expectedKeys: Seq[String] = Seq(
              "RCASP ID",
              "Is this reporting cryptoasset service provider (RCASP) an organisation or individual?",
              "What is the name of the RCASP?",
              "National Insurance number",
              "Main business address"
            )

            compareRowsAndTitleToExpected(expectedTitle, expectedKeys, section)
          }
        }

        "must return None when pages are missing" in {
          val userAnswers = emptyUserAnswers
            .withPage(ReportForRegisteredBusinessPage, false)
            .withPage(OrganisationOrIndividualPage, Individual)
            .withPage(IndividualNamePage, testIndividualName)

          val section = testHelper.getIndividualSectionMaybe(userAnswers, changeJourney = true)

          section mustBe None
        }

        "must return None when ReportForRegisteredBusiness is true" in {
          val userAnswers = emptyUserAnswers
            .withPage(ReportForRegisteredBusinessPage, true)
            .withPage(OrganisationOrIndividualPage, Individual)
            .withPage(IndividualNamePage, testIndividualName)
            .withPage(NiNumberPage, testNiNumber)
            .withPage(UkAddressInUserAnswers, testAddressUk)
            .withPage(ChangeRcaspCachedDetails, individualRcaspDetailsViewUpdate)

          val section = testHelper.getIndividualSectionMaybe(userAnswers, changeJourney = true)

          section mustBe None
        }

        "must return None when ReportForRegisteredBusiness is missing" in {
          val userAnswers = emptyUserAnswers
            .withPage(OrganisationOrIndividualPage, Individual)
            .withPage(IndividualNamePage, testIndividualName)
            .withPage(NiNumberPage, testNiNumber)
            .withPage(UkAddressInUserAnswers, testAddressUk)
            .withPage(ChangeRcaspCachedDetails, individualRcaspDetailsViewUpdate)

          val section = testHelper.getIndividualSectionMaybe(userAnswers, changeJourney = true)

          section mustBe None
        }
      }
    }

    "getIndividualContactDetailsMaybe" - {
      "must return a section when relevant pages are present" in {
        val userAnswers = emptyUserAnswers
          .withPage(IndividualEmailPage, testEmail)
          .withPage(IndividualHavePhonePage, true)
          .withPage(IndividualPhonePage, testPhone)

        val section: Section          = testHelper.getIndividualContactDetailsMaybe(userAnswers).get
        val expectedTitle             = "Contact details"
        val expectedKeys: Seq[String] = Seq("Email address", "Can we contact the RCASP by phone?", "Phone number")

        compareRowsAndTitleToExpected(expectedTitle, expectedKeys, section)
      }

      "must return NONE when havePhone is true but phone is missing" in {
        val userAnswers = emptyUserAnswers
          .withPage(IndividualEmailPage, testEmail)
          .withPage(IndividualHavePhonePage, true)

        val section = testHelper.getIndividualContactDetailsMaybe(userAnswers)

        section mustBe None
      }

      "must NOT return phone row when have phone is false" in {
        val userAnswers = emptyUserAnswers
          .withPage(IndividualEmailPage, testEmail)
          .withPage(IndividualHavePhonePage, false)
          .withPage(IndividualPhonePage, testPhone)

        val section: Section          = testHelper.getIndividualContactDetailsMaybe(userAnswers).get
        val expectedTitle             = "Contact details"
        val expectedKeys: Seq[String] = Seq("Email address", "Can we contact the RCASP by phone?")

        compareRowsAndTitleToExpected(expectedTitle, expectedKeys, section)
      }

      "must NOT return a section when a page is missing" in {
        val userAnswers = emptyUserAnswers
          .withPage(IndividualEmailPage, testEmail)

        val section = testHelper.getIndividualContactDetailsMaybe(userAnswers)

        section mustBe None
      }
    }

    "getOrganisationSectionMaybe" - {
      "for the add journey" - {
        "must return a section with the correct name row url when relevant pages are present" - {
          "when ReportForRegisteredBusiness is answered and haveTradingName is true" in {
            val userAnswers = emptyUserAnswers
              .withPage(ReportForRegisteredBusinessPage, false)
              .withPage(OrganisationOrIndividualPage, Organisation)
              .withPage(OverwritableOrganisationName, testOrgName)
              .withPage(HaveTradingNamePage, true)
              .withPage(TradingNamePage, testTradingName)
              .withPage(UtrPage, testUtr.uniqueTaxPayerReference)
              .withPage(UkAddressInUserAnswers, testAddressUk)

            val section: Section = testHelper.getOrganisationSectionMaybe(userAnswers, changeJourney = false).get

            val expectedReportForRegisteredBusinessUrl: String =
              controllers.organisation.routes.ReportForRegisteredBusinessController.onPageLoad(ChangeMode).url
            val expectedOrganisationNameUrl: String            =
              controllers.organisation.routes.OrganisationNameController.onPageLoad(ChangeMode).url

            val expectedTitle             = ""
            val expectedKeys: Seq[String] = Seq(
              "Is the business you registered as a reporting cryptoasset service provider (RCASP)?",
              "Would you like to add an organisation or individual as an RCASP?",
              "What is the name of the organisation?",
              "Does the organisation trade under a different name?",
              "What is the trading name for the organisation?",
              "Unique Taxpayer Reference",
              "Main business address"
            )

            compareRowsAndTitleToExpected(expectedTitle, expectedKeys, section)
            section.rows.head.actions.get.items.head.href mustBe expectedReportForRegisteredBusinessUrl
            section.rows(2).actions.get.items.head.href   mustBe expectedOrganisationNameUrl
          }

          "when ReportForRegisteredBusiness is not answered and and haveTradingName is false" in {
            val userAnswers = emptyUserAnswers
              .withPage(OrganisationOrIndividualPage, Organisation)
              .withPage(OverwritableOrganisationName, testOrgName)
              .withPage(HaveTradingNamePage, false)
              .withPage(UtrPage, testUtr.uniqueTaxPayerReference)
              .withPage(UkAddressInUserAnswers, testAddressUk)

            val section: Section          = testHelper.getOrganisationSectionMaybe(userAnswers, changeJourney = false).get
            val expectedTitle             = ""
            val expectedKeys: Seq[String] = Seq(
              "Would you like to add an organisation or individual as a reporting cryptoasset service provider (RCASP)?",
              "What is the name of the organisation?",
              "Does the organisation trade under a different name?",
              "Unique Taxpayer Reference",
              "Main business address"
            )

            compareRowsAndTitleToExpected(expectedTitle, expectedKeys, section)
          }
        }

        "must return None when haveTradingName is true but trading name is missing" in {
          val userAnswers = emptyUserAnswers
            .withPage(OrganisationOrIndividualPage, Organisation)
            .withPage(OverwritableOrganisationName, testOrgName)
            .withPage(HaveTradingNamePage, true)
            .withPage(UtrPage, testUtr.uniqueTaxPayerReference)
            .withPage(UkAddressInUserAnswers, testAddressUk)

          val section = testHelper.getOrganisationSectionMaybe(userAnswers, changeJourney = false)

          section mustBe None
        }

        "must return None when pages are missing" in {
          val userAnswers = emptyUserAnswers
            .withPage(OverwritableOrganisationName, testOrgName)

          val section = testHelper.getOrganisationSectionMaybe(userAnswers, changeJourney = false)

          section mustBe None
        }

        "must return None when ReportForRegisteredBusiness is true" in {
          val userAnswers = emptyUserAnswers
            .withPage(ReportForRegisteredBusinessPage, true)
            .withPage(OrganisationOrIndividualPage, Organisation)
            .withPage(OverwritableOrganisationName, testOrgName)
            .withPage(HaveTradingNamePage, true)
            .withPage(TradingNamePage, testTradingName)
            .withPage(UtrPage, testUtr.uniqueTaxPayerReference)
            .withPage(UkAddressInUserAnswers, testAddressUk)

          val section = testHelper.getOrganisationSectionMaybe(userAnswers, changeJourney = false)

          section mustBe None
        }
      }

      "for the change journey" - {
        "must return a section with the correct name row url when relevant pages are present" - {
          "when isRcaspUser = true from API and haveTradingName is true" in {
            val userAnswers = emptyUserAnswers
              .withPage(ReportForRegisteredBusinessPage, false)
              .withPage(OrganisationOrIndividualPage, Organisation)
              .withPage(OverwritableOrganisationName, testOrgName)
              .withPage(HaveTradingNamePage, true)
              .withPage(TradingNamePage, testTradingName)
              .withPage(UtrPage, testUtr.uniqueTaxPayerReference)
              .withPage(UkAddressInUserAnswers, testAddressUk)
              .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate.copy(IsRCASPUser = true))

            val section: Section = testHelper.getOrganisationSectionMaybe(userAnswers, changeJourney = true).get

            val expectedReportForRegisteredBusinessUrl: String =
              controllers.organisation.routes.ReportForRegisteredBusinessController.onPageLoad(ChangeMode).url
            val expectedOrganisationNameUrl: String            =
              controllers.organisation.routes.OrganisationNameController.onPageLoad(ChangeMode).url

            val expectedTitle             = ""
            val expectedKeys: Seq[String] = Seq(
              "RCASP ID",
              "Is this reporting cryptoasset service provider (RCASP) the business you registered as?",
              "Is this RCASP an organisation or individual?",
              "What is the name of the organisation?",
              "Does the organisation trade under a different name?",
              "What is the trading name for the organisation?",
              "Unique Taxpayer Reference",
              "Main business address"
            )

            compareRowsAndTitleToExpected(expectedTitle, expectedKeys, section)
            section.rows(1).actions.get.items.head.href mustBe expectedReportForRegisteredBusinessUrl
            section.rows(3).actions.get.items.head.href mustBe expectedOrganisationNameUrl
          }

          "when isRcaspUser = false from API and haveTradingName is false" in {
            val userAnswers = emptyUserAnswers
              .withPage(ReportForRegisteredBusinessPage, false)
              .withPage(OrganisationOrIndividualPage, Organisation)
              .withPage(OverwritableOrganisationName, testOrgName)
              .withPage(HaveTradingNamePage, false)
              .withPage(UtrPage, testUtr.uniqueTaxPayerReference)
              .withPage(UkAddressInUserAnswers, testAddressUk)
              .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate)

            val section: Section          = testHelper.getOrganisationSectionMaybe(userAnswers, changeJourney = true).get
            val expectedTitle             = ""
            val expectedKeys: Seq[String] = Seq(
              "RCASP ID",
              "Is this reporting cryptoasset service provider (RCASP) an organisation or individual?",
              "What is the name of the organisation?",
              "Does the organisation trade under a different name?",
              "Unique Taxpayer Reference",
              "Main business address"
            )

            compareRowsAndTitleToExpected(expectedTitle, expectedKeys, section)
          }
        }

        "must return None when haveTradingName is true but trading name is missing" in {
          val userAnswers = emptyUserAnswers
            .withPage(ReportForRegisteredBusinessPage, false)
            .withPage(OrganisationOrIndividualPage, Organisation)
            .withPage(OverwritableOrganisationName, testOrgName)
            .withPage(HaveTradingNamePage, true)
            .withPage(UtrPage, testUtr.uniqueTaxPayerReference)
            .withPage(UkAddressInUserAnswers, testAddressUk)
            .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate)

          val section = testHelper.getOrganisationSectionMaybe(userAnswers, changeJourney = true)

          section mustBe None
        }

        "must return None when pages are missing" in {
          val userAnswers = emptyUserAnswers
            .withPage(ReportForRegisteredBusinessPage, false)
            .withPage(OrganisationOrIndividualPage, Organisation)
            .withPage(OverwritableOrganisationName, testOrgName)

          val section = testHelper.getOrganisationSectionMaybe(userAnswers, changeJourney = true)

          section mustBe None
        }

        "must return None when ReportForRegisteredBusiness is true" in {
          val userAnswers = emptyUserAnswers
            .withPage(ReportForRegisteredBusinessPage, true)
            .withPage(OrganisationOrIndividualPage, Organisation)
            .withPage(OverwritableOrganisationName, testOrgName)
            .withPage(HaveTradingNamePage, false)
            .withPage(UtrPage, testUtr.uniqueTaxPayerReference)
            .withPage(UkAddressInUserAnswers, testAddressUk)
            .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate)

          val section = testHelper.getOrganisationSectionMaybe(userAnswers, changeJourney = true)

          section mustBe None
        }

        "must return None when ReportForRegisteredBusiness is missing" in {
          val userAnswers = emptyUserAnswers
            .withPage(OrganisationOrIndividualPage, Organisation)
            .withPage(OverwritableOrganisationName, testOrgName)
            .withPage(HaveTradingNamePage, false)
            .withPage(UtrPage, testUtr.uniqueTaxPayerReference)
            .withPage(UkAddressInUserAnswers, testAddressUk)
            .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate)

          val section = testHelper.getOrganisationSectionMaybe(userAnswers, changeJourney = true)

          section mustBe None
        }
      }
    }

    "getOrganisationFirstContactDetailsMaybe" - {
      "must return a section when relevant pages are present" - {
        "when firstContactHavePhone is true" in {
          val userAnswers = emptyUserAnswers
            .withPage(OrganisationFirstContactNamePage, testOrgContactName)
            .withPage(OrganisationFirstContactEmailPage, testEmail)
            .withPage(OrganisationFirstContactHavePhonePage, true)
            .withPage(OrganisationFirstContactPhoneNumberPage, testPhone)

          val section: Section          = testHelper.getOrganisationFirstContactDetailsMaybe(userAnswers).get
          val expectedTitle             = "First contact"
          val expectedKeys: Seq[String] = Seq(
            "First contact name",
            "First contact email address",
            "Can we contact the first contact by phone?",
            "First contact phone number"
          )

          compareRowsAndTitleToExpected(expectedTitle, expectedKeys, section)
        }

        "when firstContactHavePhone is false" in {
          val userAnswers = emptyUserAnswers
            .withPage(OrganisationFirstContactNamePage, testOrgContactName)
            .withPage(OrganisationFirstContactEmailPage, testEmail)
            .withPage(OrganisationFirstContactHavePhonePage, false)

          val section: Section          = testHelper.getOrganisationFirstContactDetailsMaybe(userAnswers).get
          val expectedTitle             = "First contact"
          val expectedKeys: Seq[String] = Seq(
            "First contact name",
            "First contact email address",
            "Can we contact the first contact by phone?"
          )

          compareRowsAndTitleToExpected(expectedTitle, expectedKeys, section)
        }
      }

      "must return None when firstContactHavePhone is true but phone number is missing" in {
        val userAnswers = emptyUserAnswers
          .withPage(OrganisationFirstContactNamePage, testOrgContactName)
          .withPage(OrganisationFirstContactEmailPage, testEmail)
          .withPage(OrganisationFirstContactHavePhonePage, true)

        val section = testHelper.getOrganisationFirstContactDetailsMaybe(userAnswers)

        section mustBe None
      }

      "must return None when pages are missing" in {
        val userAnswers = emptyUserAnswers
          .withPage(OrganisationFirstContactNamePage, testOrgContactName)

        val section = testHelper.getOrganisationFirstContactDetailsMaybe(userAnswers)

        section mustBe None
      }
    }

    "getOrganisationSecondContactDetailsMaybe" - {
      "must return a section when relevant pages are present" - {
        "when haveSecondContact is false" in {
          val userAnswers = emptyUserAnswers
            .withPage(OrganisationHaveSecondContactPage, false)

          val section: Section          = testHelper.getOrganisationSecondContactDetailsMaybe(userAnswers).get
          val expectedTitle             = "Second contact"
          val expectedKeys: Seq[String] = Seq("Does the organisation have a second contact?")

          compareRowsAndTitleToExpected(expectedTitle, expectedKeys, section)
        }

        "when haveSecondContact is true and secondContactHavePhone is false" in {
          val userAnswers = emptyUserAnswers
            .withPage(OrganisationHaveSecondContactPage, true)
            .withPage(OrganisationSecondContactNamePage, testOrgContactName)
            .withPage(OrganisationSecondContactEmailPage, testEmail)
            .withPage(OrganisationSecondContactHavePhonePage, false)

          val section: Section          = testHelper.getOrganisationSecondContactDetailsMaybe(userAnswers).get
          val expectedTitle             = "Second contact"
          val expectedKeys: Seq[String] = Seq(
            "Does the organisation have a second contact?",
            "Second contact name",
            "Second contact email address",
            "Can we contact the second contact by phone?"
          )

          compareRowsAndTitleToExpected(expectedTitle, expectedKeys, section)
        }

        "when haveSecondContact is true and secondContactHavePhone is true" in {
          val userAnswers = emptyUserAnswers
            .withPage(OrganisationHaveSecondContactPage, true)
            .withPage(OrganisationSecondContactNamePage, testOrgContactName)
            .withPage(OrganisationSecondContactEmailPage, testEmail)
            .withPage(OrganisationSecondContactHavePhonePage, true)
            .withPage(OrganisationSecondContactPhoneNumberPage, testPhone)

          val section: Section          = testHelper.getOrganisationSecondContactDetailsMaybe(userAnswers).get
          val expectedTitle             = "Second contact"
          val expectedKeys: Seq[String] = Seq(
            "Does the organisation have a second contact?",
            "Second contact name",
            "Second contact email address",
            "Can we contact the second contact by phone?",
            "Second contact phone number"
          )

          compareRowsAndTitleToExpected(expectedTitle, expectedKeys, section)
        }
      }

      "must return None when haveSecondContact is true but subsequent answers are missing" in {
        val userAnswers = emptyUserAnswers
          .withPage(OrganisationHaveSecondContactPage, true)

        val section = testHelper.getOrganisationSecondContactDetailsMaybe(userAnswers)

        section mustBe None
      }

      "must return None when secondContactHavePhone is true but phone number is missing" in {
        val userAnswers = emptyUserAnswers
          .withPage(OrganisationHaveSecondContactPage, true)
          .withPage(OrganisationSecondContactNamePage, testOrgContactName)
          .withPage(OrganisationSecondContactEmailPage, testEmail)
          .withPage(OrganisationSecondContactHavePhonePage, true)

        val section = testHelper.getOrganisationSecondContactDetailsMaybe(userAnswers)

        section mustBe None
      }

      "must return None when haveSecondContact is missing" in {
        val userAnswers = emptyUserAnswers
          .withPage(OrganisationSecondContactNamePage, testOrgContactName)
          .withPage(OrganisationSecondContactEmailPage, testEmail)
          .withPage(OrganisationSecondContactHavePhonePage, false)

        val section = testHelper.getOrganisationSecondContactDetailsMaybe(userAnswers)

        section mustBe None
      }
    }
  }
}
