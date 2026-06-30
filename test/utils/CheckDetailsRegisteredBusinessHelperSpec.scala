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
import pages.organisation.*
import play.api.i18n.Messages

class CheckDetailsRegisteredBusinessHelperSpec extends SpecBase {

  val helper: CheckDetailsRegisteredBusinessHelper = new CheckDetailsRegisteredBusinessHelper()

  implicit lazy val msgs: Messages = messages(app)

  // TODO test RegisteredBusinessAddressSummary logic here

  val completeUserAnswers: UserAnswers = emptyUserAnswers
    .withPage(ReportForRegisteredBusinessPage, true)
    .withPage(OverwritableOrganisationName, "Test Business Ltd")
    .withPage(HaveTradingNamePage, false)
    .withPage(CachedBusinessDetailsPage, cachedBusinessDetails)

  "CheckDetailsRegBusinessHelper" - {

    "getRegisteredBusinessSection" - {

      "must return None when no answers are present" in {
        helper.getRegisteredBusinessSection(emptyUserAnswers) mustBe None
      }

      "must return None when any mandatory page is missing" in {
        val userAnswers = emptyUserAnswers
          .withPage(ReportForRegisteredBusinessPage, true)
          .withPage(OverwritableOrganisationName, "Test Business Ltd")
        helper.getRegisteredBusinessSection(userAnswers) mustBe None
      }

      "must return a section with all rows when have trading name is true" in {
        val userAnswers = completeUserAnswers
          .withPage(HaveTradingNamePage, true)
          .withPage(TradingNamePage, "Trading Co")

        val result = helper.getRegisteredBusinessSection(userAnswers)

        result          mustBe defined
        result.value.rows must have length 5
      }

      "must return None when have trading name is true but trading name is missing" in {
        val userAnswers = completeUserAnswers.withPage(HaveTradingNamePage, true)

        helper.getRegisteredBusinessSection(userAnswers) mustBe None
      }

      "must return None when all answers are present but ReportForRegisteredBusinessPage is false" in {
        val userAnswers = completeUserAnswers.withPage(ReportForRegisteredBusinessPage, false)

        helper.getRegisteredBusinessSection(userAnswers) mustBe None
      }

      "must return a section without trading name row when have trading name is false" in {
        val result = helper.getRegisteredBusinessSection(completeUserAnswers)

        result          mustBe defined
        result.value.rows must have length 4
      }

      // TODO: Add back in when address becomes not hard coded
      // TODO: more tests in here for address logic and in the Summary Spec itself

//      "must return None when address is missing" in {
//        val userAnswers = emptyUserAnswers
//          .withPage(ReportForRegisteredBusinessPage, true)
//          .withPage(OverwritableOrganisationName, "Test Business Ltd")
//          .withPage(HaveTradingNamePage, false)
//
//        helper.getRegisteredBusinessSection(userAnswers) mustBe None
//      }
    }
  }
}
