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

package services

import cats.syntax.all.*
import models.OrganisationOrIndividual.{Individual, Organisation}
import models.audit.*
import models.errors.ApiError.InternalServerError
import models.errors.CarfError
import models.viewAndUpdateRcasp.{IndividualRcaspDetails, OrganisationRcaspDetails}
import models.*
import pages.*
import pages.changeDetails.ChangeRcaspCachedDetails
import pages.combined.OrganisationOrIndividualPage
import pages.individual.*
import pages.organisation.*
import play.api.libs.json.{JsValue, Json}
import types.ResultT
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.http.connector.AuditResult.*
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import utils.LoggerUtil.*

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.language.postfixOps
import scala.util.control.NonFatal

class AuditService @Inject (auditConnector: AuditConnector)(using ec: ExecutionContext) {

  def auditAddRcasp(
      userAnswers: UserAnswers
  )(implicit hc: HeaderCarrier): ResultT[Unit] =
    for {
      addRcaspEvent <- ResultT.fromValue(
                         userAnswers.get(OrganisationOrIndividualPage) match {
                           case Some(Individual)   =>
                             AddRcaspAuditEvent(
                               organisationCTMatch = getOrganisationCtMatch(userAnswers),
                               isRCASPAnOrganisationOrIndividual = userAnswers.get(OrganisationOrIndividualPage),
                               addRCASPIndividual = getAddRcaspIndividual(userAnswers),
                               addRCASPOrganisation = None,
                               addressLookup = getAddressLookup(userAnswers),
                               individualContactDetails = getIndividualContactDetails(userAnswers),
                               organisationContactDetails = None
                             )
                           case Some(Organisation) =>
                             AddRcaspAuditEvent(
                               organisationCTMatch = getOrganisationCtMatch(userAnswers),
                               isRCASPAnOrganisationOrIndividual = Some(Organisation),
                               addRCASPIndividual = None,
                               addRCASPOrganisation = getAddRcaspOrganisation(userAnswers),
                               addressLookup = getAddressLookup(userAnswers),
                               individualContactDetails = None,
                               organisationContactDetails = getOrganisationContactDetails(userAnswers)
                             )
                           case None               =>
                             AddRcaspAuditEvent(
                               organisationCTMatch = getOrganisationCtMatch(userAnswers),
                               isRCASPAnOrganisationOrIndividual = None,
                               addRCASPIndividual = None,
                               addRCASPOrganisation = getAddRcaspOrganisation(userAnswers),
                               addressLookup =
                                 if (userAnswers.get(RegisteredBusinessIsTheAddressCorrectPage).contains(true)) { None }
                                 else getAddressLookup(userAnswers),
                               individualContactDetails = None,
                               organisationContactDetails = None
                             )
                         }
                       )
      extendedEvent  = convertToExtendedEvent(Json.toJson(addRcaspEvent), "AddRCASP")
      _             <- sendEvent(extendedEvent, "Add")
    } yield ()

  def auditChangeRcasp(
      userAnswers: UserAnswers
  )(implicit hc: HeaderCarrier): ResultT[Unit] =
    for {
      changeRcaspEvent <-
        (
          userAnswers.get(ReportForRegisteredBusinessPage),
          userAnswers.get(ChangeRcaspCachedDetails).map(_.IsRCASPUser)
        ) match {
          case (Some(true), Some(true))   =>
            ResultT.fromValue(
              ChangeRcaspAuditEvent(
                changeRCASPIsUserUpdatedValues = getChangeRcaspUserUpdated(userAnswers),
                changeRCASPIsUserOriginalValues = getChangeRcaspUserOriginal(userAnswers),
                changeRCASPisNotUserUpdatedValues = None,
                changeRCASPisNotUserOriginalValues = None
              )
            )
          case (Some(false), Some(true))  =>
            ResultT.fromValue(
              ChangeRcaspAuditEvent(
                changeRCASPIsUserUpdatedValues = None,
                changeRCASPIsUserOriginalValues = getChangeRcaspUserOriginal(userAnswers),
                changeRCASPisNotUserUpdatedValues = getChangeRcaspNotUserUpdated(userAnswers),
                changeRCASPisNotUserOriginalValues = None
              )
            )
          case (Some(false), Some(false)) =>
            ResultT.fromValue(
              ChangeRcaspAuditEvent(
                changeRCASPIsUserUpdatedValues = None,
                changeRCASPIsUserOriginalValues = None,
                changeRCASPisNotUserUpdatedValues = getChangeRcaspNotUserUpdated(userAnswers),
                changeRCASPisNotUserOriginalValues = getChangeRcaspNotUserOriginal(userAnswers)
              )
            )
          case _                          => ResultT.fromError(InternalServerError)
        }

      extendedEvent = convertToExtendedEvent(Json.toJson(changeRcaspEvent), "ChangeRCASP")
      _            <- sendEvent(extendedEvent, "Change")
    } yield ()

