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

package forms.combined

import base.SpecBase
import models.OrganisationOrIndividual
import play.api.data.FormError

class OrganisationOrIndividualFormProviderSpec extends SpecBase {

  val requiredKey: String = "organisationOrIndividual.error.required"
  val invalidKey: String  = "organisationOrIndividual.error.required"

  val form = new OrganisationOrIndividualFormProvider()(requiredKey)

  ".value" - {

    "must bind valid Organisation value" in {
      val result = form.bind(Map("value" -> "Organisation"))

      result.errors         mustBe empty
      result.value.value mustEqual OrganisationOrIndividual.Organisation
    }

    "must bind valid Individual value" in {
      val result = form.bind(Map("value" -> "Individual"))

      result.errors         mustBe empty
      result.value.value mustEqual OrganisationOrIndividual.Individual
    }

    "must produce a required error when value is missing" in {
      val result = form.bind(Map.empty[String, String])

      result.errors must contain only FormError("value", requiredKey)
    }

    "must produce an error when value is invalid" in {
      val result = form.bind(Map("value" -> "invalid"))

      result.errors must contain only FormError("value", invalidKey)
    }
  }
}
