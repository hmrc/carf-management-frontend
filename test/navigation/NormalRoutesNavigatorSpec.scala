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
import models.NormalMode
import pages.Page
import pages.individual.{IndividualNamePage, NiNumberPage}
import pages.organisation.{HaveTradingNamePage, OrganisationNamePage, TradingNamePage}

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
