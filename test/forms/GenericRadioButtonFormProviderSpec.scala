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

package forms

import forms.behaviours.OptionFieldBehaviours
import models.Enumerable
import play.api.data.{Form, FormError}

class GenericRadioButtonFormProviderSpec extends OptionFieldBehaviours {

  sealed trait GenericTestClass

  object GenericTestClass extends Enumerable.Implicits {

    case object GenericOne extends GenericTestClass
    case object GenericTwo extends GenericTestClass

    val values: Seq[GenericTestClass] = Seq(GenericOne, GenericTwo)

    implicit val enumerable: Enumerable[GenericTestClass] =
      Enumerable(values.map(v => v.toString -> v): _*)
  }

  val requiredKey = "generic.error.required"

  val form: Form[GenericTestClass] = new GenericRadioButtonFormProvider()[GenericTestClass](requiredKey)

  ".value" - {

    val fieldName = "value"

    behave like optionsField[GenericTestClass](
      form,
      fieldName,
      validValues = GenericTestClass.values,
      invalidError = FormError(fieldName, "error.invalid")
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
