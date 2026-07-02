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
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import pages.UkAddressInUserAnswers
import pages.individual.*
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import viewmodels.Section

class CheckDetailsHelperSpec extends SpecBase with ScalaCheckPropertyChecks with Generators {

  val testHelper                  = new CheckDetailsHelper()
  implicit val messages: Messages = messages(app)

  "CheckDetailsHelperSpec" - {
    "getIndividualSectionMaybe" - {
      "should return a section when relevant pages are present" in {
        val userAnswers = emptyUserAnswers
          .withPage(IndividualNamePage, testIndividualName)
          .withPage(NiNumberPage, testNiNumber)
          .withPage(UkAddressInUserAnswers, testAddressUk)

        val section: Section          = testHelper.getIndividualSectionMaybe(userAnswers).get
        val expectedTitle             = ""
        val expectedKeys: Seq[String] = Seq(
          "What is the name of the RCASP?",
          "National Insurance number",
          "Main business address"
        )

        compareRowsAndTitleToExpected(expectedTitle, expectedKeys, section)
      }

      "should NOT return a section when a page is missing" in {
        val userAnswers = emptyUserAnswers
          .withPage(IndividualNamePage, testIndividualName)

        val section = testHelper.getIndividualSectionMaybe(userAnswers)

        section mustBe None
      }
    }

    "getContactDetails" - {
      "should return a section when relevant pages are present" in {
        val userAnswers = emptyUserAnswers
          .withPage(IndividualEmailPage, testEmail)
          .withPage(IndividualHavePhonePage, true)
          .withPage(IndividualPhonePage, testPhone)

        val section: Section          = testHelper.getContactDetails(userAnswers).get
        val expectedTitle             = "Contact details"
        val expectedKeys: Seq[String] = Seq("Email address", "Can we contact the RCASP by phone?", "Phone number")

        compareRowsAndTitleToExpected(expectedTitle, expectedKeys, section)
      }

      "should return NONE when havePhone is true but phone is missing" in {
        val userAnswers = emptyUserAnswers
          .withPage(IndividualEmailPage, testEmail)
          .withPage(IndividualHavePhonePage, true)

        val section = testHelper.getContactDetails(userAnswers)

        section mustBe None
      }

      "should NOT return phone row when have phone is false" in {
        val userAnswers = emptyUserAnswers
          .withPage(IndividualEmailPage, testEmail)
          .withPage(IndividualHavePhonePage, false)
          .withPage(IndividualPhonePage, testPhone)

        val section: Section          = testHelper.getContactDetails(userAnswers).get
        val expectedTitle             = "Contact details"
        val expectedKeys: Seq[String] = Seq("Email address", "Can we contact the RCASP by phone?")

        compareRowsAndTitleToExpected(expectedTitle, expectedKeys, section)
      }

      "should NOT return a section when a page is missing" in {
        val userAnswers = emptyUserAnswers
          .withPage(IndividualEmailPage, testEmail)

        val section = testHelper.getContactDetails(userAnswers)

        section mustBe None
      }
    }
  }
}
