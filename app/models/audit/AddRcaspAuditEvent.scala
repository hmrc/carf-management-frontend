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

package models.audit

import models.OrganisationOrIndividual
import play.api.libs.json.*

case class AddRcaspAuditEvent(
    organisationCTMatch: Option[OrganisationCtMatch],
    isRCASPAnOrganisationOrIndividual: Option[OrganisationOrIndividual],
    addRCASPIndividual: Option[AddRcaspIndividual],
    addRCASPOrganisation: Option[AddRcaspOrganisation],
    addressLookup: Option[AddressLookup],
    individualContactDetails: Option[IndividualContactDetails],
    organisationContactDetails: Option[OrganisationContactDetails]
)

object AddRcaspAuditEvent {
  implicit val format: OFormat[AddRcaspAuditEvent] = Json.format[AddRcaspAuditEvent]
}

case class OrganisationCtMatch(isBusinessAnRCASP: Boolean, isBusinessNameCorrect: Option[Boolean])

object OrganisationCtMatch {
  implicit val format: OFormat[OrganisationCtMatch] = Json.format[OrganisationCtMatch]
}

case class AddRcaspIndividual(
    individualRCASPFirstName: String,
    individualRCASPLastName: String,
    RCASPNino: String
)

object AddRcaspIndividual {
  implicit val format: OFormat[AddRcaspIndividual] = Json.format[AddRcaspIndividual]
}

case class AddRcaspOrganisation(
    organisationName: String,
    doesRCASPTradeUnderDifferentName: Boolean,
    RCASPTradingName: Option[String],
    RCASPUTR: Option[String],
    confirmRCASPregisteredAddress: Option[Boolean]
)

object AddRcaspOrganisation {
  implicit val format: OFormat[AddRcaspOrganisation] = Json.format[AddRcaspOrganisation]
}

case class AddressLookup(
    findAddress: String,
    propertyNameOrNumber: Option[String],
    UPRN: Option[Long],
    chooseAddress: Option[String],
    RCASPAddressLine1: String,
    RCASPAddressLine2: Option[String],
    RCASPAddressLine3: Option[String],
    RCASPTown: String,
    RCASPPostcode: String
)

object AddressLookup {
  implicit val format: OFormat[AddressLookup] = Json.format[AddressLookup]
}

case class IndividualContactDetails(
    individualEmailAddress: String,
    individualContactByPhone: Boolean,
    individualPhoneNumber: Option[String]
)

object IndividualContactDetails {
  implicit val format: OFormat[IndividualContactDetails] = Json.format[IndividualContactDetails]
}

case class OrganisationContactDetails(
    Contact1Name: String,
    Contact1Email: String,
    Contact1ByPhone: Boolean,
    Contact1PhoneNumber: Option[String],
    Contact2: Boolean,
    Contact2Name: Option[String],
    Contact2Email: Option[String],
    Contact2Phone: Option[Boolean],
    Contact2PhoneNumber: Option[String]
)

object OrganisationContactDetails {
  implicit val format: OFormat[OrganisationContactDetails] = Json.format[OrganisationContactDetails]
}
