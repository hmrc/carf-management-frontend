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
import models.BusinessDetails
import models.responses.AddressRegistrationResponse
import pages.organisation.*
import play.api.i18n.Messages

class CheckDetailsRegBusinessHelperSpec extends SpecBase {

  val helper: CheckDetailsRegBusinessHelper = new CheckDetailsRegBusinessHelper()

  implicit lazy val msgs: Messages = messages(app)

  val businessDetails: BusinessDetails = BusinessDetails(
    name = "Test Business Ltd",
    address = AddressRegistrationResponse(
      addressLine1 = "1 Test Street",
      addressLine2 = Some("Testville"),
      addressLine3 = None,
      addressLine4 = None,
      postalCode = Some("TE1 1ST"),
      countryCode = "GB"
    )
  )

  "CheckDetailsRegBusinessHelper" - {

    "getRegisteredBusinessSection" - {

      "must return None when no answers are present" in {
        helper.getRegisteredBusinessSection(emptyUserAnswers) mustBe None
      }

      "must return a section with all rows when have trading name is true" in {
        val userAnswers = emptyUserAnswers
          .withPage(ReportForRegisteredBusinessPage, true)
          .withPage(RegisteredBusinessIsThisYourBusinessNamePage, true)
          .withPage(OverwritableOrganisationName, "Test Business Ltd")
          .withPage(HaveTradingNamePage, true)
          .withPage(TradingNamePage, "Trading Co")
          .withPage(CachedBusinessDetailsPage, businessDetails)

        val result = helper.getRegisteredBusinessSection(userAnswers)

        result          mustBe defined
        result.value.rows must have length 5
      }

      "must return a section without trading name row when have trading name is false" in {
        val userAnswers = emptyUserAnswers
          .withPage(ReportForRegisteredBusinessPage, true)
          .withPage(RegisteredBusinessIsThisYourBusinessNamePage, true)
          .withPage(OverwritableOrganisationName, "Test Business Ltd")
          .withPage(HaveTradingNamePage, false)
          .withPage(CachedBusinessDetailsPage, businessDetails)

        val result = helper.getRegisteredBusinessSection(userAnswers)

        result          mustBe defined
        result.value.rows must have length 4
      }

      "must return a section without trading name row when have trading name is not answered" in {
        val userAnswers = emptyUserAnswers
          .withPage(ReportForRegisteredBusinessPage, true)
          .withPage(RegisteredBusinessIsThisYourBusinessNamePage, true)
          .withPage(OverwritableOrganisationName, "Test Business Ltd")
          .withPage(CachedBusinessDetailsPage, businessDetails)

        val result = helper.getRegisteredBusinessSection(userAnswers)

        result          mustBe defined
        result.value.rows must have length 3
      }

      "must return a section with only rows that have answers" in {
        val userAnswers = emptyUserAnswers
          .withPage(ReportForRegisteredBusinessPage, true)

        val result = helper.getRegisteredBusinessSection(userAnswers)

        result          mustBe defined
        result.value.rows must have length 1
      }

      "must return None when rows are empty" in {
        helper.getRegisteredBusinessSection(emptyUserAnswers) mustBe None
      }
    }
  }
}
