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
import models.*
import models.OrganisationOrIndividual.{Individual, Organisation}
import models.responses.AddressRegistrationResponse
import pages.*
import pages.combined.OrganisationOrIndividualPage
import pages.individual.*
import pages.organisation.*

class NormalRoutesNavigatorSpec extends SpecBase {

  val navigator = new Navigator()

  val cachedBusinessDetailsGb: CachedBusinessDetails =
    CachedBusinessDetails(
      name = "Test Business Ltd",
      address = AddressRegistrationResponse(
        addressLine1 = "1 Test Street",
        addressLine2 = Some("Testville"),
        addressLine3 = None,
        addressLine4 = None,
        postalCode = Some("TE1 1ST"),
        countryCode = "GB"
      ),
      countryName = "United Kingdom"
    )

  val cachedBusinessDetailsNonGb: CachedBusinessDetails =
    CachedBusinessDetails(
      name = "Test Business Ltd",
      address = AddressRegistrationResponse(
        addressLine1 = "3 Apple Street",
        addressLine2 = Some("New York"),
        addressLine3 = None,
        addressLine4 = None,
        postalCode = Some("11722"),
        countryCode = "US"
      ),
      countryName = "United States"
    )

  "NormalRoutesNavigator" - {

    "When passed OrganisationNamePage" - {
      "Should redirect to HaveTradingNameController" in {
        navigator.nextPage(
          OrganisationNamePage,
          NormalMode,
          emptyUserAnswers
        ) mustBe controllers.organisation.routes.HaveTradingNameController.onPageLoad(NormalMode)
      }
    }

    "When passed HaveTradingNamePage" - {
      "Should redirect to TradingNameController if the page answer is true" in {
        val ua = emptyUserAnswers.withPage(HaveTradingNamePage, true)

        navigator.nextPage(
          HaveTradingNamePage,
          NormalMode,
          ua
        ) mustBe controllers.organisation.routes.TradingNameController.onPageLoad(NormalMode)
      }

      "Should redirect to RegisteredBusinessIsTheAddressCorrectController if the page answer is no but the user is the registered business" in {
        val ua = emptyUserAnswers
          .copy(rcaspIsRegisteredBusiness = true)
          .withPage(HaveTradingNamePage, false)

        navigator.nextPage(
          HaveTradingNamePage,
          NormalMode,
          ua
        ) mustBe controllers.organisation.routes.RegisteredBusinessIsTheAddressCorrectController.onPageLoad(NormalMode)
      }

      "Should redirect to UtrController if the page answer is no but the user is NOT the registered business" in {
        val ua = emptyUserAnswers.withPage(HaveTradingNamePage, false)

        navigator.nextPage(
          HaveTradingNamePage,
          NormalMode,
          ua
        ) mustBe controllers.organisation.routes.UtrController.onPageLoad(NormalMode)
      }
    }

    "When passed UtrPage" - {
      "Should redirect to FindAddressController" in {
        navigator.nextPage(
          UtrPage,
          NormalMode,
          emptyUserAnswers
        ) mustBe controllers.routes.FindAddressController.onPageLoad(NormalMode)
      }
    }

    "When passed TradingNamePage" - {
      "Should redirect to RegisteredBusinessIsTheAddressCorrectController when the user is the registered business" in {
        val ua = emptyUserAnswers.copy(rcaspIsRegisteredBusiness = true)

        navigator.nextPage(
          TradingNamePage,
          NormalMode,
          ua
        ) mustBe controllers.organisation.routes.RegisteredBusinessIsTheAddressCorrectController.onPageLoad(NormalMode)
      }

      "Should redirect to UtrController when the user is NOT the registered business" in {
        navigator.nextPage(
          TradingNamePage,
          NormalMode,
          emptyUserAnswers
        ) mustBe controllers.organisation.routes.UtrController.onPageLoad(NormalMode)
      }
    }

    "When passed RegisteredBusinessIsTheAddressCorrectPage" - {
      "Should redirect to RegisteredBusinessCheckDetailsController when answer is true and country is GB" in {
        val ua = emptyUserAnswers
          .withPage(RegisteredBusinessIsTheAddressCorrectPage, true)
          .withPage(CachedBusinessDetailsPage, cachedBusinessDetailsGb)

        navigator.nextPage(
          RegisteredBusinessIsTheAddressCorrectPage,
          NormalMode,
          ua
        ) mustBe controllers.organisation.routes.RegisteredBusinessCheckDetailsController.onPageLoad

      }

      "Should redirect to RegisteredBusinessCheckDetailsController when answer is true and country is lowercase gb" in {
        val ua = emptyUserAnswers
          .withPage(RegisteredBusinessIsTheAddressCorrectPage, true)
          .withPage(
            CachedBusinessDetailsPage,
            cachedBusinessDetailsGb.copy(
              address = cachedBusinessDetailsGb.address.copy(countryCode = "gb")
            )
          )

        navigator.nextPage(
          RegisteredBusinessIsTheAddressCorrectPage,
          NormalMode,
          ua
        ) mustBe controllers.organisation.routes.RegisteredBusinessCheckDetailsController.onPageLoad
      }

      "Should redirect to NotInUkController when answer is true and country is not GB" in {
        val ua = emptyUserAnswers
          .withPage(RegisteredBusinessIsTheAddressCorrectPage, true)
          .withPage(CachedBusinessDetailsPage, cachedBusinessDetailsNonGb)

        navigator.nextPage(
          RegisteredBusinessIsTheAddressCorrectPage,
          NormalMode,
          ua
        ) mustBe controllers.organisation.routes.NotInUkController.onPageLoad()
      }

      "Should redirect to FindAddressController when answer is false" in {
        val ua = emptyUserAnswers.withPage(RegisteredBusinessIsTheAddressCorrectPage, false)

        navigator.nextPage(
          RegisteredBusinessIsTheAddressCorrectPage,
          NormalMode,
          ua
        ) mustBe controllers.routes.FindAddressController.onPageLoad(NormalMode)
      }

      "Should redirect to Journey Recovery when no answer is present" in {
        navigator.nextPage(
          RegisteredBusinessIsTheAddressCorrectPage,
          NormalMode,
          emptyUserAnswers
        ) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
      }

      "Should redirect to Journey Recovery when no cached business details found" in {
        val ua = emptyUserAnswers.withPage(RegisteredBusinessIsTheAddressCorrectPage, true)

        navigator.nextPage(
          RegisteredBusinessIsTheAddressCorrectPage,
          NormalMode,
          ua
        ) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
      }
    }

    "When passed IndividualNamePage" - {
      "Should redirect to NiNumberController" in {
        navigator.nextPage(
          IndividualNamePage,
          NormalMode,
          emptyUserAnswers
        ) mustBe controllers.individual.routes.NiNumberController.onPageLoad(NormalMode)
      }
    }

    "When passed NiNumberPage" - {
      "Should redirect to FindAddressController" in {
        navigator.nextPage(
          NiNumberPage,
          NormalMode,
          emptyUserAnswers
        ) mustBe controllers.routes.FindAddressController.onPageLoad(NormalMode)
      }
    }

    "When passed IndividualEmailPage" - {
      "Should redirect to IndividualHavePhoneController" in {
        navigator.nextPage(
          IndividualEmailPage,
          NormalMode,
          emptyUserAnswers
        ) mustBe controllers.individual.routes.IndividualHavePhoneController.onPageLoad(NormalMode)
      }
    }

    "When passed IndividualHavePhonePage" - {
      "Should redirect to Check answers controller when the user answered No on the page" in {
        val ua = emptyUserAnswers.withPage(IndividualHavePhonePage, false)

        navigator.nextPage(
          IndividualHavePhonePage,
          NormalMode,
          ua
        ) mustBe controllers.routes.CheckDetailsController.onPageLoad
      }

      "Should redirect to IndividualPhoneController when the user answered Yes on the page" in {
        val ua = emptyUserAnswers.withPage(IndividualHavePhonePage, true)

        navigator.nextPage(
          IndividualHavePhonePage,
          NormalMode,
          ua
        ) mustBe controllers.individual.routes.IndividualPhoneController.onPageLoad(NormalMode)
      }

      "Should redirect to Journey Recovery when the user has no answer for the page" in {
        navigator.nextPage(
          IndividualHavePhonePage,
          NormalMode,
          emptyUserAnswers
        ) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
      }
    }

    "When passed IndividualPhonePage" - {
      "Should redirect to Check answers controller" in {
        navigator.nextPage(
          IndividualPhonePage,
          NormalMode,
          emptyUserAnswers
        ) mustBe controllers.routes.CheckDetailsController.onPageLoad
      }
    }

    "When passed OrganisationOrIndividualPage" - {
      "Should redirect to OrganisationNameController when Organisation is selected" in {
        val ua = emptyUserAnswers.withPage(OrganisationOrIndividualPage, OrganisationOrIndividual.Organisation)

        navigator.nextPage(
          OrganisationOrIndividualPage,
          NormalMode,
          ua
        ) mustBe controllers.organisation.routes.OrganisationNameController.onPageLoad(NormalMode)
      }

      "Should redirect to IndividualNameController when Individual is selected" in {
        val ua = emptyUserAnswers.withPage(OrganisationOrIndividualPage, OrganisationOrIndividual.Individual)

        navigator.nextPage(
          OrganisationOrIndividualPage,
          NormalMode,
          ua
        ) mustBe controllers.individual.routes.IndividualNameController.onPageLoad(NormalMode)
      }

      "Should redirect to Journey Recovery when no answer is present" in {
        navigator.nextPage(
          OrganisationOrIndividualPage,
          NormalMode,
          emptyUserAnswers
        ) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
      }
    }

    "When passed ReportForRegisteredBusinessPage" - {
      "Should redirect to RegisteredBusinessIsThisYourBusinessNameController when answer is true" in {
        val ua = emptyUserAnswers.withPage(ReportForRegisteredBusinessPage, true)

        navigator.nextPage(
          ReportForRegisteredBusinessPage,
          NormalMode,
          ua
        ) mustBe controllers.organisation.routes.RegisteredBusinessIsThisYourBusinessNameController
          .onPageLoad(NormalMode)
      }

      "Should redirect to OrganisationOrIndividualController when answer is false" in {
        val ua = emptyUserAnswers.withPage(ReportForRegisteredBusinessPage, false)

        navigator.nextPage(
          ReportForRegisteredBusinessPage,
          NormalMode,
          ua
        ) mustBe controllers.combined.routes.OrganisationOrIndividualController.onPageLoad(NormalMode)
      }

      "Should redirect to Journey Recovery when no answer is present" in {
        navigator.nextPage(
          ReportForRegisteredBusinessPage,
          NormalMode,
          emptyUserAnswers
        ) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
      }
    }

    "When passed RegisteredBusinessIsThisYourBusinessNamePage" - {
      "Should redirect to HaveTradingNameController when answer is true" in {
        val ua = emptyUserAnswers.withPage(RegisteredBusinessIsThisYourBusinessNamePage, true)

        navigator.nextPage(
          RegisteredBusinessIsThisYourBusinessNamePage,
          NormalMode,
          ua
        ) mustBe controllers.organisation.routes.HaveTradingNameController.onPageLoad(NormalMode)
      }

      "Should redirect to OrganisationNameController when answer is false" in {
        val ua = emptyUserAnswers.withPage(RegisteredBusinessIsThisYourBusinessNamePage, false)

        navigator.nextPage(
          RegisteredBusinessIsThisYourBusinessNamePage,
          NormalMode,
          ua
        ) mustBe controllers.organisation.routes.OrganisationNameController.onPageLoad(NormalMode)
      }

      "Should redirect to Journey Recovery when no answer is present" in {
        navigator.nextPage(
          RegisteredBusinessIsThisYourBusinessNamePage,
          NormalMode,
          emptyUserAnswers
        ) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
      }
    }

    "When passed OrganisationFirstContactNamePage" - {
      "Should redirect to Org first contact email" in {
        navigator.nextPage(
          OrganisationFirstContactNamePage,
          NormalMode,
          emptyUserAnswers
        ) mustBe controllers.organisation.routes.OrganisationFirstContactEmailController.onPageLoad(NormalMode)
      }
    }

    "When passed OrganisationFirstContactEmailPage" - {
      "Should redirect to Org first contact have phone" in {
        navigator.nextPage(
          OrganisationFirstContactEmailPage,
          NormalMode,
          emptyUserAnswers
        ) mustBe controllers.organisation.routes.OrganisationFirstContactHavePhoneController.onPageLoad(NormalMode)
      }
    }

    "When passed OrganisationFirstContactHavePhonePage" - {
      "Should redirect to Org first contact phone number if the page answer was true" in {
        val ua = emptyUserAnswers.withPage(OrganisationFirstContactHavePhonePage, true)

        navigator.nextPage(
          OrganisationFirstContactHavePhonePage,
          NormalMode,
          ua
        ) mustBe controllers.organisation.routes.OrganisationFirstContactPhoneNumberController.onPageLoad(NormalMode)
      }

      "Should redirect to Org have second contact if the page answer was false" in {
        val ua = emptyUserAnswers.withPage(OrganisationFirstContactHavePhonePage, false)

        navigator.nextPage(
          OrganisationFirstContactHavePhonePage,
          NormalMode,
          ua
        ) mustBe controllers.organisation.routes.OrganisationHaveSecondContactController.onPageLoad(NormalMode)
      }

      "Should redirect to Journey Recovery if the page answer is None" in {
        navigator.nextPage(
          OrganisationFirstContactHavePhonePage,
          NormalMode,
          emptyUserAnswers
        ) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
      }
    }

    "When passed OrganisationFirstContactPhoneNumberPage" - {
      "Should redirect to Org have second contact" in {
        navigator.nextPage(
          OrganisationFirstContactPhoneNumberPage,
          NormalMode,
          emptyUserAnswers
        ) mustBe controllers.organisation.routes.OrganisationHaveSecondContactController.onPageLoad(NormalMode)
      }
    }

    "When passed OrganisationHaveSecondContactPage" - {
      "Should redirect to OrganisationSecondContactNamePage when the provided answer is Yes" in {
        val ua = emptyUserAnswers.withPage(OrganisationHaveSecondContactPage, true)

        navigator.nextPage(
          OrganisationHaveSecondContactPage,
          NormalMode,
          ua
        ) mustBe controllers.organisation.routes.OrganisationSecondContactNameController.onPageLoad(NormalMode)
      }

      "Should redirect to CheckDetailsController when the provided answer is No" in {
        val ua = emptyUserAnswers.withPage(OrganisationHaveSecondContactPage, false)

        navigator.nextPage(
          OrganisationHaveSecondContactPage,
          NormalMode,
          ua
        ) mustBe controllers.routes.CheckDetailsController.onPageLoad
      }

      "Should redirect to JourneyRecovery when no answer is provided" in {
        navigator.nextPage(
          OrganisationHaveSecondContactPage,
          NormalMode,
          emptyUserAnswers
        ) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
      }
    }

    "When passed OrganisationSecondContactNamePage" - {
      "Should redirect to OrganisationSecondContactEmailPage" in {
        val ua = emptyUserAnswers.withPage(OrganisationSecondContactNamePage, "name")

        navigator.nextPage(
          OrganisationSecondContactNamePage,
          NormalMode,
          ua
        ) mustBe controllers.organisation.routes.OrganisationSecondContactEmailController.onPageLoad(NormalMode)
      }
    }

    "When passed OrganisationSecondContactEmailPage" - {
      "Should redirect to OrganisationSecondContactHavePhonePage" in {
        val ua = emptyUserAnswers.withPage(OrganisationSecondContactEmailPage, "email@email.com")

        navigator.nextPage(
          OrganisationSecondContactEmailPage,
          NormalMode,
          ua
        ) mustBe controllers.organisation.routes.OrganisationSecondContactHavePhoneController.onPageLoad(NormalMode)
      }
    }

    "When passed OrganisationSecondContactHavePhonePage" - {
      "Should redirect to OrganisationSecondContactPhoneNumberPage when the provided answer is Yes" in {
        val ua = emptyUserAnswers.withPage(OrganisationSecondContactHavePhonePage, true)

        navigator.nextPage(
          OrganisationSecondContactHavePhonePage,
          NormalMode,
          ua
        ) mustBe controllers.organisation.routes.OrganisationSecondContactPhoneNumberController.onPageLoad(NormalMode)
      }

      "Should redirect to CheckDetailsController when the provided answer is No" in {
        val ua = emptyUserAnswers.withPage(OrganisationSecondContactHavePhonePage, false)

        navigator.nextPage(
          OrganisationSecondContactHavePhonePage,
          NormalMode,
          ua
        ) mustBe controllers.routes.CheckDetailsController.onPageLoad
      }

      "Should redirect to JourneyRecovery when no answer is provided" in {
        navigator.nextPage(
          OrganisationSecondContactHavePhonePage,
          NormalMode,
          emptyUserAnswers
        ) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
      }
    }

    "When passed OrganisationSecondContactPhoneNumberPage" - {
      "Should redirect to CheckDetailsController" in {
        val ua = emptyUserAnswers.withPage(OrganisationSecondContactPhoneNumberPage, "07123412345")

        navigator.nextPage(
          OrganisationSecondContactPhoneNumberPage,
          NormalMode,
          ua
        ) mustBe controllers.routes.CheckDetailsController.onPageLoad
      }
    }

    "When passed FindAddressPage" - {
      "Should redirect to ChooseAddressPage when multiple addresses are returned" in {
        val userAnswers = emptyUserAnswers.withPage(AddressLookupResult, testAddressAndUprns)
        navigator.nextPage(
          FindAddressPage,
          NormalMode,
          userAnswers
        ) mustBe controllers.routes.ChooseAddressController.onPageLoad(NormalMode)
      }

      "Should redirect to ReviewAddressPage when one address is returned" in {
        val userAnswers = emptyUserAnswers.withPage(AddressPagePrePop, testAddressUk)
        navigator.nextPage(
          FindAddressPage,
          NormalMode,
          userAnswers
        ) mustBe controllers.routes.ReviewAddressController.onPageLoad(NormalMode)
      }

      "Should redirect to JourneyRecovery when no address is returned and navigation has occurred" in {
        navigator.nextPage(
          FindAddressPage,
          NormalMode,
          emptyUserAnswers
        ) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
      }
    }

    "When passed ChooseAddressPage" - {
      "Should redirect to IndividualEmailPage when address is selected on individual journey and user is not an rcasp" in {
        val userAnswers = emptyUserAnswers
          .copy(rcaspIsRegisteredBusiness = false)
          .withPage(ChooseAddressPage, testAddressUk.formatAddress)
          .withPage(OrganisationOrIndividualPage, Individual)

        navigator.nextPage(
          ChooseAddressPage,
          NormalMode,
          userAnswers
        ) mustBe controllers.individual.routes.IndividualEmailController.onPageLoad(NormalMode)
      }

      "Should redirect to OrganisationFirstContactNamePage when address is selected on organisation journey and user is not an rcasp" in {
        val userAnswers = emptyUserAnswers
          .copy(rcaspIsRegisteredBusiness = false)
          .withPage(ChooseAddressPage, testAddressUk.formatAddress)
          .withPage(OrganisationOrIndividualPage, Organisation)

        navigator.nextPage(
          ChooseAddressPage,
          NormalMode,
          userAnswers
        ) mustBe controllers.organisation.routes.OrganisationFirstContactNameController.onPageLoad(NormalMode)
      }

      "Should redirect to RegisteredBusinessCheckDetailsController when address is selected the rcasp is the registered business" in {
        val userAnswers = emptyUserAnswers
          .copy(rcaspIsRegisteredBusiness = true)
          .withPage(ChooseAddressPage, testAddressUk.formatAddress)

        navigator.nextPage(
          ChooseAddressPage,
          NormalMode,
          userAnswers
        ) mustBe controllers.organisation.routes.RegisteredBusinessCheckDetailsController.onPageLoad
      }

      "Should redirect to ReviewAddressPage when none of these is selected" in {
        val userAnswers = emptyUserAnswers
          .withPage(ChooseAddressPage, noneOfTheseValue)

        navigator.nextPage(
          ChooseAddressPage,
          NormalMode,
          userAnswers
        ) mustBe controllers.routes.AddressController.onPageLoad(NormalMode)
      }

      "Should redirect to JourneyRecovery when address is selected but OrganisationOrIndividual is missing" in {
        val userAnswers = emptyUserAnswers
          .withPage(ChooseAddressPage, testAddressUk.formatAddress)

        navigator.nextPage(
          ChooseAddressPage,
          NormalMode,
          userAnswers
        ) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
      }

      "Should redirect to JourneyRecovery when no value is selected" in {
        navigator.nextPage(
          ChooseAddressPage,
          NormalMode,
          emptyUserAnswers
        ) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
      }
    }

    "When passed ReviewAddressPage" - {
      "Should redirect to RegisteredBusinessCheckDetailsController the rcasp is the registered business" in {
        val userAnswers = emptyUserAnswers.copy(rcaspIsRegisteredBusiness = true)

        navigator.nextPage(
          ReviewAddressPageForNavigatorOnly,
          NormalMode,
          userAnswers
        ) mustBe controllers.organisation.routes.RegisteredBusinessCheckDetailsController.onPageLoad
      }

      "Should redirect to IndividualEmailPage when address is selected on individual journey and user is not an rcasp" in {
        val userAnswers = emptyUserAnswers
          .copy(rcaspIsRegisteredBusiness = false)
          .withPage(OrganisationOrIndividualPage, Individual)

        navigator.nextPage(
          ReviewAddressPageForNavigatorOnly,
          NormalMode,
          userAnswers
        ) mustBe controllers.individual.routes.IndividualEmailController.onPageLoad(NormalMode)
      }

      "Should redirect to OrganisationFirstContactNamePage when address is selected on organisation journey and user is not an rcasp" in {
        val userAnswers = emptyUserAnswers
          .copy(rcaspIsRegisteredBusiness = false)
          .withPage(OrganisationOrIndividualPage, Organisation)

        navigator.nextPage(
          ReviewAddressPageForNavigatorOnly,
          NormalMode,
          userAnswers
        ) mustBe controllers.organisation.routes.OrganisationFirstContactNameController.onPageLoad(NormalMode)
      }

      "Should redirect to JourneyRecovery when OrganisationOrIndividual is missing and user is not an rcasp" in {
        val userAnswers = emptyUserAnswers.copy(rcaspIsRegisteredBusiness = false)

        navigator.nextPage(
          ReviewAddressPageForNavigatorOnly,
          NormalMode,
          userAnswers
        ) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
      }
    }

    "When passed AddressPage" - {
      "Should redirect to RegisteredBusinessCheckDetailsController the rcasp is the registered business" in {
        val userAnswers = emptyUserAnswers.copy(rcaspIsRegisteredBusiness = true)

        navigator.nextPage(
          AddressPageForNavigatorOnly,
          NormalMode,
          userAnswers
        ) mustBe controllers.organisation.routes.RegisteredBusinessCheckDetailsController.onPageLoad
      }

      "Should redirect to IndividualEmailPage when address is selected on individual journey and user is not an rcasp" in {
        val userAnswers = emptyUserAnswers
          .copy(rcaspIsRegisteredBusiness = false)
          .withPage(OrganisationOrIndividualPage, Individual)

        navigator.nextPage(
          AddressPageForNavigatorOnly,
          NormalMode,
          userAnswers
        ) mustBe controllers.individual.routes.IndividualEmailController.onPageLoad(NormalMode)
      }

      "Should redirect to OrganisationFirstContactNamePage when address is selected on organisation journey and user is not an rcasp" in {
        val userAnswers = emptyUserAnswers
          .copy(rcaspIsRegisteredBusiness = false)
          .withPage(OrganisationOrIndividualPage, Organisation)

        navigator.nextPage(
          AddressPageForNavigatorOnly,
          NormalMode,
          userAnswers
        ) mustBe controllers.organisation.routes.OrganisationFirstContactNameController.onPageLoad(NormalMode)
      }

      "Should redirect to JourneyRecovery when OrganisationOrIndividual is missing and user is not an rcasp" in {
        val userAnswers = emptyUserAnswers.copy(rcaspIsRegisteredBusiness = false)

        navigator.nextPage(
          AddressPageForNavigatorOnly,
          NormalMode,
          userAnswers
        ) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
      }
    }

    "When passed an unknown page" - {
      "Should redirect to journey recovery" in {
        case object UnknownPage extends Page

        navigator.nextPage(
          UnknownPage,
          NormalMode,
          emptyUserAnswers
        ) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
      }
    }
  }
}
