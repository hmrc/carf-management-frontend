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
import models.{ChangeMode, UserAnswers}
import pages.*
import pages.changeDetails.ChangeRcaspCachedDetails
import pages.organisation.*
import play.api.i18n.Messages

class RegisteredBusinessDetailsHelperSpec extends SpecBase {

  val helper: RegisteredBusinessDetailsHelper = new RegisteredBusinessDetailsHelper()

  implicit lazy val msgs: Messages = messages(app)

  val completeUserAnswers: UserAnswers = emptyUserAnswers
    .withPage(ReportForRegisteredBusinessPage, true)
    .withPage(RegisteredBusinessIsThisYourBusinessNamePage, true)
    .withPage(OverwritableOrganisationName, "Test Business Ltd")
    .withPage(HaveTradingNamePage, false)
    .withPage(UkAddressInUserAnswers, testAddressUk)

  val expectedEmptySectionTitle = ""

  "RegisteredBusinessDetailsHelper" - {
    "getRegisteredBusinessSection" - {
      "for the add journey" - {
        "must return a section with all rows when have trading name is true" in {
          val userAnswers = emptyUserAnswers
            .withPage(ReportForRegisteredBusinessPage, true)
            .withPage(RegisteredBusinessIsThisYourBusinessNamePage, true)
            .withPage(OverwritableOrganisationName, testOrgName)
            .withPage(HaveTradingNamePage, true)
            .withPage(TradingNamePage, "Trading Co")
            .withPage(UkAddressInUserAnswers, testAddressUk)

          val section = helper.getRegisteredBusinessSection(userAnswers, changeJourney = false).get

          val expectedKeys: Seq[String] = Seq(
            "Is the business you registered as a reporting cryptoasset service provider (RCASP)?",
            "What is the name of the organisation?",
            "Does the organisation trade under a different name?",
            "What is the trading name for the organisation?",
            "Main business address"
          )

          compareRowsAndTitleToExpected(expectedEmptySectionTitle, expectedKeys, section)
        }

        "must return an alternative section with no trading name" - {
          "when user indicates that the business name from the api is not correct" in {
            val userAnswers = emptyUserAnswers
              .withPage(ReportForRegisteredBusinessPage, true)
              .withPage(RegisteredBusinessIsThisYourBusinessNamePage, false)
              .withPage(OverwritableOrganisationName, testOrgName)
              .withPage(HaveTradingNamePage, false)
              .withPage(UkAddressInUserAnswers, testAddressUk)

            val section = helper.getRegisteredBusinessSection(userAnswers, changeJourney = false).get

            val expectedReportForRegisteredBusinessUrl: String =
              controllers.organisation.routes.ReportForRegisteredBusinessController.onPageLoad(ChangeMode).url
            val expectedOrganisationNameUrl: String            =
              controllers.organisation.routes.RegisteredBusinessIsThisYourBusinessNameController
                .onPageLoad(ChangeMode)
                .url

            val expectedKeys: Seq[String] = Seq(
              "Is the business you registered as a reporting cryptoasset service provider (RCASP)?",
              "What is the name of the organisation?",
              "Does the organisation trade under a different name?",
              "Main business address"
            )

            compareRowsAndTitleToExpected(expectedEmptySectionTitle, expectedKeys, section)
            section.rows.head.actions.get.items.head.href mustBe expectedReportForRegisteredBusinessUrl
            section.rows(1).actions.get.items.head.href   mustBe expectedOrganisationNameUrl
          }
        }

        "must return None when no answers are present" in {
          helper.getRegisteredBusinessSection(emptyUserAnswers, changeJourney = false) mustBe None
        }

        "must return None when have trading name is true but trading name is missing" in {
          val userAnswers = completeUserAnswers.withPage(HaveTradingNamePage, true)

          helper.getRegisteredBusinessSection(userAnswers, changeJourney = false) mustBe None
        }

        "must return None when all answers are present but ReportForRegisteredBusinessPage is false" in {
          val userAnswers = completeUserAnswers.withPage(ReportForRegisteredBusinessPage, false)

          helper.getRegisteredBusinessSection(userAnswers, changeJourney = false) mustBe None
        }
      }

      "for the change journey" - {
        "must return a section with all rows when have trading name is true" in {
          val userAnswers = emptyUserAnswers
            .withPage(ReportForRegisteredBusinessPage, true)
            .withPage(RegisteredBusinessIsThisYourBusinessNamePage, true)
            .withPage(OverwritableOrganisationName, testOrgName)
            .withPage(HaveTradingNamePage, true)
            .withPage(TradingNamePage, "Trading Co")
            .withPage(UkAddressInUserAnswers, testAddressUk)
            .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate.copy(IsRCASPUser = true))

          val section = helper.getRegisteredBusinessSection(userAnswers, changeJourney = true).get

          val expectedKeys: Seq[String] = Seq(
            "RCASP ID",
            "Is this reporting cryptoasset service provider (RCASP) the business you registered as?",
            "What is the name of the organisation?",
            "Does the organisation trade under a different name?",
            "What is the trading name for the organisation?",
            "Main business address"
          )

          compareRowsAndTitleToExpected(expectedEmptySectionTitle, expectedKeys, section)
        }

        "must return an alternative section with no trading name" - {
          "when user indicates that the business name from the api is not correct" in {
            val userAnswers = emptyUserAnswers
              .withPage(ReportForRegisteredBusinessPage, true)
              .withPage(RegisteredBusinessIsThisYourBusinessNamePage, false)
              .withPage(OverwritableOrganisationName, testOrgName)
              .withPage(HaveTradingNamePage, false)
              .withPage(UkAddressInUserAnswers, testAddressUk)
              .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate.copy(IsRCASPUser = true))

            val section = helper.getRegisteredBusinessSection(userAnswers, changeJourney = true).get

            val expectedReportForRegisteredBusinessUrl: String =
              controllers.combined.routes.ChangeReportForRegisteredBusinessController.onPageLoad.url
            val expectedOrganisationNameUrl: String            =
              controllers.organisation.routes.RegisteredBusinessIsThisYourBusinessNameController
                .onPageLoad(ChangeMode)
                .url

            val expectedKeys: Seq[String] = Seq(
              "RCASP ID",
              "Is this reporting cryptoasset service provider (RCASP) the business you registered as?",
              "What is the name of the organisation?",
              "Does the organisation trade under a different name?",
              "Main business address"
            )

            compareRowsAndTitleToExpected(expectedEmptySectionTitle, expectedKeys, section)
            section.rows(1).actions.get.items.head.href mustBe expectedReportForRegisteredBusinessUrl
            section.rows(2).actions.get.items.head.href mustBe expectedOrganisationNameUrl
          }
        }

        "must return None when no answers are present" in {
          helper.getRegisteredBusinessSection(emptyUserAnswers, changeJourney = true) mustBe None
        }

        "must return None when have trading name is true but trading name is missing" in {
          val userAnswers = completeUserAnswers
            .withPage(HaveTradingNamePage, true)
            .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate.copy(IsRCASPUser = true))

          helper.getRegisteredBusinessSection(userAnswers, changeJourney = true) mustBe None
        }

        "must return None when all answers are present but ReportForRegisteredBusinessPage is false" in {
          val userAnswers = completeUserAnswers
            .withPage(ReportForRegisteredBusinessPage, false)
            .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate.copy(IsRCASPUser = true))

          helper.getRegisteredBusinessSection(userAnswers, changeJourney = true) mustBe None
        }
      }
    }
  }
}
