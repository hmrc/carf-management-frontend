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

package forms.organisation

import config.Constants.{contactNameRegex, maxOrganisationContactNameLength}

import javax.inject.Inject
import forms.mappings.Mappings
import play.api.data.Form

class GenericOrganisationContactNameFormProvider @Inject() extends Mappings {

  def apply(messagesBaseKey: String): Form[String] =
    Form(
      "value" -> validatedText(
        requiredKey = s"$messagesBaseKey.error.required",
        invalidKey = s"$messagesBaseKey.error.invalid",
        lengthKey = s"$messagesBaseKey.error.length",
        regex = contactNameRegex,
        maxLength = maxOrganisationContactNameLength
      )
    )
}