  def auditRemoveRcasp(
      removeUserAccess: Boolean,
      removeOtherAccess: Boolean,
      removeRcasp: Boolean
  )(implicit hc: HeaderCarrier): ResultT[Unit] =
    for {
      removeRcaspEvent <- ResultT.fromValue(RemoveRcaspAuditEvent(removeUserAccess, removeOtherAccess, removeRcasp))
      extendedEvent     = convertToExtendedEvent(Json.toJson(removeRcaspEvent), "RemoveRCASP")
      _                <- sendEvent(extendedEvent, "Remove")
    } yield ()

  private def convertToExtendedEvent(eventJsValue: JsValue, auditType: String) =
    ExtendedDataEvent(
      auditSource = "carf-management-frontend",
      auditType = auditType,
      detail = eventJsValue
    )

  private def sendEvent(extendedEvent: ExtendedDataEvent, eventType: String)(implicit
      hc: HeaderCarrier
  ): ResultT[Unit] =
    ResultT.fromFuture(auditConnector.sendExtendedEvent(extendedEvent).map {
      case Success         =>
        logDebug(s"Successfully sent Management audit event for $eventType RCASP")
        Right[CarfError, Unit](())
      case Disabled        =>
        logError(s"Failed to audit Management for $eventType RCASP - Disabled result returned")
        Left[CarfError, Unit](InternalServerError)
      case Failure(msg, _) =>
        logError(s"Failed to audit Management for $eventType RCASP with message $msg")
        Left[CarfError, Unit](InternalServerError)
    } recover {
      case e if NonFatal(e) =>
        logError(s"Failed to audit Management for $eventType RCASP")
        Left[CarfError, Unit](InternalServerError)
    })

  private def getOrganisationCtMatch(userAnswers: UserAnswers): Option[OrganisationCtMatch] =
    userAnswers.get(ReportForRegisteredBusinessPage).map { reportForRegisteredBusiness =>
      OrganisationCtMatch(
        reportForRegisteredBusiness,
        userAnswers.get(RegisteredBusinessIsThisYourBusinessNamePage)
      )
    }

  private def getAddRcaspIndividual(userAnswers: UserAnswers): Option[AddRcaspIndividual] =
    (
      userAnswers.get(IndividualNamePage),
      userAnswers.get(NiNumberPage)
    ).mapN { (name, nino) =>
      AddRcaspIndividual(
        name.firstName,
        name.lastName,
        nino
      )
    }

  private def getAddRcaspOrganisation(userAnswers: UserAnswers): Option[AddRcaspOrganisation] =
    userAnswers
      .get(HaveTradingNamePage)
      .map { haveTradingName =>
        AddRcaspOrganisation(
          if (userAnswers.get(RegisteredBusinessIsThisYourBusinessNamePage).contains(true))
            None
          else userAnswers.get(OrganisationNamePage),
          haveTradingName,
          userAnswers.get(TradingNamePage),
          userAnswers.get(UtrPage),
          userAnswers.get(RegisteredBusinessIsTheAddressCorrectPage)
        )
      }

  private def getAddressLookup(userAnswers: UserAnswers): Option[AddressLookup] =
    userAnswers.get(UkAddressInUserAnswers).map { ukAddress =>
      AddressLookup(
        userAnswers.get(FindAddressPage).map(_.postcode),
        userAnswers.get(FindAddressPage).flatMap(_.propertyNameOrNumber),
        userAnswers.get(AddressUPRNUserAnswers),
        userAnswers.get(ChooseAddressPage),
        ukAddress.addressLine1,
        ukAddress.addressLine2,
        ukAddress.addressLine3,
        ukAddress.townOrCity,
        ukAddress.postCode
      )
    }

  private def getIndividualContactDetails(userAnswers: UserAnswers): Option[IndividualContactDetails] =
    (userAnswers.get(IndividualEmailPage), userAnswers.get(IndividualHavePhonePage)).mapN { (email, havePhone) =>
      IndividualContactDetails(
        email,
        havePhone,
        if (havePhone) { userAnswers.get(IndividualPhonePage) }
        else None
      )
    }

