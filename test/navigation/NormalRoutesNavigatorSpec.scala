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
import models.{CachedBusinessDetails, NormalMode, OrganisationOrIndividual}
import models.responses.AddressRegistrationResponse
import controllers.routes
import pages.Page
import models.{NormalMode, OrganisationOrIndividual}
import pages.{AddressLookupResult, AddressPagePrePop, FindAddressPage, Page}
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

      "Should redirect to JourneyRecoveryController if the page answer is false" in {
        val ua = emptyUserAnswers.withPage(HaveTradingNamePage, false)

        navigator.nextPage(
          HaveTradingNamePage,
          NormalMode,
          ua
        ) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
      }

      "Should redirect to Journey Recovery if the page answer is empty" in {
        navigator.nextPage(
          HaveTradingNamePage,
          NormalMode,
          emptyUserAnswers
        ) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
      }
    }

    "When passed UtrPage" - {
      "Should redirect to PlaceholderController for /find-address" in {
        navigator.nextPage(
          UtrPage,
          NormalMode,
          emptyUserAnswers
        ) mustBe controllers.routes.PlaceholderController.onPageLoad("Should redirect to /find-address (CARF-200)")
      }
    }

    "When passed RegisteredBusinessIsTheAddressCorrectPage" - {
      "Should redirect to PlaceholderController for /check-answers when answer is true and country is GB" in {
        val ua = emptyUserAnswers
          .withPage(RegisteredBusinessIsTheAddressCorrectPage, true)
          .withPage(CachedBusinessDetailsPage, cachedBusinessDetailsGb)

        navigator.nextPage(
          RegisteredBusinessIsTheAddressCorrectPage,
          NormalMode,
          ua
        ) mustBe controllers.routes.PlaceholderController.onPageLoad(
          "Should nav to /registered-business/check-answers (CARF-294)"
        )

      }

      "Should redirect to PlaceholderController for /check-answers when answer is true and country is lowercase gb" in {
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
        ) mustBe controllers.routes.PlaceholderController.onPageLoad(
          "Should nav to /registered-business/check-answers (CARF-294)"
        )
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

      "Should redirect to PlaceholderController for /find-address when answer is false" in {
        val ua = emptyUserAnswers.withPage(RegisteredBusinessIsTheAddressCorrectPage, false)

        navigator.nextPage(
          RegisteredBusinessIsTheAddressCorrectPage,
          NormalMode,
          ua
        ) mustBe controllers.routes.PlaceholderController.onPageLoad("Should redirect to /find-address (CARF-200)")
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
        ) mustBe
          controllers.organisation.routes.HaveTradingNameController.onPageLoad(NormalMode)
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

    "When passed OrganisationFirstContactEmailPage" - {
      "Should redirect to Org first contact have phone" in {
        navigator.nextPage(
          OrganisationFirstContactEmailPage,
          NormalMode,
          emptyUserAnswers
        ) mustBe controllers.organisation.routes.OrganisationFirstContactHavePhoneController.onPageLoad(NormalMode)
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
        val updatedAnswers =
          emptyUserAnswers
            .withPage(OrganisationHaveSecondContactPage, true)

        navigator.nextPage(
          OrganisationHaveSecondContactPage,
          NormalMode,
          updatedAnswers
        ) mustBe controllers.organisation.routes.OrganisationSecondContactNameController.onPageLoad(NormalMode)
      }

      "Should redirect to CheckYourAnswersPage when the provided answer is No" in {
        val updatedAnswers =
          emptyUserAnswers
            .withPage(OrganisationHaveSecondContactPage, false)

        navigator.nextPage(
          OrganisationHaveSecondContactPage,
          NormalMode,
          updatedAnswers
        ) mustBe controllers.routes.CheckDetailsController.onPageLoad
      }

      "Should redirect to JourneyRecovery when no answer is provided" in {
        val userAnswers = emptyUserAnswers

        navigator.nextPage(
          OrganisationHaveSecondContactPage,
          NormalMode,
          userAnswers
        ) mustBe routes.JourneyRecoveryController.onPageLoad()
      }
    }

    "When passed OrganisationSecondContactNamePage" - {
      "Should to OrganisationSecondContactEmailPage" in {
        val updatedAnswers =
          emptyUserAnswers
            .withPage(OrganisationSecondContactNamePage, "name")

        navigator.nextPage(
          OrganisationSecondContactNamePage,
          NormalMode,
          updatedAnswers
        ) mustBe controllers.organisation.routes.OrganisationSecondContactEmailController.onPageLoad(NormalMode)
      }
    }

    "When passed OrganisationSecondContactEmailPage" - {
      "Should redirect to OrganisationSecondContactHavePhonePage" in {
        val updatedAnswers =
          emptyUserAnswers
            .withPage(OrganisationSecondContactEmailPage, "email@email.com")

        navigator.nextPage(
          OrganisationSecondContactEmailPage,
          NormalMode,
          updatedAnswers
        ) mustBe controllers.organisation.routes.OrganisationSecondContactHavePhoneController.onPageLoad(NormalMode)
      }
    }

    "When passed OrganisationSecondContactHavePhonePage" - {
      "Should redirect to OrganisationSecondContactPhoneNumberPage when the provided answer is Yes" in {
        val userAnswers = emptyUserAnswers.withPage(OrganisationSecondContactHavePhonePage, true)
        navigator.nextPage(
          OrganisationSecondContactHavePhonePage,
          NormalMode,
          userAnswers
        ) mustBe controllers.organisation.routes.OrganisationSecondContactPhoneNumberController.onPageLoad(NormalMode)
      }

      "Should redirect to CheckYourAnswersPage when the provided answer is No" in {
        val userAnswers = emptyUserAnswers.withPage(OrganisationSecondContactHavePhonePage, false)
        navigator.nextPage(
          OrganisationSecondContactHavePhonePage,
          NormalMode,
          userAnswers
        ) mustBe controllers.routes.CheckDetailsController.onPageLoad
      }

      "Should redirect to JourneyRecovery when no answer is provided" in {
        val userAnswers = emptyUserAnswers

        navigator.nextPage(
          OrganisationSecondContactHavePhonePage,
          NormalMode,
          userAnswers
        ) mustBe routes.JourneyRecoveryController.onPageLoad()
      }
    }

    "When passed OrganisationSecondContactPhoneNumberPage" - {
      "Should redirect to CheckYourAnswersPage" in {
        val userAnswers = emptyUserAnswers.withPage(OrganisationSecondContactPhoneNumberPage, "07123412345")
        navigator.nextPage(
          OrganisationSecondContactPhoneNumberPage,
          NormalMode,
          userAnswers
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
        ) mustBe controllers.routes.PlaceholderController.onPageLoad("Should nav to /choose-address (CARF-201)")
      }

      "Should redirect to ReviewAddressPage when one address is returned" in {
        val userAnswers = emptyUserAnswers.withPage(AddressPagePrePop, testAddressUk)
        navigator.nextPage(
          FindAddressPage,
          NormalMode,
          userAnswers
        ) mustBe controllers.routes.PlaceholderController.onPageLoad("Should nav to /review-address (CARF-201)")
      }

      "Should redirect to JourneyRecovery when no address is returned and navigation has occurred" in {
        navigator.nextPage(
          FindAddressPage,
          NormalMode,
          emptyUserAnswers
        ) mustBe routes.JourneyRecoveryController.onPageLoad()
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
