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

import models.{NormalMode, OrganisationOrIndividual, UserAnswers}
import pages.Page
import pages.organisation.{HaveTradingNamePage, OrganisationNamePage, RegisteredBusinessIsThisYourBusinessNamePage, ReportForRegisteredBusinessPage, TradingNamePage}
import pages.combined.OrganisationOrIndividualPage
import pages.individual.{IndividualEmailPage, IndividualHavePhonePage, IndividualNamePage, IndividualPhonePage, NiNumberPage}
import play.api.mvc.Call

trait NormalRoutesNavigator {

  val normalRoutes: Page => UserAnswers => Call = {

    case OrganisationNamePage =>
      _ => controllers.organisation.routes.HaveTradingNameController.onPageLoad(NormalMode)

    case HaveTradingNamePage =>
      userAnswers => navigateFromHaveTradingNamePage(userAnswers)

    case TradingNamePage              =>
      _ =>
        controllers.routes.PlaceholderController.onPageLoad(
          "If is RCASP user = true, nav to /is-the-address-correct, else nav to /utr (CARF-197)"
        )
    case OrganisationOrIndividualPage =>
      userAnswers => navigateFromOrganisationOrIndividualPage(userAnswers)

    case ReportForRegisteredBusinessPage =>
      userAnswers => navigateFromReportForRegisteredBusinessPage(userAnswers)

    case RegisteredBusinessIsThisYourBusinessNamePage =>
      userAnswers => navigateFromRegisteredBusinessIsThisYourBusinessNamePage(userAnswers)

    case IndividualNamePage => _ => controllers.individual.routes.NiNumberController.onPageLoad(NormalMode)

    case NiNumberPage =>
      _ => controllers.routes.PlaceholderController.onPageLoad("Should redirect to /find-address (CARF-200)")

    case IndividualEmailPage => _ => controllers.individual.routes.IndividualHavePhoneController.onPageLoad(NormalMode)

    case IndividualHavePhonePage => userAnswers => navigateFromIndividualHavePhonePage(userAnswers)

    case IndividualPhonePage =>
      _ => controllers.routes.PlaceholderController.onPageLoad("Should nav to /check-answers (CARF-540)")

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

  private def navigateFromIndividualHavePhonePage(userAnswers: UserAnswers): Call =
    userAnswers.get(IndividualHavePhonePage) match {
      case Some(true)  => controllers.individual.routes.IndividualPhoneController.onPageLoad(NormalMode)
      case Some(false) =>
        controllers.routes.PlaceholderController.onPageLoad("Should nav to /check-answers (CARF-540)")
      case None        => controllers.routes.JourneyRecoveryController.onPageLoad()
    }

  private def navigateFromReportForRegisteredBusinessPage(userAnswers: UserAnswers): Call =
    userAnswers.get(ReportForRegisteredBusinessPage) match {
      case Some(true)  =>
        controllers.organisation.routes.RegisteredBusinessIsThisYourBusinessNameController.onPageLoad(NormalMode)
      case Some(false) => controllers.combined.routes.OrganisationOrIndividualController.onPageLoad(NormalMode)
      case None        => controllers.routes.JourneyRecoveryController.onPageLoad()
    }

  private def navigateFromRegisteredBusinessIsThisYourBusinessNamePage(userAnswers: UserAnswers): Call =
    userAnswers.get(RegisteredBusinessIsThisYourBusinessNamePage) match {
      case Some(true)  =>
        controllers.organisation.routes.HaveTradingNameController.onPageLoad(NormalMode)
      case Some(false) =>
        controllers.organisation.routes.OrganisationNameController.onPageLoad(NormalMode)
      case None        => controllers.routes.JourneyRecoveryController.onPageLoad()
    }

  private def navigateFromOrganisationOrIndividualPage(userAnswers: UserAnswers): Call =
    userAnswers.get(OrganisationOrIndividualPage) match {
      case Some(OrganisationOrIndividual.Organisation) =>
        controllers.organisation.routes.OrganisationNameController.onPageLoad(NormalMode)
      case Some(OrganisationOrIndividual.Individual)   =>
        controllers.individual.routes.IndividualNameController.onPageLoad(NormalMode)
      case None                                        =>
        controllers.routes.JourneyRecoveryController.onPageLoad()
    }
}