  private def getOrganisationContactDetails(userAnswers: UserAnswers): Option[OrganisationContactDetails] =
    (
      userAnswers.get(OrganisationFirstContactNamePage),
      userAnswers.get(OrganisationFirstContactEmailPage),
      userAnswers.get(OrganisationFirstContactHavePhonePage),
      userAnswers.get(OrganisationHaveSecondContactPage)
    ).mapN { (name, email, havePhone, secondContact) =>
      OrganisationContactDetails(
        name,
        email,
        havePhone,
        userAnswers.get(OrganisationFirstContactPhoneNumberPage),
        secondContact,
        userAnswers.get(OrganisationSecondContactNamePage),
        userAnswers.get(OrganisationSecondContactEmailPage),
        userAnswers.get(OrganisationSecondContactHavePhonePage),
        userAnswers.get(OrganisationSecondContactPhoneNumberPage)
      )
    }

  private def getChangeRcaspUserUpdated(userAnswers: UserAnswers): Option[ChangeRcaspIsUserValues] =
    (
      userAnswers.get(ReportForRegisteredBusinessPage),
      userAnswers.get(OverwritableOrganisationName),
      userAnswers.get(HaveTradingNamePage),
      userAnswers.get(UkAddressInUserAnswers)
    ).mapN { (isRcasp, orgName, haveTrading, address) =>
      ChangeRcaspIsUserValues(
        isRcasp,
        orgName,
        haveTrading,
        userAnswers.get(TradingNamePage),
        address.formatAddress
      )

    }

  private def getChangeRcaspUserOriginal(userAnswers: UserAnswers): Option[ChangeRcaspIsUserValues] =
    userAnswers
      .get(ChangeRcaspCachedDetails)
      .collect { case organisation: OrganisationRcaspDetails =>
        ChangeRcaspIsUserValues(
          isBusinessAnRCASP = organisation.IsRCASPUser,
          organisationName = organisation.RCASPName,
          doesRCASPTradeUnderDifferentName = organisation.TradingName != organisation.RCASPName,
          RCASPTradingName = if (organisation.TradingName != organisation.RCASPName) {
            Some(organisation.TradingName)
          } else None,
          RCASPAddress = organisation.AddressDetails.formatRcaspAddress
        )
      }

  private def getChangeRcaspNotUserUpdated(userAnswers: UserAnswers): Option[ChangeRcaspIsNotUserValues] =
    (
      userAnswers.get(ReportForRegisteredBusinessPage),
      userAnswers.get(OrganisationOrIndividualPage),
      userAnswers.get(UkAddressInUserAnswers)
    ).mapN { (isRcasp, orgOrInd, address) =>
      orgOrInd match {
        case OrganisationOrIndividual.Individual   =>
          ChangeRcaspIsNotUserValues(
            isBusinessAnRCASP = isRcasp,
            isRCASPAnOrganisationOrIndividual = orgOrInd.toString,
            organisationName = None,
            doesRCASPTradeUnderDifferentName = None,
            RCASPTradeName = None,
            RCASPUTR = None,
            IndividualRCASPFirstName = userAnswers.get(IndividualNamePage).map(_.firstName),
            IndividualRCASPLastName = userAnswers.get(IndividualNamePage).map(_.lastName),
            IndividualRCASPNino = userAnswers.get(NiNumberPage),
            RCASPAddress = address.formatAddress,
            Contact1Name = None,
            Contact1EmailAddress = None,
            Contact1ContactByPhone = None,
            Contact1PhoneNumber = None,
            Contact2 = None,
            Contact2Name = None,
            Contact2EmailAddress = None,
            Contact2ContactByPhone = None,
            Contact2PhoneNumber = None,
            individualEmailAddress = userAnswers.get(IndividualEmailPage),
            individualContactByPhone = userAnswers.get(IndividualHavePhonePage),
            individuaPhoneNumber = userAnswers.get(IndividualPhonePage)
          )
        case OrganisationOrIndividual.Organisation =>
          ChangeRcaspIsNotUserValues(
            isBusinessAnRCASP = isRcasp,
            isRCASPAnOrganisationOrIndividual = orgOrInd.toString,
            organisationName = userAnswers.get(OrganisationNamePage),
            doesRCASPTradeUnderDifferentName = userAnswers.get(HaveTradingNamePage),
            RCASPTradeName = userAnswers.get(TradingNamePage),
            RCASPUTR = userAnswers.get(UtrPage),
            IndividualRCASPFirstName = None,
            IndividualRCASPLastName = None,
            IndividualRCASPNino = None,
            RCASPAddress = address.formatAddress,
            Contact1Name = userAnswers.get(OrganisationFirstContactNamePage),
            Contact1EmailAddress = userAnswers.get(OrganisationFirstContactEmailPage),
            Contact1ContactByPhone = userAnswers.get(OrganisationFirstContactHavePhonePage),
            Contact1PhoneNumber = userAnswers.get(OrganisationFirstContactPhoneNumberPage),
            Contact2 = userAnswers.get(OrganisationHaveSecondContactPage),
            Contact2Name = userAnswers.get(OrganisationSecondContactNamePage),
            Contact2EmailAddress = userAnswers.get(OrganisationSecondContactEmailPage),
            Contact2ContactByPhone = userAnswers.get(OrganisationSecondContactHavePhonePage),
            Contact2PhoneNumber = userAnswers.get(OrganisationSecondContactPhoneNumberPage),
            individualEmailAddress = None,
            individualContactByPhone = None,
            individuaPhoneNumber = None
          )
      }

    }

