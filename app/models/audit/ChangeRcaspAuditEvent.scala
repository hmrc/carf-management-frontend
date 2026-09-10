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

import play.api.libs.json.{JsValue, Json, OFormat}

case class ChangeRcaspAuditEvent(
    changeRegisteredBusinessRCASPUpdatedInformation: Option[ChangeRegisteredBusinessRcaspInformation],
    changeRegisteredBusinessRCASPOriginalInformation: Option[ChangeRegisteredBusinessRcaspInformation],
    changeOtherBusinessRCASPUpdatedInformation: Option[ChangeOtherBusinessRcaspInformation],
    changeOtherBusinessRCASPOriginalInformation: Option[ChangeOtherBusinessRcaspInformation]
)

object ChangeRcaspAuditEvent {
  implicit val format: OFormat[ChangeRcaspAuditEvent] = Json.format[ChangeRcaspAuditEvent]
}

case class ChangeRegisteredBusinessRcaspInformation(
    isBusinessAnRCASP: Boolean,
    organisationName: String,
    doesRCASPTradeUnderDifferentName: Boolean,
    RCASPTradingName: Option[String],
    RCASPAddress: String
)

object ChangeRegisteredBusinessRcaspInformation {
  implicit val format: OFormat[ChangeRegisteredBusinessRcaspInformation] =
    Json.format[ChangeRegisteredBusinessRcaspInformation]
}

case class ChangeOtherBusinessRcaspInformation(
    isBusinessAnRCASP: Boolean,
    isRCASPAnOrganisationOrIndividual: String,
    organisationName: Option[String],
    doesRCASPTradeUnderDifferentName: Option[Boolean],
    RCASPTradeName: Option[String],
    RCASPUTR: Option[String],
    IndividualRCASPFirstName: Option[String],
    IndividualRCASPLastName: Option[String],
    IndividualRCASPNino: Option[String],
    RCASPAddress: String,
    Contact1Name: Option[String],
    Contact1EmailAddress: Option[String],
    Contact1ContactByPhone: Option[Boolean],
    Contact1PhoneNumber: Option[String],
    Contact2: Option[Boolean],
    Contact2Name: Option[String],
    Contact2EmailAddress: Option[String],
    Contact2ContactByPhone: Option[Boolean],
    Contact2PhoneNumber: Option[String],
    individualEmailAddress: Option[String],
    individualContactByPhone: Option[Boolean],
    individuaPhoneNumber: Option[String]
)

object ChangeOtherBusinessRcaspInformation {
  implicit val format: OFormat[ChangeOtherBusinessRcaspInformation] = Json.format[ChangeOtherBusinessRcaspInformation]
}
