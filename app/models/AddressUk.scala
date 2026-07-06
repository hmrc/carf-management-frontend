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

import config.Constants.ukCountryCode
import models.countries.CountryUk
import play.api.libs.json.{Json, OFormat}

import scala.collection.immutable.Seq

case class AddressUk(
    addressLine1: String,
    addressLine2: Option[String],
    addressLine3: Option[String],
    townOrCity: String,
    postCode: String,
    countryUk: CountryUk
)

extension (address: AddressUk) {
  def renderHtml: String = {
    val addressLines = Seq(
      Some(address.addressLine1),
      address.addressLine2,
      address.addressLine3,
      Some(address.townOrCity),
      Some(address.postCode)
    ).flatten.filter(_.nonEmpty)

    val htmlLines = addressLines.zipWithIndex.map { case (line, index) =>
      if (index < addressLines.length - 1) {
        s"""<span class="govuk-!-margin-bottom-0">$line</span>"""
      } else {
        line
      }
    }

    htmlLines.mkString("<br>")
  }

  def format: String = {

    val addressLines: Seq[String] = Seq(
      Some(address.addressLine1),
      address.addressLine2,
      address.addressLine3,
      Some(address.townOrCity),
      Some(address.postCode)
    ).flatten ++ {
      if (address.countryUk.code == ukCountryCode) {
        Seq.empty
      } else Seq(address.countryUk.name)
    }
    addressLines.mkString(", ")
  }

  def toRcaspAddress: RcaspAddress = {
    val addressSubsequentLines = Seq(address.addressLine2, Some(address.townOrCity), address.addressLine3).flatten
    RcaspAddress(
      AddressLine1 = address.addressLine1,
      AddressLine2 = addressSubsequentLines.headOption,
      AddressLine3 = addressSubsequentLines.lift(1),
      AddressLine4 = addressSubsequentLines.lift(2),
      PostalCode = address.postCode,
      CountryCode = address.countryUk.code
    )
  }
}

object AddressUk {
  implicit val format: OFormat[AddressUk] = Json.format[AddressUk]
}
