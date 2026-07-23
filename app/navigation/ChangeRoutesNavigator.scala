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
import models.{ChangeMode, UserAnswers}
import pages.*
import pages.changeDetails.ChangeRcaspCachedDetails
import pages.individual.*
import pages.organisation.*
import play.api.mvc.Call

trait ChangeRoutesNavigator {

  private lazy val recovery: Call = routes.JourneyRecoveryController.onPageLoad()

  val changeRoutes: Page => UserAnswers => Call = {
    case FindAddressPage                          => userAnswers => navigateFromFindAddressPage(userAnswers)
    case ReviewAddressPageForNavigatorOnly        => userAnswers => changeDetailsNavigation(userAnswers)
    case ChooseAddressPage                        => userAnswers => navigateFromChooseAddressPage(userAnswers)
    case AddressPageForNavigatorOnly              => userAnswers => changeDetailsNavigation(userAnswers)
    case IndividualNamePage                       => userAnswers => changeDetailsNavigation(userAnswers)
    case NiNumberPage                             =>
      userAnswers => changeDetailsNavigation(userAnswers)
    case IndividualEmailPage                      => userAnswers => changeDetailsNavigation(userAnswers)
    case IndividualPhonePage                      => userAnswers => changeDetailsNavigation(userAnswers)
    case OrganisationNamePage                     => userAnswers => changeDetailsNavigation(userAnswers)
    case TradingNamePage                          => userAnswers => changeDetailsNavigation(userAnswers)
    case UtrPage                                  => userAnswers => changeDetailsNavigation(userAnswers)
    case OrganisationFirstContactNamePage         => userAnswers => changeDetailsNavigation(userAnswers)
    case OrganisationFirstContactEmailPage        => userAnswers => changeDetailsNavigation(userAnswers)
    case OrganisationFirstContactPhoneNumberPage  => userAnswers => changeDetailsNavigation(userAnswers)
    case OrganisationSecondContactNamePage        => userAnswers => changeDetailsNavigation(userAnswers)
    case OrganisationSecondContactEmailPage       => userAnswers => changeDetailsNavigation(userAnswers)
    case OrganisationSecondContactHavePhonePage   =>
      userAnswers => navigateFromOrganisationSecondContactHavePhonePage(userAnswers)
    case OrganisationSecondContactPhoneNumberPage => userAnswers => changeDetailsNavigation(userAnswers)

    case RegisteredBusinessIsThisYourBusinessNamePage =>
      userAnswers => navigateFromRegisteredBusinessIsThisYourBusinessNamePage(userAnswers)
    case RegisteredBusinessIsTheAddressCorrectPage    =>
      userAnswers => navigateFromRegisteredBusinessIsTheAddressCorrectPage(userAnswers)

    case _ => _ => recovery
  }

  private def navigateFromFindAddressPage(userAnswers: UserAnswers): Call =
    (userAnswers.get(AddressLookupResult), userAnswers.get(AddressPagePrePop)) match {
      case (Some(addresses), None) =>
        controllers.routes.ChooseAddressController.onPageLoad(ChangeMode)
      case (None, Some(address))   =>
        controllers.routes.ReviewAddressController.onPageLoad(ChangeMode)
      case _                       => recovery
    }

  private def navigateFromChooseAddressPage(userAnswers: UserAnswers): Call =
    userAnswers
      .get(ChooseAddressPage)
      .fold {
        routes.JourneyRecoveryController.onPageLoad()
      } { answer =>
        if answer == noneOfTheseValue then controllers.routes.AddressController.onPageLoad(ChangeMode)
        else changeDetailsNavigation(userAnswers)
      }

  private def changeDetailsNavigation(userAnswers: UserAnswers): Call = {
    val maybeRcaspId = userAnswers.get(ChangeRcaspCachedDetails).map(_.RCASPID)

    maybeRcaspId.fold(recovery) { rcaspId =>
      controllers.changeDetails.routes.ChangeDetailsRoutingController.onPageLoad(rcaspId)
    }
  }

  private def navigateFromOrganisationSecondContactHavePhonePage(userAnswers: UserAnswers): Call =
    userAnswers.get(OrganisationSecondContactHavePhonePage) match {
      case Some(true)  =>
        controllers.organisation.routes.OrganisationSecondContactPhoneNumberController.onPageLoad(ChangeMode)
      case Some(false) =>
        changeDetailsNavigation(userAnswers)
      case None        =>
        routes.JourneyRecoveryController.onPageLoad()
    }

  private def navigateFromRegisteredBusinessIsThisYourBusinessNamePage(userAnswers: UserAnswers): Call =
    userAnswers.get(RegisteredBusinessIsThisYourBusinessNamePage) match {
      case Some(true)  =>
        changeDetailsNavigation(userAnswers)
      case Some(false) =>
        controllers.organisation.routes.OrganisationNameController.onPageLoad(ChangeMode)
      case None        => controllers.routes.JourneyRecoveryController.onPageLoad()
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
              changeDetailsNavigation(userAnswers)
            } else {
              controllers.organisation.routes.NotInUkController.onPageLoad()
            }
          }
      case Some(false) =>
        controllers.routes.FindAddressController.onPageLoad(ChangeMode)
      case None        =>
        controllers.routes.JourneyRecoveryController.onPageLoad()
    }
}
