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

import controllers.routes
import models.{NormalMode, OrganisationOrIndividual, UserAnswers}
import pages.Page
import pages.*
import pages.combined.OrganisationOrIndividualPage
import pages.individual.*
import pages.organisation.*
import play.api.mvc.Call
import config.Constants

trait NormalRoutesNavigator {

  val normalRoutes: Page => UserAnswers => Call = {

    case OrganisationNamePage =>
      _ => controllers.organisation.routes.HaveTradingNameController.onPageLoad(NormalMode)

    case HaveTradingNamePage =>
      userAnswers => navigateFromHaveTradingNamePage(userAnswers)

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

    case _ => _ => controllers.routes.JourneyRecoveryController.onPageLoad()
  }

  private def navigateFromHaveTradingNamePage(userAnswers: UserAnswers): Call =
    userAnswers.get(HaveTradingNamePage) match {
      case Some(true) => controllers.organisation.routes.TradingNameController.onPageLoad(NormalMode)
      case _          => controllers.routes.JourneyRecoveryController.onPageLoad()
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

  private def navigateFromOrganisationOrIndividualPage(userAnswers: UserAnswers): Call            =
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
            if (businessDetails.address.countryCode.toUpperCase == Constants.ukCountryCode) {
              controllers.routes.PlaceholderController
                .onPageLoad("Should nav to /registered-business/check-answers (CARF-294)")
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
        controllers.routes.PlaceholderController.onPageLoad("Should nav to /choose-address (CARF-201)")
      case (None, Some(address))   =>
        controllers.routes.PlaceholderController.onPageLoad("Should nav to /review-address (CARF-201)")
      case _                       =>
        controllers.routes.JourneyRecoveryController.onPageLoad()
    }
}
