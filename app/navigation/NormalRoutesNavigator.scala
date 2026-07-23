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

import config.Constants
import config.Constants.noneOfTheseValue
import controllers.routes
import models.OrganisationOrIndividual.{Individual, Organisation}
import models.{NormalMode, OrganisationOrIndividual, UserAnswers}
import pages.*
import pages.combined.OrganisationOrIndividualPage
import pages.individual.*
import pages.organisation.*
import play.api.mvc.Call

trait NormalRoutesNavigator {

  val normalRoutes: Page => UserAnswers => Call = {

    case OrganisationNamePage =>
      _ => controllers.organisation.routes.HaveTradingNameController.onPageLoad(NormalMode)

    case HaveTradingNamePage =>
      userAnswers => navigateFromHaveTradingNamePage(userAnswers)

    case TradingNamePage => userAnswers => tradingNamePagesRegisteredBusinessRedirects(userAnswers)

    case UtrPage =>
      _ => controllers.routes.FindAddressController.onPageLoad(NormalMode)

    case RegisteredBusinessIsTheAddressCorrectPage =>
      userAnswers => navigateFromRegisteredBusinessIsTheAddressCorrectPage(userAnswers)

    case OrganisationOrIndividualPage =>
      userAnswers => navigateFromOrganisationOrIndividualPage(userAnswers)

    case ReportForRegisteredBusinessPage =>
      userAnswers => navigateFromReportForRegisteredBusinessPage(userAnswers)

    case RegisteredBusinessIsThisYourBusinessNamePage =>
      userAnswers => navigateFromRegisteredBusinessIsThisYourBusinessNamePage(userAnswers)

    case IndividualNamePage => _ => controllers.individual.routes.NiNumberController.onPageLoad(NormalMode)

    case NiNumberPage =>
      _ => controllers.routes.FindAddressController.onPageLoad(NormalMode)

    case IndividualEmailPage => _ => controllers.individual.routes.IndividualHavePhoneController.onPageLoad(NormalMode)

    case IndividualHavePhonePage => userAnswers => navigateFromIndividualHavePhonePage(userAnswers)

    case IndividualPhonePage =>
      _ => controllers.routes.CheckDetailsController.onPageLoad

    case OrganisationFirstContactNamePage =>
      _ => controllers.organisation.routes.OrganisationFirstContactEmailController.onPageLoad(NormalMode)

    case OrganisationFirstContactEmailPage =>
      _ => controllers.organisation.routes.OrganisationFirstContactHavePhoneController.onPageLoad(NormalMode)

    case OrganisationFirstContactHavePhonePage =>
      userAnswers => navigateFromOrganisationFirstContactHavePhonePage(userAnswers)

    case OrganisationFirstContactPhoneNumberPage =>
      _ => controllers.organisation.routes.OrganisationHaveSecondContactController.onPageLoad(NormalMode)

    case OrganisationHaveSecondContactPage =>
      userAnswers => navigateFromOrganisationHaveSecondContactController(userAnswers)

    case OrganisationSecondContactNamePage =>
      _ => controllers.organisation.routes.OrganisationSecondContactEmailController.onPageLoad(NormalMode)

    case OrganisationSecondContactEmailPage =>
      _ => controllers.organisation.routes.OrganisationSecondContactHavePhoneController.onPageLoad(NormalMode)

    case OrganisationSecondContactHavePhonePage =>
      userAnswers => navigateFromOrganisationSecondContactHavePhonePage(userAnswers)

    case OrganisationSecondContactPhoneNumberPage =>
      _ => controllers.routes.CheckDetailsController.onPageLoad

    case FindAddressPage =>
      userAnswers => navigateFromFindAddressPage(userAnswers)

    case ChooseAddressPage =>
      userAnswers => navigateFromChooseAddressPage(userAnswers)

    case ReviewAddressPageForNavigatorOnly =>
      userAnswers => successfulAddressNavigation(userAnswers)

    case AddressPageForNavigatorOnly =>
      userAnswers => successfulAddressNavigation(userAnswers)

    case _ => _ => controllers.routes.JourneyRecoveryController.onPageLoad()
  }

  private def navigateFromHaveTradingNamePage(userAnswers: UserAnswers): Call =
    userAnswers.get(HaveTradingNamePage) match {
      case Some(true) => controllers.organisation.routes.TradingNameController.onPageLoad(NormalMode)
      case _          => tradingNamePagesRegisteredBusinessRedirects(userAnswers)
    }

