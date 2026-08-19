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
import pages.individual.*
import pages.organisation.*
import play.api.mvc.Call

trait ChangeRoutesNavigator {

  private lazy val recovery: Call = routes.JourneyRecoveryController.onPageLoad()

  val changeRoutes: Page => UserAnswers => Call = {
    case FindAddressPage                          => userAnswers => navigateFromFindAddressPage(userAnswers)
    case ReviewAddressPageForNavigatorOnly        =>
      userAnswers => controllers.routes.EndOfJourneyRoutingController.onPageLoad()
    case ChooseAddressPage                        => userAnswers => navigateFromChooseAddressPage(userAnswers)
    case AddressPageForNavigatorOnly              => userAnswers => controllers.routes.EndOfJourneyRoutingController.onPageLoad()
    case IndividualNamePage                       => userAnswers => controllers.routes.EndOfJourneyRoutingController.onPageLoad()
    case NiNumberPage                             =>
      userAnswers => controllers.routes.EndOfJourneyRoutingController.onPageLoad()
    case IndividualEmailPage                      => userAnswers => controllers.routes.EndOfJourneyRoutingController.onPageLoad()
    case IndividualPhonePage                      => userAnswers => controllers.routes.EndOfJourneyRoutingController.onPageLoad()
    case OrganisationNamePage                     => userAnswers => controllers.routes.EndOfJourneyRoutingController.onPageLoad()
    case TradingNamePage                          => userAnswers => controllers.routes.EndOfJourneyRoutingController.onPageLoad()
    case UtrPage                                  => userAnswers => controllers.routes.EndOfJourneyRoutingController.onPageLoad()
    case OrganisationFirstContactNamePage         =>
      userAnswers => controllers.routes.EndOfJourneyRoutingController.onPageLoad()
    case OrganisationFirstContactEmailPage        =>
      userAnswers => controllers.routes.EndOfJourneyRoutingController.onPageLoad()
    case OrganisationFirstContactPhoneNumberPage  =>
      userAnswers => controllers.routes.EndOfJourneyRoutingController.onPageLoad()
    case OrganisationSecondContactNamePage        =>
      userAnswers => controllers.routes.EndOfJourneyRoutingController.onPageLoad()
    case OrganisationSecondContactEmailPage       =>
      userAnswers => controllers.routes.EndOfJourneyRoutingController.onPageLoad()
    case OrganisationSecondContactPhoneNumberPage =>
      userAnswers => controllers.routes.EndOfJourneyRoutingController.onPageLoad()

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
      .fold(recovery) { answer =>
        if answer == noneOfTheseValue then controllers.routes.AddressController.onPageLoad(ChangeMode)
        else controllers.routes.EndOfJourneyRoutingController.onPageLoad()
      }

  private def navigateFromRegisteredBusinessIsThisYourBusinessNamePage(userAnswers: UserAnswers): Call =
    userAnswers.get(RegisteredBusinessIsThisYourBusinessNamePage) match {
      case Some(true)  => controllers.routes.EndOfJourneyRoutingController.onPageLoad()
      case Some(false) => controllers.organisation.routes.OrganisationNameController.onPageLoad(ChangeMode)
      case None        => recovery
    }

  private def navigateFromRegisteredBusinessIsTheAddressCorrectPage(userAnswers: UserAnswers): Call =
    userAnswers.get(RegisteredBusinessIsTheAddressCorrectPage) match {
      case Some(true)  =>
        userAnswers
          .get(CachedBusinessDetailsPage)
          .fold(recovery) { businessDetails =>
            if (Constants.acceptedUkCountryCode.contains(businessDetails.address.countryCode.toUpperCase)) {
              controllers.routes.EndOfJourneyRoutingController.onPageLoad()
            } else {
              controllers.organisation.routes.NotInUkController.onPageLoad()
            }
          }
      case Some(false) => controllers.routes.FindAddressController.onPageLoad(ChangeMode)
      case None        => recovery
    }
}
