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
import models.{NormalMode, OrganisationOrIndividual}
import controllers.routes
import pages.Page
import pages.individual.*
import pages.organisation.*
import pages.combined.OrganisationOrIndividualPage

class NormalRoutesNavigatorSpec extends SpecBase {

  val navigator = new Navigator()

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

      "Should redirect to PlaceholderController if the page answer is false" in {
        val ua = emptyUserAnswers.withPage(HaveTradingNamePage, false)

        navigator.nextPage(
          HaveTradingNamePage,
          NormalMode,
          ua
        ) mustBe controllers.routes.PlaceholderController.onPageLoad(
          "If is RCASP user = true, nav to /is-the-address-correct, else nav to /utr (CARF-197)"
        )
      }

      "Should redirect to Journey Recovery if the page answer is empty" in {
        navigator.nextPage(
          HaveTradingNamePage,
          NormalMode,
          emptyUserAnswers
        ) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
      }
    }

    "When passed TradingNamePage" - {
      "Should redirect to PlaceholderController" in {
        navigator.nextPage(
          TradingNamePage,
          NormalMode,
          emptyUserAnswers
        ) mustBe controllers.routes.PlaceholderController.onPageLoad(
          "If is RCASP user = true, nav to /is-the-address-correct, else nav to /utr (CARF-197)"
        )
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
        ) mustBe controllers.routes.PlaceholderController.onPageLoad("Should redirect to /find-address (CARF-200)")
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
        ) mustBe controllers.routes.PlaceholderController.onPageLoad("Should nav to /check-answers (CARF-540)")
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
        ) mustBe controllers.routes.PlaceholderController.onPageLoad("Should nav to /check-answers (CARF-540)")
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
      "Should redirect to PlaceholderController when answer is true" in {
        val ua = emptyUserAnswers.withPage(RegisteredBusinessIsThisYourBusinessNamePage, true)

        navigator.nextPage(
          RegisteredBusinessIsThisYourBusinessNamePage,
          NormalMode,
          ua
        ) mustBe
          controllers.organisation.routes.HaveTradingNameController.onPageLoad(NormalMode)
      }

      "Should redirect to PlaceholderController when answer is false" in {
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
        ) mustBe controllers.routes.PlaceholderController.onPageLoad("Should nav to /check-answers (CARF-540)")
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
        ) mustBe controllers.routes.PlaceholderController.onPageLoad("Should nav to /check-answers (CARF-540)")
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
        ) mustBe controllers.routes.PlaceholderController.onPageLoad("Should nav to /check-answers (CARF-540)")
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
