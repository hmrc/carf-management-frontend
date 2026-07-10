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

import config.Constants.acceptedUkCountryCode
import models.errors.{CarfError, ConversionError, InvalidCountryCode}
import models.{AddressAndUPRN, AddressUk}
import play.api.libs.json.{Json, OFormat}

case class AddressLookupResponse(
    id: String,
    uprn: Long,
    address: AddressRecord
)

object AddressLookupResponse {
  implicit val format: OFormat[AddressLookupResponse] = Json.format[AddressLookupResponse]

  extension (addressResponse: AddressLookupResponse)
    def toDomainAddressAndUprn: Either[CarfError, AddressAndUPRN] = {
      val address = addressResponse.address
      if !acceptedUkCountryCode.contains(address.country.code.toUpperCase) then Left(InvalidCountryCode)
      else
        address.lines match {
          case head :: next =>
            Right(
              AddressAndUPRN(
                AddressUk(
                  addressLine1 = head,
                  addressLine2 = next.headOption,
                  addressLine3 = next.lift(1),
                  townOrCity = address.town,
                  postCode = address.postcode
                ),
                addressResponse.uprn
              )
            )
          case Nil          => Left(ConversionError)
        }
    }
}

case class AddressRecord(
    lines: List[String],
    town: String,
    postcode: String,
    country: CountryRecord
)

object AddressRecord {
  implicit val format: OFormat[AddressRecord] = Json.format[AddressRecord]
}

case class CountryRecord(
    // ISO3166-1 or ISO3166-2 code, e.g. "GB" or "GB-ENG" (note that "GB" is the official
    // code for UK although "UK" is a reserved synonym and may be used instead)
    // See https://en.wikipedia.org/wiki/ISO_3166-1_alpha-2
    // and https://en.wikipedia.org/wiki/ISO_3166-2:GB
    code: String,
    // The printable name for the country, e.g. "United Kingdom"
    name: String
)

object CountryRecord {
  implicit val format: OFormat[CountryRecord] = Json.format[CountryRecord]
}
