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
import models.{format, ChangeMode, NormalMode}
import org.scalatest.prop.Tables.Table
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import pages.*
import pages.changeDetails.ChangeRcaspCachedDetails
import pages.individual.*
import pages.organisation.*

class ChangeRoutesNavigatorSpec extends SpecBase with ScalaCheckPropertyChecks {

  val navigator = new Navigator()

  case object TestPage extends Page

  "ChangeRoutesNavigator" - {
    "When passed any page" - {
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

    "When passed ReviewAddressPage" - {
      "Should redirect to ChangeDetailsController when ChangeRcaspCachedDetails is present" in {
        val userAnswers = emptyUserAnswers.withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsResponse)

        navigator.nextPage(
          ReviewAddressPageForNavigatorOnly,
          ChangeMode,
          userAnswers
        ) mustBe controllers.changeDetails.routes.ChangeDetailsController.onPageLoad(rcaspId)
      }

      "Should redirect to Journey Recovery when no answer is present" in {
        navigator.nextPage(
          ReviewAddressPageForNavigatorOnly,
          ChangeMode,
          emptyUserAnswers
        ) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
      }
    }

    "When passed ChooseAddressPage" - {
      "Should redirect to AddressController when 'none of these' is selected ChangeRcaspCachedDetails is present" in {
        val userAnswers = emptyUserAnswers.withPage(ChooseAddressPage, noneOfTheseValue)

        navigator.nextPage(
          ChooseAddressPage,
          ChangeMode,
          userAnswers
        ) mustBe controllers.routes.AddressController.onPageLoad(ChangeMode)
      }

      "Should redirect to ChangeDetailsController when an address is selected ChangeRcaspCachedDetails is present" in {
        val userAnswers = emptyUserAnswers
          .withPage(ChooseAddressPage, testAddressUk.format)
          .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsResponse)

        navigator.nextPage(
          ChooseAddressPage,
          ChangeMode,
          userAnswers
        ) mustBe controllers.changeDetails.routes.ChangeDetailsController.onPageLoad(rcaspId)
      }

      "Should redirect to JourneyRecoveryController when an address is selected and ChangeRcaspCachedDetails is not present" in {
        val userAnswers = emptyUserAnswers
          .withPage(ChooseAddressPage, testAddressUk.format)

        navigator.nextPage(
          ChooseAddressPage,
          ChangeMode,
          userAnswers
        ) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
      }

      "Should redirect to Journey Recovery when no answer is present" in {
        navigator.nextPage(
          ChooseAddressPage,
          ChangeMode,
          emptyUserAnswers
        ) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
      }
    }

    "When passed AddressPage" - {
      "Should redirect to ChangeDetailsController when ChangeRcaspCachedDetails is present" in {
        val userAnswers = emptyUserAnswers.withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsResponse)

        navigator.nextPage(
          AddressPageForNavigatorOnly,
          ChangeMode,
          userAnswers
        ) mustBe controllers.changeDetails.routes.ChangeDetailsController.onPageLoad(rcaspId)
      }

