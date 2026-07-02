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

import models.errors.{CarfError, ConversionError}
import models.{AddressAndUPRN, AddressUk}
import play.api.libs.json.{Json, OFormat}

case class AddressResponse(
    id: String,
    uprn: Long,
    address: AddressRecord
)

object AddressResponse {
  implicit val format: OFormat[AddressResponse] = Json.format[AddressResponse]

  extension (addressResponse: AddressResponse)
    def toDomainAddressUk: Either[CarfError, AddressAndUPRN] = {
      val address = addressResponse.address
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