  private def tradingNamePagesRegisteredBusinessRedirects(userAnswers: UserAnswers): Call =
    if (userAnswers.rcaspIsRegisteredBusiness) {
      controllers.organisation.routes.RegisteredBusinessIsTheAddressCorrectController.onPageLoad(NormalMode)
    } else {
      controllers.organisation.routes.UtrController.onPageLoad(NormalMode)
    }

  private def navigateFromIndividualHavePhonePage(userAnswers: UserAnswers): Call =
    userAnswers.get(IndividualHavePhonePage) match {
      case Some(true)  => controllers.individual.routes.IndividualPhoneController.onPageLoad(NormalMode)
      case Some(false) =>
        controllers.routes.CheckDetailsController.onPageLoad
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

  private def navigateFromOrganisationHaveSecondContactController(userAnswers: UserAnswers): Call =
    userAnswers.get(OrganisationHaveSecondContactPage) match {
      case Some(true)  =>
        controllers.organisation.routes.OrganisationSecondContactNameController.onPageLoad(NormalMode)
      case Some(false) =>
        controllers.routes.CheckDetailsController.onPageLoad
      case None        => routes.JourneyRecoveryController.onPageLoad()
    }

  private def navigateFromOrganisationSecondContactHavePhonePage(userAnswers: UserAnswers): Call =
    userAnswers.get(OrganisationSecondContactHavePhonePage) match {
      case Some(true)  =>
        controllers.organisation.routes.OrganisationSecondContactPhoneNumberController.onPageLoad(NormalMode)
      case Some(false) =>
        controllers.routes.CheckDetailsController.onPageLoad
      case None        =>
        routes.JourneyRecoveryController.onPageLoad()
    }

  private def navigateFromRegisteredBusinessIsTheAddressCorrectPage(userAnswers: UserAnswers): Call =
    userAnswers.get(RegisteredBusinessIsTheAddressCorrectPage) match {
      case Some(true)  =>
        userAnswers
          .get(CachedBusinessDetailsPage)
          .fold(
            controllers.routes.JourneyRecoveryController.onPageLoad()
          ) { businessDetails =>
            if (Constants.acceptedUkCountryCode.contains(businessDetails.address.countryCode.toUpperCase)) {
              controllers.organisation.routes.RegisteredBusinessCheckDetailsController.onPageLoad
            } else {
              controllers.organisation.routes.NotInUkController.onPageLoad()
            }
          }
      case Some(false) =>
        controllers.routes.FindAddressController.onPageLoad(NormalMode)
      case None        =>
        controllers.routes.JourneyRecoveryController.onPageLoad()
    }

  private def navigateFromOrganisationFirstContactHavePhonePage(userAnswers: UserAnswers): Call =
    userAnswers.get(OrganisationFirstContactHavePhonePage) match {
      case Some(true)  =>
        controllers.organisation.routes.OrganisationFirstContactPhoneNumberController.onPageLoad(NormalMode)
      case Some(false) =>
        controllers.organisation.routes.OrganisationHaveSecondContactController.onPageLoad(NormalMode)
      case None        => routes.JourneyRecoveryController.onPageLoad()
    }

  private def navigateFromFindAddressPage(userAnswers: UserAnswers): Call =
    (userAnswers.get(AddressLookupResult), userAnswers.get(AddressPagePrePop)) match {
      case (Some(addresses), None) =>
        controllers.routes.ChooseAddressController.onPageLoad(NormalMode)
      case (None, Some(address))   =>
        controllers.routes.ReviewAddressController.onPageLoad(NormalMode)
      case _                       =>
        controllers.routes.JourneyRecoveryController.onPageLoad()
    }

  private def navigateFromChooseAddressPage(userAnswers: UserAnswers): Call =
    userAnswers
      .get(ChooseAddressPage)
      .fold {
        routes.JourneyRecoveryController.onPageLoad()
      } { answer =>
        if answer == noneOfTheseValue then controllers.routes.AddressController.onPageLoad(NormalMode)
        else successfulAddressNavigation(userAnswers)
      }

  private def successfulAddressNavigation(userAnswers: UserAnswers): Call =
    if (userAnswers.rcaspIsRegisteredBusiness) {
      controllers.organisation.routes.RegisteredBusinessCheckDetailsController.onPageLoad
    } else {
      userAnswers.get(OrganisationOrIndividualPage) match {
        case Some(Individual)   =>
          controllers.individual.routes.IndividualEmailController.onPageLoad(NormalMode)
        case Some(Organisation) =>
          controllers.organisation.routes.OrganisationFirstContactNameController.onPageLoad(NormalMode)
        case None               =>
          controllers.routes.JourneyRecoveryController.onPageLoad()
      }
    }
}
