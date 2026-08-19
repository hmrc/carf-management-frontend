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

package navigation

import base.SpecBase
import config.Constants.noneOfTheseValue
import models.responses.AddressRegistrationResponse
import models.{formatAddress, CachedBusinessDetails, ChangeMode}
import org.scalatest.prop.Tables.Table
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import pages.*
import pages.individual.*
import pages.organisation.*

class ChangeRoutesNavigatorSpec extends SpecBase with ScalaCheckPropertyChecks {

  val navigator = new Navigator()

  case object TestPage extends Page

  "ChangeRoutesNavigator" - {
    "When passed an unknown page" - {
      "Should redirect to Journey Recovery" in {
        navigator.nextPage(
          TestPage,
          ChangeMode,
          emptyUserAnswers
        ) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
      }
    }

    "When passed FindAddressPage" - {
      "Should redirect to ChooseAddressPage when multiple addresses are returned" in {
        val userAnswers = emptyUserAnswers.withPage(AddressLookupResult, testAddressAndUprns)
        navigator.nextPage(
          FindAddressPage,
          ChangeMode,
          userAnswers
        ) mustBe controllers.routes.ChooseAddressController.onPageLoad(ChangeMode)
      }

      "Should redirect to ReviewAddressPage when one address is returned" in {
        val userAnswers = emptyUserAnswers.withPage(AddressPagePrePop, testAddressUk)
        navigator.nextPage(
          FindAddressPage,
          ChangeMode,
          userAnswers
        ) mustBe controllers.routes.ReviewAddressController.onPageLoad(ChangeMode)
      }

      "Should redirect to JourneyRecovery when no address is returned and navigation has occurred" in {
        navigator.nextPage(
          FindAddressPage,
          ChangeMode,
          emptyUserAnswers
        ) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
      }
    }

    "When passed ChooseAddressPage" - {
      "Should redirect to AddressController when 'none of these' is selected" in {
        val userAnswers = emptyUserAnswers.withPage(ChooseAddressPage, noneOfTheseValue)

        navigator.nextPage(
          ChooseAddressPage,
          ChangeMode,
          userAnswers
        ) mustBe controllers.routes.AddressController.onPageLoad(ChangeMode)
      }

      "Should redirect to EndOfJourneyRoutingController when an address is selected" in {
        val userAnswers = emptyUserAnswers.withPage(ChooseAddressPage, testAddressUk.format)

        navigator.nextPage(
          ChooseAddressPage,
          ChangeMode,
          userAnswers
        ) mustBe controllers.routes.EndOfJourneyRoutingController.onPageLoad()
      }

      "Should redirect to Journey Recovery when no answer is present" in {
        navigator.nextPage(
          ChooseAddressPage,
          ChangeMode,
          emptyUserAnswers
        ) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
      }
    }

    "for pages expecting to go to EndOfJourneyRoutingController" - {

      val pages = Table(
        "page expecting to go to check/change details",
        ReviewAddressPageForNavigatorOnly,
        AddressPageForNavigatorOnly,
        NiNumberPage,
        IndividualNamePage,
        IndividualEmailPage,
        IndividualPhonePage,
        OrganisationNamePage,
        TradingNamePage,
        UtrPage,
        OrganisationFirstContactNamePage,
        OrganisationFirstContactEmailPage,
        OrganisationFirstContactPhoneNumberPage,
        OrganisationSecondContactNamePage,
        OrganisationSecondContactEmailPage,
        OrganisationSecondContactPhoneNumberPage
      )

      "Should redirect to EndOfJourneyRoutingController" in {
        forAll(pages) { page =>
          navigator.nextPage(
            page,
            ChangeMode,
            emptyUserAnswers
          ) mustBe controllers.routes.EndOfJourneyRoutingController.onPageLoad()
        }
      }
    }

    "When passed RegisteredBusinessIsThisYourBusinessNamePage" - {
      "Should redirect to OrganisationNameController when answer is false" in {
        val userAnswers = emptyUserAnswers.withPage(RegisteredBusinessIsThisYourBusinessNamePage, false)

        navigator.nextPage(
          RegisteredBusinessIsThisYourBusinessNamePage,
          ChangeMode,
          userAnswers
        ) mustBe controllers.organisation.routes.OrganisationNameController.onPageLoad(ChangeMode)
      }

      "Should redirect to EndOfJourneyRoutingController when answer is true" in {
        val userAnswers = emptyUserAnswers.withPage(RegisteredBusinessIsThisYourBusinessNamePage, true)

        navigator.nextPage(
          RegisteredBusinessIsThisYourBusinessNamePage,
          ChangeMode,
          userAnswers
        ) mustBe controllers.routes.EndOfJourneyRoutingController.onPageLoad()
      }

      "Should redirect to Journey Recovery when no answer is present" in {
        navigator.nextPage(
          RegisteredBusinessIsThisYourBusinessNamePage,
          ChangeMode,
          emptyUserAnswers
        ) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
      }
    }

    "When passed RegisteredBusinessIsTheAddressCorrectPage" - {
      "Should redirect to EndOfJourneyRoutingController when answer is true and country is GB" in {
        val ua = emptyUserAnswers
          .withPage(RegisteredBusinessIsTheAddressCorrectPage, true)
          .withPage(CachedBusinessDetailsPage, cachedBusinessDetails)

        navigator.nextPage(
          RegisteredBusinessIsTheAddressCorrectPage,
          ChangeMode,
          ua
        ) mustBe controllers.routes.EndOfJourneyRoutingController.onPageLoad()
      }

      "Should redirect to NotInUkController when answer is true and country is not GB" in {
        val ua = emptyUserAnswers
          .withPage(RegisteredBusinessIsTheAddressCorrectPage, true)
          .withPage(
            CachedBusinessDetailsPage,
            cachedBusinessDetails.copy(address = cachedBusinessDetails.address.copy(countryCode = "US"))
          )

        navigator.nextPage(
          RegisteredBusinessIsTheAddressCorrectPage,
          ChangeMode,
          ua
        ) mustBe controllers.organisation.routes.NotInUkController.onPageLoad()
      }

      "Should redirect to FindAddressController when answer is false" in {
        val ua = emptyUserAnswers.withPage(RegisteredBusinessIsTheAddressCorrectPage, false)

        navigator.nextPage(
          RegisteredBusinessIsTheAddressCorrectPage,
          ChangeMode,
          ua
        ) mustBe controllers.routes.FindAddressController.onPageLoad(ChangeMode)
      }

      "Should redirect to Journey Recovery when no answer is present" in {
        navigator.nextPage(
          RegisteredBusinessIsTheAddressCorrectPage,
          ChangeMode,
          emptyUserAnswers
        ) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
      }

      "Should redirect to Journey Recovery when no cached business details found" in {
        val ua = emptyUserAnswers.withPage(RegisteredBusinessIsTheAddressCorrectPage, true)

        navigator.nextPage(
          RegisteredBusinessIsTheAddressCorrectPage,
          ChangeMode,
          ua
        ) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
      }
    }
  }
}
