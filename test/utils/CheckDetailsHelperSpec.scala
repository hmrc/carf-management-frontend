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
import generators.Generators
import models.ChangeMode
import models.OrganisationOrIndividual.{Individual, Organisation}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import pages.UkAddressInUserAnswers
import pages.combined.OrganisationOrIndividualPage
import pages.individual.*
import pages.organisation.*
import play.api.i18n.Messages
import viewmodels.Section

class CheckDetailsHelperSpec extends SpecBase with ScalaCheckPropertyChecks with Generators {

  val testHelper                  = new CheckDetailsHelper()
  implicit val messages: Messages = messages(app)

  "CheckDetailsHelperSpec" - {
    "getIndividualSectionMaybe" - {
      "must return a section when relevant pages are present" - {
        "when ReportForRegisteredBusiness is answered" in {
          val userAnswers = emptyUserAnswers
            .withPage(ReportForRegisteredBusinessPage, false)
            .withPage(OrganisationOrIndividualPage, Individual)
            .withPage(IndividualNamePage, testIndividualName)
            .withPage(NiNumberPage, testNiNumber)
            .withPage(UkAddressInUserAnswers, testAddressUk)

          val section: Section          = testHelper.getIndividualSectionMaybe(userAnswers).get
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

          val section: Section          = testHelper.getIndividualSectionMaybe(userAnswers).get
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

        val section = testHelper.getIndividualSectionMaybe(userAnswers)

        section mustBe None
      }

      "must return None when ReportForRegisteredBusiness is true" in {
        val userAnswers = emptyUserAnswers
          .withPage(ReportForRegisteredBusinessPage, true)
          .withPage(OrganisationOrIndividualPage, Individual)
          .withPage(IndividualNamePage, testIndividualName)
          .withPage(NiNumberPage, testNiNumber)
          .withPage(UkAddressInUserAnswers, testAddressUk)

        val section = testHelper.getIndividualSectionMaybe(userAnswers)

        section mustBe None
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

          val section: Section = testHelper.getOrganisationSectionMaybe(userAnswers).get

          val expectedOrganisationNameUrl: String =
            controllers.organisation.routes.OrganisationNameController.onPageLoad(ChangeMode).url
          val expectedTitle                       = ""
          val expectedKeys: Seq[String]           = Seq(
            "Is the business you registered as a reporting cryptoasset service provider (RCASP)?",
            "Would you like to add an organisation or individual as an RCASP?",
            "What is the name of the organisation?",
            "Does the organisation trade under a different name?",
            "What is the trading name for the organisation?",
            "Unique Taxpayer Reference",
            "Main business address"
          )

          compareRowsAndTitleToExpected(expectedTitle, expectedKeys, section)
          section.rows(2).actions.get.items.head.href mustBe expectedOrganisationNameUrl
        }

        "when ReportForRegisteredBusiness is not answered and and haveTradingName is false" in {
          val userAnswers = emptyUserAnswers
            .withPage(OrganisationOrIndividualPage, Organisation)
            .withPage(OverwritableOrganisationName, testOrgName)
            .withPage(HaveTradingNamePage, false)
            .withPage(UtrPage, testUtr.uniqueTaxPayerReference)
            .withPage(UkAddressInUserAnswers, testAddressUk)

          val section: Section          = testHelper.getOrganisationSectionMaybe(userAnswers).get
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

        val section = testHelper.getOrganisationSectionMaybe(userAnswers)

        section mustBe None
      }

      "must return None when pages are missing" in {
        val userAnswers = emptyUserAnswers
          .withPage(OverwritableOrganisationName, testOrgName)

        val section = testHelper.getOrganisationSectionMaybe(userAnswers)

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

        val section = testHelper.getOrganisationSectionMaybe(userAnswers)

        section mustBe None
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