      "Should redirect to Journey Recovery when no answer is present" in {
        navigator.nextPage(
          AddressPageForNavigatorOnly,
          ChangeMode,
          emptyUserAnswers
        ) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
      }
    }

    "When passed IndividualNamePage" - {
      "Should redirect to ChangeDetailsController when ChangeRcaspCachedDetails is present" in {
        val userAnswers = emptyUserAnswers.withPage(ChangeRcaspCachedDetails, individualRcaspDetailsResponse)

        navigator.nextPage(
          IndividualNamePage,
          ChangeMode,
          userAnswers
        ) mustBe controllers.changeDetails.routes.ChangeDetailsController.onPageLoad(rcaspId)
      }

      "Should redirect to Journey Recovery when no answer is present" in {
        navigator.nextPage(
          IndividualNamePage,
          ChangeMode,
          emptyUserAnswers
        ) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
      }
    }

    "When passed NiNumberPage" - {
      "Should redirect to ChangeDetailsController when ChangeRcaspCachedDetails is present" in {
        val userAnswers = emptyUserAnswers.withPage(ChangeRcaspCachedDetails, individualRcaspDetailsResponse)

        navigator.nextPage(
          NiNumberPage,
          ChangeMode,
          userAnswers
        ) mustBe controllers.changeDetails.routes.ChangeDetailsController.onPageLoad(rcaspId)
      }

      "Should redirect to Journey Recovery when no answer is present" in {
        navigator.nextPage(
          NiNumberPage,
          ChangeMode,
          emptyUserAnswers
        ) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
      }
    }

    "When passed IndividualEmailPage" - {
      "Should redirect to ChangeDetailsController when ChangeRcaspCachedDetails is present" in {
        val userAnswers = emptyUserAnswers.withPage(ChangeRcaspCachedDetails, individualRcaspDetailsResponse)

        navigator.nextPage(
          IndividualEmailPage,
          ChangeMode,
          userAnswers
        ) mustBe controllers.changeDetails.routes.ChangeDetailsController.onPageLoad(rcaspId)
      }

      "Should redirect to Journey Recovery when no answer is present" in {
        navigator.nextPage(
          IndividualEmailPage,
          ChangeMode,
          emptyUserAnswers
        ) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
      }
    }

    "When passed IndividualHavePhonePage" - {
      "Should redirect to IndividualPhoneController when answer is true" in {
        val userAnswers = emptyUserAnswers.withPage(IndividualHavePhonePage, true)

        navigator.nextPage(
          IndividualHavePhonePage,
          ChangeMode,
          userAnswers
        ) mustBe controllers.individual.routes.IndividualPhoneController.onPageLoad(ChangeMode)
      }

      "Should redirect to ChangeDetailsController when answer is false and ChangeRcaspCachedDetails is present" in {
        val userAnswers = emptyUserAnswers
          .withPage(IndividualHavePhonePage, false)
          .withPage(ChangeRcaspCachedDetails, individualRcaspDetailsResponse)

        navigator.nextPage(
          IndividualHavePhonePage,
          ChangeMode,
          userAnswers
        ) mustBe controllers.changeDetails.routes.ChangeDetailsController.onPageLoad(rcaspId)
      }

      "Should redirect to Journey Recovery when answer is false and ChangeRcaspCachedDetails is missing" in {
        val userAnswers = emptyUserAnswers.withPage(IndividualHavePhonePage, false)

        navigator.nextPage(
          IndividualHavePhonePage,
          ChangeMode,
          userAnswers
        ) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
      }

      "Should redirect to Journey Recovery when no answer is present" in {
        navigator.nextPage(
          IndividualHavePhonePage,
          ChangeMode,
          emptyUserAnswers
        ) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
      }
    }

    "When passed IndividualPhonePage" - {
      "Should redirect to ChangeDetailsController when ChangeRcaspCachedDetails is present" in {
        val userAnswers = emptyUserAnswers.withPage(ChangeRcaspCachedDetails, individualRcaspDetailsResponse)

        navigator.nextPage(
          IndividualPhonePage,
          ChangeMode,
          userAnswers
        ) mustBe controllers.changeDetails.routes.ChangeDetailsController.onPageLoad(rcaspId)
      }

      "Should redirect to Journey Recovery when no answer is present" in {
        navigator.nextPage(
          IndividualPhonePage,
          ChangeMode,
          emptyUserAnswers
        ) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
      }
    }

    "for pages expecting to go to Change Details navigation" - {

      val pages = Table(
        "page expecting to go to change details",
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

      "Should redirect to ChangeDetailsController when ChangeRcaspCachedDetails is present" in {
        forAll(pages) { page =>
          val userAnswers = emptyUserAnswers.withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsResponse)

          navigator.nextPage(
            page,
            ChangeMode,
            userAnswers
          ) mustBe controllers.changeDetails.routes.ChangeDetailsController.onPageLoad(rcaspId)
        }
      }

      "Should redirect to Journey Recovery when no answer is present" in {
        forAll(pages) { page =>
          navigator.nextPage(page, ChangeMode, emptyUserAnswers) mustBe controllers.routes.JourneyRecoveryController
            .onPageLoad()
        }
      }
    }

    "When passed HaveTradingNamePage" - {

      "Should redirect to TradingNameController when answer is true" in {
        val userAnswers = emptyUserAnswers.withPage(HaveTradingNamePage, true)

        navigator.nextPage(
          HaveTradingNamePage,
          ChangeMode,
          userAnswers
        ) mustBe controllers.organisation.routes.TradingNameController.onPageLoad(ChangeMode)
      }

      "Should redirect to ChangeDetailsController when answer is false and ChangeRcaspCachedDetails is present" in {
        val userAnswers = emptyUserAnswers
          .withPage(HaveTradingNamePage, false)
          .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsResponse)

        navigator.nextPage(
          HaveTradingNamePage,
          ChangeMode,
          userAnswers
        ) mustBe controllers.changeDetails.routes.ChangeDetailsController.onPageLoad(rcaspId)
      }

      "Should redirect to Journey Recovery when answer is false and ChangeRcaspCachedDetails is missing" in {
        val userAnswers = emptyUserAnswers.withPage(HaveTradingNamePage, false)

        navigator.nextPage(
          HaveTradingNamePage,
          ChangeMode,
          userAnswers
        ) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
      }

      "Should redirect to Journey Recovery when no answer is present" in {
        navigator.nextPage(
          HaveTradingNamePage,
          ChangeMode,
          emptyUserAnswers
        ) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
      }
    }

    "When passed OrganisationFirstContactHavePhonePage" - {

      "Should redirect to OrganisationFirstContactPhoneNumberController when answer is true" in {
        val userAnswers = emptyUserAnswers.withPage(OrganisationFirstContactHavePhonePage, true)

        navigator.nextPage(
          OrganisationFirstContactHavePhonePage,
          ChangeMode,
          userAnswers
        ) mustBe controllers.organisation.routes.OrganisationFirstContactPhoneNumberController.onPageLoad(ChangeMode)
      }

      "Should redirect to ChangeDetailsController when answer is false and ChangeRcaspCachedDetails is present" in {
        val userAnswers = emptyUserAnswers
          .withPage(OrganisationFirstContactHavePhonePage, false)
          .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsResponse)

        navigator.nextPage(
          OrganisationFirstContactHavePhonePage,
          ChangeMode,
          userAnswers
        ) mustBe controllers.changeDetails.routes.ChangeDetailsController.onPageLoad(rcaspId)
      }

      "Should redirect to Journey Recovery when answer is false and ChangeRcaspCachedDetails is missing" in {
        val userAnswers = emptyUserAnswers.withPage(OrganisationFirstContactHavePhonePage, false)

        navigator.nextPage(
          OrganisationFirstContactHavePhonePage,
          ChangeMode,
          userAnswers
        ) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
      }

      "Should redirect to Journey Recovery when no answer is present" in {
        navigator.nextPage(
          OrganisationFirstContactHavePhonePage,
          ChangeMode,
          emptyUserAnswers
        ) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
      }
    }

    "When passed OrganisationHaveSecondContactPage" - {

      "Should redirect to OrganisationSecondContactNameController when answer is true" in {
        val userAnswers = emptyUserAnswers.withPage(OrganisationHaveSecondContactPage, true)

        navigator.nextPage(
          OrganisationHaveSecondContactPage,
          ChangeMode,
          userAnswers
        ) mustBe controllers.organisation.routes.OrganisationSecondContactNameController
          .onPageLoad(NormalMode)
      }

      "Should redirect to ChangeDetailsController when answer is false and ChangeRcaspCachedDetails is present" in {
        val userAnswers = emptyUserAnswers
          .withPage(OrganisationHaveSecondContactPage, false)
          .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsResponse)

        navigator.nextPage(
          OrganisationHaveSecondContactPage,
          ChangeMode,
          userAnswers
        ) mustBe controllers.changeDetails.routes.ChangeDetailsController.onPageLoad(rcaspId)
      }

      "Should redirect to Journey Recovery when answer is false and ChangeRcaspCachedDetails is missing" in {
        val userAnswers = emptyUserAnswers.withPage(OrganisationHaveSecondContactPage, false)

        navigator.nextPage(
          OrganisationHaveSecondContactPage,
          ChangeMode,
          userAnswers
        ) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
      }

      "Should redirect to Journey Recovery when no answer is present" in {
        navigator.nextPage(
          OrganisationHaveSecondContactPage,
          ChangeMode,
          emptyUserAnswers
        ) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
      }
    }

    "When passed OrganisationSecondContactHavePhonePage" - {

      "Should redirect to OrganisationSecondContactPhoneNumberController when answer is true" in {
        val userAnswers = emptyUserAnswers.withPage(OrganisationSecondContactHavePhonePage, true)

        navigator.nextPage(
          OrganisationSecondContactHavePhonePage,
          ChangeMode,
          userAnswers
        ) mustBe controllers.organisation.routes.OrganisationSecondContactPhoneNumberController.onPageLoad(ChangeMode)
      }

      "Should redirect to ChangeDetailsController when answer is false and ChangeRcaspCachedDetails is present" in {
        val userAnswers = emptyUserAnswers
          .withPage(OrganisationSecondContactHavePhonePage, false)
          .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsResponse)

        navigator.nextPage(
          OrganisationSecondContactHavePhonePage,
          ChangeMode,
          userAnswers
        ) mustBe controllers.changeDetails.routes.ChangeDetailsController.onPageLoad(rcaspId)
      }

      "Should redirect to Journey Recovery when answer is false and ChangeRcaspCachedDetails is missing" in {
        val userAnswers = emptyUserAnswers.withPage(OrganisationSecondContactHavePhonePage, false)

        navigator.nextPage(
          OrganisationSecondContactHavePhonePage,
          ChangeMode,
          userAnswers
        ) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
      }

      "Should redirect to Journey Recovery when no answer is present" in {
        navigator.nextPage(
          OrganisationSecondContactHavePhonePage,
          ChangeMode,
          emptyUserAnswers
        ) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
      }
    }
  }
}
