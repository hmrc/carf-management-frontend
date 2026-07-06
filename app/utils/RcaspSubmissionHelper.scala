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

package utils

import config.Constants.ukCountryCode
import models.OrganisationOrIndividual.{Individual, Organisation}
import models.requests.*
import models.{toRcaspAddress, OrganisationOrIndividual, RcaspContactDetails, TinDetails, UniqueTaxpayerReference, UserAnswers}
import pages.UkAddressInUserAnswers
import pages.combined.OrganisationOrIndividualPage
import pages.individual.*
import pages.organisation.*

class RcaspSubmissionHelper {

  val rcaspCreateRequestCommon: RcaspCreateRequestCommon =
    RcaspCreateRequestCommon(
      OriginatingSystem = "MDTP",
      TransmittingSystem = "EIS",
      RequestType = "CREATE",
      Regime = "CARF",
      RequestParameters = List(RequestParameter("key", "value"))
    )

  def createRcaspRequestForRegisteredBusiness(
      carfId: String,
      utr: UniqueTaxpayerReference,
      userAnswers: UserAnswers
  ): Option[CreateRcaspRequest] =
    for {
      isRcaspUser            <- userAnswers.get(ReportForRegisteredBusinessPage)
      if isRcaspUser
      orgName                <- userAnswers.get(OverwritableOrganisationName)
      haveTradingName        <- userAnswers.get(HaveTradingNamePage)
      tradingName            <- if (haveTradingName) userAnswers.get(TradingNamePage) else Some(orgName)
      isCachedAddressCorrect <- userAnswers.get(RegisteredBusinessIsTheAddressCorrectPage)
      address                <- if (isCachedAddressCorrect) {
                                  import models.responses.toRcaspAddress
                                  userAnswers.get(CachedBusinessDetailsPage).flatMap(_.address.toRcaspAddress)
                                } else {
                                  import models.toRcaspAddress
                                  userAnswers.get(UkAddressInUserAnswers).map(_.toRcaspAddress)
                                }
    } yield CreateRcaspRequest(
      RCASPManagement = RCASPManagementRequest(
        RequestCommon = rcaspCreateRequestCommon,
        RequestDetails = OrganisationRcaspDetails(
          SubscriptionID = carfId,
          IsRCASPUser = isRcaspUser,
          PartyType = "Organisation",
          RCASPName = orgName,
          TradingName = tradingName,
          TINDetails = Some(
            List(
              TinDetails(
                TINType = "UTR",
                TIN = utr.uniqueTaxPayerReference,
                IssuedBy = ukCountryCode
              )
            )
          ),
          AddressDetails = address,
          PrimaryContactDetails = None,
          SecondaryContactDetails = None
        )
      )
    )

  def createRcaspRequest(carfId: String, userAnswers: UserAnswers): Option[CreateRcaspRequest] =
    for {
      organisationOrIndividual <- userAnswers.get(OrganisationOrIndividualPage)
      rcaspDetails             <- createRcaspDetails(carfId, userAnswers, organisationOrIndividual)
    } yield CreateRcaspRequest(
      RCASPManagement = RCASPManagementRequest(
        RequestCommon = rcaspCreateRequestCommon,
        RequestDetails = rcaspDetails
      )
    )

  private def createRcaspDetails(
      carfId: String,
      userAnswers: UserAnswers,
      organisationOrIndividual: OrganisationOrIndividual
  ): Option[RcaspDetails] =
    organisationOrIndividual match {
      case Individual   => createIndividualRcaspDetails(carfId: String, userAnswers: UserAnswers)
      case Organisation => createOrganisationRcaspDetails(carfId: String, userAnswers: UserAnswers)
    }

