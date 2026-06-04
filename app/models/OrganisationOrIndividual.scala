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

import play.api.libs.json.{Format, JsError, JsString, JsSuccess, Reads, Writes}

sealed trait OrganisationOrIndividual {
  override def toString: String = this match {
    case OrganisationOrIndividual.Organisation => "Organisation"
    case OrganisationOrIndividual.Individual   => "Individual"
  }
}

object OrganisationOrIndividual {

  case object Organisation extends OrganisationOrIndividual
  case object Individual extends OrganisationOrIndividual

  val values: Seq[OrganisationOrIndividual] = Seq(Organisation, Individual)

  implicit val enumerable: Enumerable[OrganisationOrIndividual] =
    Enumerable(values.map(v => v.toString -> v): _*)

  implicit val reads: Reads[OrganisationOrIndividual] = Reads {
    case JsString("Organisation") => JsSuccess(Organisation)
    case JsString("Individual")   => JsSuccess(Individual)
    case _                        => JsError("organisationOrIndividual.error.required")
  }

  implicit val writes: Writes[OrganisationOrIndividual] = Writes {
    case Organisation => JsString("Organisation")
    case Individual   => JsString("Individual")
  }

  implicit val format: Format[OrganisationOrIndividual] = Format(reads, writes)
}
