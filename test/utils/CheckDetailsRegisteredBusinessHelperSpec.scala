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
import models.{CachedBusinessDetails, UserAnswers}
import pages.*
import pages.organisation.*
import play.api.i18n.Messages

class CheckDetailsRegisteredBusinessHelperSpec extends SpecBase {

  val helper: CheckDetailsRegisteredBusinessHelper = new CheckDetailsRegisteredBusinessHelper()

  implicit lazy val msgs: Messages = messages(app)

  val completeUserAnswers: UserAnswers = emptyUserAnswers
    .withPage(ReportForRegisteredBusinessPage, true)
    .withPage(RegisteredBusinessIsTheAddressCorrectPage, true)
    .withPage(RegisteredBusinessIsThisYourBusinessNamePage, true)
    .withPage(OverwritableOrganisationName, "Test Business Ltd")
    .withPage(HaveTradingNamePage, false)
    .withPage(CachedBusinessDetailsPage, cachedBusinessDetails)

  "CheckDetailsRegBusinessHelper" - {
    "getRegisteredBusinessSection" - {
      "must return a section with all rows when have trading name is true" in {
        val userAnswers = emptyUserAnswers
          .withPage(ReportForRegisteredBusinessPage, true)
          .withPage(RegisteredBusinessIsTheAddressCorrectPage, true)
          .withPage(RegisteredBusinessIsThisYourBusinessNamePage, true)
          .withPage(OverwritableOrganisationName, testOrgName)
          .withPage(HaveTradingNamePage, true)
          .withPage(TradingNamePage, "Trading Co")
          .withPage(CachedBusinessDetailsPage, cachedBusinessDetails)

        val section = helper.getRegisteredBusinessSection(userAnswers).get

        val expectedTitle             = ""
        val expectedKeys: Seq[String] = Seq(
          "Is the business you registered as a reporting cryptoasset service provider (RCASP)?",
          "What is the name of the organisation?",
          "Does the organisation trade under a different name?",
          "Trading name",
          "Main business address"
        )

        compareRowsAndTitleToExpected(expectedTitle, expectedKeys, section)
      }

      "must return an alternative section with no trading name, non cached address or business name" - {
        "when user indicates that the do not trade under a different name, and the address from the api is not correct" in {
          val userAnswers = emptyUserAnswers
            .withPage(ReportForRegisteredBusinessPage, true)
            .withPage(RegisteredBusinessIsTheAddressCorrectPage, false)
            .withPage(RegisteredBusinessIsThisYourBusinessNamePage, false)
            .withPage(OverwritableOrganisationName, testOrgName)
            .withPage(UkAddressInUserAnswers, testAddressUk)
            .withPage(HaveTradingNamePage, false)

          val section = helper.getRegisteredBusinessSection(userAnswers).get

          val expectedTitle             = ""
          val expectedKeys: Seq[String] = Seq(
            "Is the business you registered as a reporting cryptoasset service provider (RCASP)?",
            "What is the name of the organisation?",
            "Does the organisation trade under a different name?",
            "Main business address"
          )

          compareRowsAndTitleToExpected(expectedTitle, expectedKeys, section)
        }
      }

      "must return None when no answers are present" in {
        helper.getRegisteredBusinessSection(emptyUserAnswers) mustBe None
      }

      "must return None when have trading name is true but trading name is missing" in {
        val userAnswers = completeUserAnswers.withPage(HaveTradingNamePage, true)

        helper.getRegisteredBusinessSection(userAnswers) mustBe None
      }

      "must return None when all answers are present but ReportForRegisteredBusinessPage is false" in {
        val userAnswers = completeUserAnswers.withPage(ReportForRegisteredBusinessPage, false)

        helper.getRegisteredBusinessSection(userAnswers) mustBe None
      }

      "must return None when cached business details are missing but the user indicated that the address there was correct" in {
        val userAnswers = completeUserAnswers
          .withPage(RegisteredBusinessIsTheAddressCorrectPage, true)
          .withoutPage(CachedBusinessDetailsPage)

        helper.getRegisteredBusinessSection(userAnswers) mustBe None
      }

      "must return None when UkAddressInUserAnswers is missing but the user indicated that the address from the api was incorrect" in {
        val userAnswers = completeUserAnswers.withPage(RegisteredBusinessIsTheAddressCorrectPage, false)

        helper.getRegisteredBusinessSection(userAnswers) mustBe None
      }
    }
  }
}