  private def createIndividualRcaspDetails(carfId: String, userAnswers: UserAnswers): Option[IndividualRcaspDetails] =
    for {
      name             <- userAnswers.get(IndividualNamePage)
      nino             <- userAnswers.get(NiNumberPage)
      contactDetails   <- buildIndividualContactDetails(userAnswers)
      address          <- userAnswers.get(UkAddressInUserAnswers).map(_.toRcaspAddress)
      isRcaspUserAnswer = userAnswers.get(ReportForRegisteredBusinessPage).contains(true)
      isRcaspUser      <- if (isRcaspUserAnswer) None else Some(isRcaspUserAnswer)
    } yield IndividualRcaspDetails(
      SubscriptionID = carfId,
      IsRCASPUser = isRcaspUser,
      PartyType = "Individual",
      FirstName = name.firstName,
      LastName = name.lastName,
      TINDetails = Some(
        List(
          TinDetails(
            TINType = "OTHER",
            TIN = nino,
            IssuedBy = ukCountryCode
          )
        )
      ),
      AddressDetails = address,
      PrimaryContactDetails = Some(contactDetails)
    )

  private def createOrganisationRcaspDetails(
      carfId: String,
      userAnswers: UserAnswers
  ): Option[OrganisationRcaspDetails] =
    for {
      orgName           <- userAnswers.get(OverwritableOrganisationName)
      haveTradingName   <- userAnswers.get(HaveTradingNamePage)
      tradingName       <- if (haveTradingName) userAnswers.get(TradingNamePage) else Some(orgName)
      utr               <- userAnswers.get(UtrPage)
      firstContact      <- buildOrgFirstContactDetails(userAnswers)
      haveSecondContact <- userAnswers.get(OrganisationHaveSecondContactPage)
      secondContact     <- if (haveSecondContact) buildOrgSecondContactDetails(userAnswers).map(Some(_)) else Some(None)
      address           <- userAnswers.get(UkAddressInUserAnswers).map(_.toRcaspAddress)
      isRcaspUserAnswer  = userAnswers.get(ReportForRegisteredBusinessPage).contains(true)
      isRcaspUser       <- if (isRcaspUserAnswer) None else Some(isRcaspUserAnswer)
    } yield OrganisationRcaspDetails(
      SubscriptionID = carfId,
      IsRCASPUser = isRcaspUser,
      PartyType = "Organisation",
      RCASPName = orgName,
      TradingName = tradingName,
      TINDetails = Some(
        List(
          TinDetails(
            TINType = "UTR",
            TIN = utr,
            IssuedBy = ukCountryCode
          )
        )
      ),
      AddressDetails = address,
      PrimaryContactDetails = Some(firstContact),
      SecondaryContactDetails = secondContact
    )

  private def buildIndividualContactDetails(userAnswers: UserAnswers): Option[RcaspContactDetails] =
    for {
      name      <- userAnswers.get(IndividualNamePage)
      email     <- userAnswers.get(IndividualEmailPage)
      havePhone <- userAnswers.get(IndividualHavePhonePage)
      phone     <- if (havePhone) userAnswers.get(IndividualPhonePage).map(Some(_)) else Some(None)
    } yield RcaspContactDetails(
      ContactName = name.fullName,
      EmailAddress = email,
      PhoneNumber = phone
    )

  private def buildOrgFirstContactDetails(userAnswers: UserAnswers): Option[RcaspContactDetails] =
    for {
      name      <- userAnswers.get(OrganisationFirstContactNamePage)
      email     <- userAnswers.get(OrganisationFirstContactEmailPage)
      havePhone <- userAnswers.get(OrganisationFirstContactHavePhonePage)
      phone     <- if (havePhone) userAnswers.get(OrganisationFirstContactPhoneNumberPage).map(Some(_)) else Some(None)
    } yield RcaspContactDetails(
      ContactName = name,
      EmailAddress = email,
      PhoneNumber = phone
    )

  private def buildOrgSecondContactDetails(userAnswers: UserAnswers): Option[RcaspContactDetails] =
    for {
      name      <- userAnswers.get(OrganisationSecondContactNamePage)
      email     <- userAnswers.get(OrganisationSecondContactEmailPage)
      havePhone <- userAnswers.get(OrganisationSecondContactHavePhonePage)
      phone     <- if (havePhone) userAnswers.get(OrganisationSecondContactPhoneNumberPage).map(Some(_)) else Some(None)
    } yield RcaspContactDetails(
      ContactName = name,
      EmailAddress = email,
      PhoneNumber = phone
    )
}