  private def getChangeRcaspNotUserOriginal(userAnswers: UserAnswers): Option[ChangeRcaspIsNotUserValues] =
    userAnswers
      .get(ChangeRcaspCachedDetails)
      .map {
        case cachedDetails @ (individual: IndividualRcaspDetails) =>
          ChangeRcaspIsNotUserValues(
            isBusinessAnRCASP = cachedDetails.IsRCASPUser,
            isRCASPAnOrganisationOrIndividual = cachedDetails.PartyType,
            organisationName = None,
            doesRCASPTradeUnderDifferentName = None,
            RCASPTradeName = None,
            RCASPUTR = None,
            IndividualRCASPFirstName = Some(individual.FirstName),
            IndividualRCASPLastName = Some(individual.LastName),
            IndividualRCASPNino = individual.TINDetails.flatMap(_.headOption.map(_.TIN)),
            RCASPAddress = individual.AddressDetails.formatRcaspAddress,
            Contact1Name = None,
            Contact1EmailAddress = None,
            Contact1ContactByPhone = None,
            Contact1PhoneNumber = None,
            Contact2 = None,
            Contact2Name = None,
            Contact2EmailAddress = None,
            Contact2ContactByPhone = None,
            Contact2PhoneNumber = None,
            individualEmailAddress = individual.PrimaryContactDetails.map(_.EmailAddress),
            individualContactByPhone = Some(individual.PrimaryContactDetails.flatMap(_.PhoneNumber).isDefined),
            individuaPhoneNumber = individual.PrimaryContactDetails.flatMap(_.PhoneNumber)
          )

        case cachedDetails @ (organisation: OrganisationRcaspDetails) =>
          ChangeRcaspIsNotUserValues(
            isBusinessAnRCASP = cachedDetails.IsRCASPUser,
            isRCASPAnOrganisationOrIndividual = cachedDetails.PartyType,
            organisationName = Some(organisation.RCASPName),
            doesRCASPTradeUnderDifferentName = Some(organisation.TradingName != organisation.RCASPName),
            RCASPTradeName = if (organisation.TradingName != organisation.RCASPName) { Some(organisation.TradingName) }
            else None,
            RCASPUTR = organisation.TINDetails.flatMap(_.headOption.map(_.TIN)),
            IndividualRCASPFirstName = None,
            IndividualRCASPLastName = None,
            IndividualRCASPNino = None,
            RCASPAddress = organisation.AddressDetails.formatRcaspAddress,
            Contact1Name = organisation.PrimaryContactDetails.map(_.ContactName),
            Contact1EmailAddress = organisation.PrimaryContactDetails.map(_.EmailAddress),
            Contact1ContactByPhone = Some(organisation.PrimaryContactDetails.flatMap(_.PhoneNumber).isDefined),
            Contact1PhoneNumber = organisation.PrimaryContactDetails.flatMap(_.PhoneNumber),
            Contact2 = Some(organisation.SecondaryContactDetails.isDefined),
            Contact2Name = organisation.SecondaryContactDetails.map(_.ContactName),
            Contact2EmailAddress = organisation.SecondaryContactDetails.map(_.EmailAddress),
            Contact2ContactByPhone = if (organisation.SecondaryContactDetails.isDefined) {
              Some(organisation.SecondaryContactDetails.flatMap(_.PhoneNumber).isDefined)
            } else None,
            Contact2PhoneNumber = organisation.SecondaryContactDetails.flatMap(_.PhoneNumber),
            individualEmailAddress = None,
            individualContactByPhone = None,
            individuaPhoneNumber = None
          )
      }

}
