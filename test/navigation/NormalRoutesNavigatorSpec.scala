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
import controllers.routes
import models.NormalMode
import pages.Page
import pages.organisation.{HaveTradingNamePage, OrganisationHaveSecondContactPage, OrganisationNamePage, OrganisationSecondContactHavePhonePage, OrganisationSecondContactNamePage, TradingNamePage}

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
          "If is RCASP user = true, nav to /is-the-business-correct, else nav to /utr (CARF-197)"
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
          "If is RCASP user = true, nav to /is-the-business-correct, else nav to /utr (CARF-197)"
        )
      }
    }

    "must navigate from OrganisationHaveSecondContact page to OrganisationSecondContactName page when the provided answer is Yes" in {

      val updatedAnswers =
        emptyUserAnswers
          .withPage(OrganisationHaveSecondContactPage, true)

      navigator.nextPage(
        OrganisationHaveSecondContactPage,
        NormalMode,
        updatedAnswers
      ) mustBe controllers.organisation.routes.OrganisationSecondContactNameController.onPageLoad(NormalMode)
    }

    "must navigate from OrganisationSecondContactName page to OrganisationSecondContactEmail page when continue is clicked" in {

      val updatedAnswers =
        emptyUserAnswers
          .withPage(OrganisationSecondContactNamePage, "name")

      navigator.nextPage(
        OrganisationSecondContactNamePage,
        NormalMode,
        updatedAnswers
      ) mustBe controllers.organisation.routes.OrganisationSecondContactEmailController.onPageLoad(NormalMode)

    }

    "must navigate from OrganisationHaveSecondContact page to Journey Recovery when no answer exists" in {
      val userAnswers = emptyUserAnswers

      navigator.nextPage(
        OrganisationHaveSecondContactPage,
        NormalMode,
        userAnswers
      ) mustBe routes.JourneyRecoveryController.onPageLoad()
    }

    "OrganisationSecondContactHavePhone navigation" - {
      "must go to Organisation Second Contact phone Number page when user answers 'Yes'" in {
        val userAnswers = emptyUserAnswers.withPage(OrganisationSecondContactHavePhonePage, true)
        navigator.nextPage(
          OrganisationSecondContactHavePhonePage,
          NormalMode,
          userAnswers
        ) mustBe controllers.organisation.routes.OrganisationSecondContactPhoneNumberController.onPageLoad(NormalMode)
      }

      "must go to Check Your Answers page when user answers 'No'" in {
        val userAnswers = emptyUserAnswers.withPage(OrganisationSecondContactHavePhonePage, false)
        navigator.nextPage(
          OrganisationSecondContactHavePhonePage,
          NormalMode,
          userAnswers
        ) mustBe routes.CheckYourAnswersController.onPageLoad()
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
