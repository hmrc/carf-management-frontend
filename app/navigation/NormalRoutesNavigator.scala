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

import models.{NormalMode, UserAnswers}
import pages.Page
import pages.individual.{IndividualEmailPage, IndividualNamePage, NiNumberPage}
import pages.organisation.{HaveTradingNamePage, OrganisationNamePage, TradingNamePage}
import play.api.mvc.Call

trait NormalRoutesNavigator {

  val normalRoutes: Page => UserAnswers => Call = {

    case OrganisationNamePage =>
      _ => controllers.organisation.routes.HaveTradingNameController.onPageLoad(NormalMode)

    case HaveTradingNamePage =>
      userAnswers => navigateFromHaveTradingNamePage(userAnswers)

    case TradingNamePage =>
      _ =>
        controllers.routes.PlaceholderController.onPageLoad(
          "If is RCASP user = true, nav to /is-the-address-correct, else nav to /utr (CARF-197)"
        )

    case IndividualNamePage => _ => controllers.individual.routes.NiNumberController.onPageLoad(NormalMode)

    case NiNumberPage =>
      _ => controllers.routes.PlaceholderController.onPageLoad("Should redirect to /find-address (CARF-200)")

    case IndividualEmailPage => _ => controllers.individual.routes.IndividualHavePhoneController.onPageLoad(NormalMode)

    case _ => _ => controllers.routes.JourneyRecoveryController.onPageLoad()
  }

  private def navigateFromHaveTradingNamePage(userAnswers: UserAnswers): Call =
    userAnswers.get(HaveTradingNamePage) match {
      case Some(true)  => controllers.organisation.routes.TradingNameController.onPageLoad(NormalMode)
      case Some(false) =>
        controllers.routes.PlaceholderController.onPageLoad(
          "If is RCASP user = true, nav to /is-the-address-correct, else nav to /utr (CARF-197)"
        )
      case None        => controllers.routes.JourneyRecoveryController.onPageLoad()
    }

}
