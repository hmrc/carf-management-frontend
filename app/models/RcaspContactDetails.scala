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

package models

import config.Constants
import models.responses.AddressRegistrationResponse
import play.api.libs.json.{Json, OFormat, Reads, Writes}

case class TinDetails(TINType: String, TIN: String, IssuedBy: String)

case class RcaspContactDetails(ContactName: String, EmailAddress: String, PhoneNumber: Option[String])

case class RcaspAddress(
    AddressLine1: String,
    AddressLine2: Option[String],
    AddressLine3: Option[String],
    AddressLine4: Option[String],
    PostalCode: String,
    CountryCode: String
) {
  def toAddressRegistrationResponse: AddressRegistrationResponse =
    AddressRegistrationResponse(
      AddressLine1,
      AddressLine2,
      AddressLine3,
      AddressLine4,
      Some(PostalCode),
      CountryCode
    )

  def toAddressUk: Option[AddressUk] =
    if (Constants.acceptedUkCountryCode.contains(CountryCode.toUpperCase)) {
      val lines2To4       = Seq(AddressLine2, AddressLine3, AddressLine4).flatten
      val maybeTownOrCity = lines2To4.lastOption
      maybeTownOrCity.map { townOrCity =>
        val intermediateLines = lines2To4.dropRight(1)
        AddressUk(
          AddressLine1,
          addressLine2 = intermediateLines.headOption,
          addressLine3 = intermediateLines.lift(1),
          townOrCity,
          PostalCode
        )
      }
    } else {
      None
    }
}

object TinDetails {
  implicit val format: OFormat[TinDetails] = Json.format[TinDetails]
}

object RcaspAddress {
  implicit val format: OFormat[RcaspAddress] = Json.format[RcaspAddress]
}

object RcaspContactDetails {
  implicit val format: OFormat[RcaspContactDetails] = Json.format[RcaspContactDetails]
}
