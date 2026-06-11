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

package forms.individual

import config.Constants.standardTextInputWithoutNumbersRegex
import forms.mappings.Mappings
import models.individual.IndividualName
import play.api.data.Form
import play.api.data.Forms.*

import javax.inject.Inject

class IndividualNameFormProvider @Inject() extends Mappings {

  private val maxIndividualNameLength: Int = 35

  def apply(): Form[IndividualName] = Form(
    mapping(
      "firstName" -> validatedText(
        requiredKey = "individualName.error.firstName.required",
        invalidKey = "individualName.error.firstName.invalid",
        lengthKey = "individualName.error.firstName.length",
        regex = standardTextInputWithoutNumbersRegex,
        maxLength = maxIndividualNameLength
      ),
      "lastName"  -> validatedText(
        requiredKey = "individualName.error.lastName.required",
        invalidKey = "individualName.error.lastName.invalid",
        lengthKey = "individualName.error.lastName.length",
        regex = standardTextInputWithoutNumbersRegex,
        maxLength = maxIndividualNameLength
      )
    )(IndividualName.apply)(individualName => Some((individualName.firstName, individualName.lastName)))
  )
}
