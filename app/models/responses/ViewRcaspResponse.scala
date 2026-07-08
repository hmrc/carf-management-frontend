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

package models.responses

import models.changeDetails.{IndividualRcaspDetailsForComparison, OrganisationRcaspDetailsForComparison, RcaspDetailsForComparison}
import play.api.libs.json.*
import models.{RcaspAddress, RcaspContactDetails, TinDetails}

sealed trait RcaspDetails {
  val SubscriptionID: String
  val RCASPID: String
  val IsRCASPUser: Boolean
  val PartyType: String
  val TINDetails: Option[List[TinDetails]]
  val AddressDetails: RcaspAddress
  val PrimaryContactDetails: Option[RcaspContactDetails]
}

object RcaspDetails {

  implicit val reads: Reads[RcaspDetails] = Reads { json =>
    (json \ "TradingName").validateOpt[String].flatMap {
      case Some(_) => json.validate[OrganisationRcaspDetails]
      case None    => json.validate[IndividualRcaspDetails]
    }
  }

  implicit val writes: Writes[RcaspDetails] = {
    case i: IndividualRcaspDetails   => IndividualRcaspDetails.format.writes(i)
    case o: OrganisationRcaspDetails => OrganisationRcaspDetails.format.writes(o)
  }
}

extension (rcaspDetails: RcaspDetails) {
  def getName: String =
    rcaspDetails match {
      case individual: IndividualRcaspDetails     => s"${individual.FirstName} ${individual.LastName}"
      case organisation: OrganisationRcaspDetails => organisation.RCASPName
    }

  def forComparison: Option[RcaspDetailsForComparison] =
    rcaspDetails match {
      case individual: IndividualRcaspDetails     =>
        for {
          nino  <- individual.TINDetails.flatMap(_.headOption.map(_.TIN))
          email <- individual.PrimaryContactDetails.map(_.EmailAddress)
        } yield IndividualRcaspDetailsForComparison(
          individual.IsRCASPUser,
          individual.FirstName,
          individual.LastName,
          nino,
          individual.AddressDetails,
          email,
          individual.PrimaryContactDetails.flatMap(_.PhoneNumber)
        )
      case organisation: OrganisationRcaspDetails =>
        organisation.TINDetails.flatMap(_.headOption.map(_.TIN)).map { utr =>
          OrganisationRcaspDetailsForComparison(
            organisation.IsRCASPUser,
            organisation.RCASPName,
            organisation.TradingName,
            utr,
            organisation.AddressDetails,
            organisation.PrimaryContactDetails,
            organisation.SecondaryContactDetails
          )
        }
    }
}

case class IndividualRcaspDetails(
    SubscriptionID: String,
    RCASPID: String,
    IsRCASPUser: Boolean,
    PartyType: String,
    FirstName: String,
    LastName: String,
    TINDetails: Option[List[TinDetails]],
    AddressDetails: RcaspAddress,
    PrimaryContactDetails: Option[RcaspContactDetails]
) extends RcaspDetails

object IndividualRcaspDetails {
  implicit val format: OFormat[IndividualRcaspDetails] = Json.format[IndividualRcaspDetails]
}

case class OrganisationRcaspDetails(
    SubscriptionID: String,
    RCASPID: String,
    IsRCASPUser: Boolean,
    PartyType: String,
    RCASPName: String,
    TradingName: String,
    TINDetails: Option[List[TinDetails]],
    AddressDetails: RcaspAddress,
    PrimaryContactDetails: Option[RcaspContactDetails],
    SecondaryContactDetails: Option[RcaspContactDetails]
) extends RcaspDetails

object OrganisationRcaspDetails {
  implicit val format: OFormat[OrganisationRcaspDetails] = Json.format[OrganisationRcaspDetails]
}

case class RcaspResponseDetails(RCASPList: List[RcaspDetails])

object RcaspResponseDetails {
  implicit val format: OFormat[RcaspResponseDetails] = Json.format[RcaspResponseDetails]
}

case class RcaspResponseParameters(ParamName: String, ParamValue: String)

object RcaspResponseParameters {
  implicit val format: OFormat[RcaspResponseParameters] = Json.format[RcaspResponseParameters]
}

case class RcaspResponseCommon(
    OriginatingSystem: String,
    TransmittingSystem: String,
    RequestType: String,
    Regime: String,
    ResponseParameters: Option[List[RcaspResponseParameters]]
)

object RcaspResponseCommon {
  implicit val format: OFormat[RcaspResponseCommon] = Json.format[RcaspResponseCommon]
}

case class ViewRcasp(ResponseCommon: RcaspResponseCommon, ResponseDetails: RcaspResponseDetails)

object ViewRcasp {
  implicit val format: OFormat[ViewRcasp] = Json.format[ViewRcasp]
}

case class ViewRcaspResponse(ViewRCASP: ViewRcasp)

object ViewRcaspResponse {
  implicit val format: OFormat[ViewRcaspResponse] = Json.format[ViewRcaspResponse]
}
