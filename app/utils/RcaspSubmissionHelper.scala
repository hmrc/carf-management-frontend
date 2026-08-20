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

import config.Constants.{individualPartyType, organisationPartyType, ukCountryCode}
import models.OrganisationOrIndividual.{Individual, Organisation}
import models.requests.*
import models.requests.createRcasp.RcaspRequest as CreateRcaspRequest
import models.requests.updateRcasp.RcaspRequest as UpdateRcaspRequest
import models.{toRcaspAddress, viewAndUpdateRcasp, IdentifierType, OrganisationOrIndividual, RcaspContactDetails, TinDetails, UniqueTaxpayerReference, UserAnswers}
import pages.UkAddressInUserAnswers
import pages.changeDetails.ChangeRcaspCachedDetails
import pages.combined.OrganisationOrIndividualPage
import pages.individual.*
import pages.organisation.*

class RcaspSubmissionHelper {

  private def rcaspRequestCommon(requestType: RequestType): RcaspRequestCommon =
    RcaspRequestCommon(
      OriginatingSystem = "MDTP",
      TransmittingSystem = "EIS",
      RequestType = requestType.name,
      Regime = "CARF",
      RequestParameters = None
    )

  def createRegisteredBusinessRcaspRequest(
      carfId: String,
      utr: UniqueTaxpayerReference,
      userAnswers: UserAnswers
  ): Option[CreateRcaspRequest] =
    for {
      isRcaspUser     <- userAnswers.get(ReportForRegisteredBusinessPage)
      if isRcaspUser
      orgName         <- userAnswers.get(OverwritableOrganisationName)
      haveTradingName <- userAnswers.get(HaveTradingNamePage)
      tradingName     <- if (haveTradingName) userAnswers.get(TradingNamePage) else Some(orgName)
      address         <- userAnswers.get(UkAddressInUserAnswers).map(_.toRcaspAddress)
    } yield CreateRcaspRequest(
      RCASPManagement = createRcasp.RcaspManagementRequest(
        RequestCommon = rcaspRequestCommon(RequestType.Create),
        RequestDetails = createRcasp.OrganisationRcaspDetails(
          SubscriptionID = carfId,
          IsRCASPUser = isRcaspUser,
          PartyType = organisationPartyType,
          RCASPName = orgName,
          TradingName = tradingName,
          TINDetails = Some(
            List(
              TinDetails(
                TINType = IdentifierType.UTR,
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

  def updateRegisteredBusinessRcaspRequest(
      carfId: String,
      utr: UniqueTaxpayerReference,
      userAnswers: UserAnswers
  ): Option[UpdateRcaspRequest] =
    for {
      isRcaspUser     <- userAnswers.get(ReportForRegisteredBusinessPage)
      if isRcaspUser
      rcaspId         <- userAnswers.get(ChangeRcaspCachedDetails).map(_.RCASPID)
      orgName         <- userAnswers.get(OverwritableOrganisationName)
      haveTradingName <- userAnswers.get(HaveTradingNamePage)
      tradingName     <- if (haveTradingName) userAnswers.get(TradingNamePage) else Some(orgName)
      address         <- userAnswers.get(UkAddressInUserAnswers).map(_.toRcaspAddress)
    } yield UpdateRcaspRequest(
      RCASPManagement = updateRcasp.RcaspManagementRequest(
        RequestCommon = rcaspRequestCommon(RequestType.Update),
        RequestDetails = viewAndUpdateRcasp.OrganisationRcaspDetails(
          RCASPID = rcaspId,
          SubscriptionID = carfId,
          IsRCASPUser = isRcaspUser,
          PartyType = organisationPartyType,
          RCASPName = orgName,
          TradingName = tradingName,
          TINDetails = Some(
            List(
              TinDetails(
                TINType = IdentifierType.UTR,
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
      RCASPManagement = createRcasp.RcaspManagementRequest(
        RequestCommon = rcaspRequestCommon(RequestType.Create),
        RequestDetails = rcaspDetails
      )
    )

  def updateRcaspRequest(carfId: String, userAnswers: UserAnswers): Option[UpdateRcaspRequest] =
    for {
      organisationOrIndividual <- userAnswers.get(OrganisationOrIndividualPage)
      rcaspDetails             <- updateRcaspDetails(carfId, userAnswers, organisationOrIndividual)
    } yield UpdateRcaspRequest(
      RCASPManagement = updateRcasp.RcaspManagementRequest(
        RequestCommon = rcaspRequestCommon(RequestType.Update),
        RequestDetails = rcaspDetails
      )
    )

  private def createRcaspDetails(
      carfId: String,
      userAnswers: UserAnswers,
      organisationOrIndividual: OrganisationOrIndividual
  ): Option[createRcasp.RcaspDetails] =
    organisationOrIndividual match {
      case Individual   => createIndividualRcaspDetails(carfId: String, userAnswers: UserAnswers)
      case Organisation => createOrganisationRcaspDetails(carfId: String, userAnswers: UserAnswers)
    }

  private def updateRcaspDetails(
      carfId: String,
      userAnswers: UserAnswers,
      organisationOrIndividual: OrganisationOrIndividual
  ): Option[viewAndUpdateRcasp.RcaspDetails] =
    organisationOrIndividual match {
      case Individual   => updateIndividualRcaspDetails(carfId: String, userAnswers: UserAnswers)
      case Organisation => updateOrganisationRcaspDetails(carfId: String, userAnswers: UserAnswers)
    }

  private def createIndividualRcaspDetails(
      carfId: String,
      userAnswers: UserAnswers
  ): Option[createRcasp.IndividualRcaspDetails] =
    for {
      name             <- userAnswers.get(IndividualNamePage)
      nino             <- userAnswers.get(NiNumberPage)
      contactDetails   <- buildIndividualContactDetails(userAnswers)
      address          <- userAnswers.get(UkAddressInUserAnswers).map(_.toRcaspAddress)
      isRcaspUserAnswer = userAnswers.get(ReportForRegisteredBusinessPage).contains(true)
      isRcaspUser      <- if (isRcaspUserAnswer) None else Some(isRcaspUserAnswer)
    } yield createRcasp.IndividualRcaspDetails(
      SubscriptionID = carfId,
      IsRCASPUser = isRcaspUser,
      PartyType = individualPartyType,
      FirstName = name.firstName,
      LastName = name.lastName,
      TINDetails = Some(
        List(
          TinDetails(
            TINType = IdentifierType.NINO,
            TIN = nino,
            IssuedBy = ukCountryCode
          )
        )
      ),
      AddressDetails = address,
      PrimaryContactDetails = Some(contactDetails)
    )

  private def updateIndividualRcaspDetails(
      carfId: String,
      userAnswers: UserAnswers
  ): Option[viewAndUpdateRcasp.IndividualRcaspDetails] =
    for {
      isRcaspUser    <- userAnswers.get(ReportForRegisteredBusinessPage)
      if !isRcaspUser
      rcaspId        <- userAnswers.get(ChangeRcaspCachedDetails).map(_.RCASPID)
      name           <- userAnswers.get(IndividualNamePage)
      nino           <- userAnswers.get(NiNumberPage)
      contactDetails <- buildIndividualContactDetails(userAnswers)
      address        <- userAnswers.get(UkAddressInUserAnswers).map(_.toRcaspAddress)
    } yield viewAndUpdateRcasp.IndividualRcaspDetails(
      RCASPID = rcaspId,
      SubscriptionID = carfId,
      IsRCASPUser = isRcaspUser,
      PartyType = individualPartyType,
      FirstName = name.firstName,
      LastName = name.lastName,
      TINDetails = Some(
        List(
          TinDetails(
            TINType = IdentifierType.NINO,
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
  ): Option[createRcasp.OrganisationRcaspDetails] =
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
    } yield createRcasp.OrganisationRcaspDetails(
      SubscriptionID = carfId,
      IsRCASPUser = isRcaspUser,
      PartyType = organisationPartyType,
      RCASPName = orgName,
      TradingName = tradingName,
      TINDetails = Some(
        List(
          TinDetails(
            TINType = IdentifierType.UTR,
            TIN = utr,
            IssuedBy = ukCountryCode
          )
        )
      ),
      AddressDetails = address,
      PrimaryContactDetails = Some(firstContact),
      SecondaryContactDetails = secondContact
    )

  private def updateOrganisationRcaspDetails(
      carfId: String,
      userAnswers: UserAnswers
  ): Option[viewAndUpdateRcasp.OrganisationRcaspDetails] =
    for {
      isRcaspUser       <- userAnswers.get(ReportForRegisteredBusinessPage)
      if !isRcaspUser
      rcaspId           <- userAnswers.get(ChangeRcaspCachedDetails).map(_.RCASPID)
      orgName           <- userAnswers.get(OverwritableOrganisationName)
      haveTradingName   <- userAnswers.get(HaveTradingNamePage)
      tradingName       <- if (haveTradingName) userAnswers.get(TradingNamePage) else Some(orgName)
      utr               <- userAnswers.get(UtrPage)
      firstContact      <- buildOrgFirstContactDetails(userAnswers)
      haveSecondContact <- userAnswers.get(OrganisationHaveSecondContactPage)
      secondContact     <- if (haveSecondContact) buildOrgSecondContactDetails(userAnswers).map(Some(_)) else Some(None)
      address           <- userAnswers.get(UkAddressInUserAnswers).map(_.toRcaspAddress)
    } yield viewAndUpdateRcasp.OrganisationRcaspDetails(
      RCASPID = rcaspId,
      SubscriptionID = carfId,
      IsRCASPUser = isRcaspUser,
      PartyType = organisationPartyType,
      RCASPName = orgName,
      TradingName = tradingName,
      TINDetails = Some(
        List(
          TinDetails(
            TINType = IdentifierType.UTR,
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
