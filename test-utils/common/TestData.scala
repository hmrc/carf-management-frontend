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

package common

import generators.ModelGenerators
import models._
import models.responses._

trait TestData extends ModelGenerators {

  val carfId: String      = "XCCAR0024000102"
  val rcaspIdNone: String = "none"

  val rcaspContactDetails: RcaspContactDetails =
    RcaspContactDetails(
      ContactName = "Prof Sada",
      EmailAddress = "test@example.com",
      PhoneNumber = Some("07123412345")
    )
  val rcaspResponseCommon: RcaspResponseCommon =
    RcaspResponseCommon(
      OriginatingSystem = "CADX",
      TransmittingSystem = "EIS",
      RequestType = "VIEW",
      Regime = "CARF",
      ResponseParameters = None
    )

  private def rcaspAddress =
    RcaspAddress(
      AddressLine1 = "64",
      AddressLine2 = Some("Zoo"),
      AddressLine3 = Some("Lane"),
      AddressLine4 = Some("Sixty Four"),
      PostalCode = "G66 2AZ",
      CountryCode = "GB"
    )

  val testViewRcaspResponse: ViewRcaspResponse =
    ViewRcaspResponse(
      ViewRCASP = ViewRcasp(
        ResponseCommon = rcaspResponseCommon,
        ResponseDetails = RcaspResponseDetails(
          RCASPList = List(
            OrganisationRcaspDetails(
              SubscriptionID = carfId,
              RCASPID = rcaspIdNone,
              IsRCASPUser = true,
              PartyType = "Organisation",
              RCASPName = "Mesagoza",
              TradingName = "Uva Academy",
              TINDetails = Some(List(TinDetails(TINType = "UTR", TIN = "68936493", IssuedBy = "GB"))),
              AddressDetails = rcaspAddress,
              PrimaryContactDetails = Some(rcaspContactDetails),
              SecondaryContactDetails = Some(rcaspContactDetails.copy(ContactName = "Prof Turo"))
            )
          )
        )
      )
    )

}
