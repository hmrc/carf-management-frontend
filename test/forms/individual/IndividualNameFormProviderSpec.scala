package forms.individual

import forms.behaviours.StringFieldBehaviours
import play.api.data.FormError

class IndividualNameFormProviderSpec extends StringFieldBehaviours {

  val form = new IndividualNameFormProvider()()

  ".firstName" - {

    val fieldName   = "firstName"
    val requiredKey = "individualName.error.firstName.required"
    val lengthKey   = "individualName.error.firstName.length"
    val maxLength   = 35

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      stringsWithMaxLength(maxLength)
    )

    behave like fieldWithMaxLength(
      form,
      fieldName,
      maxLength = maxLength,
      lengthError = FormError(fieldName, lengthKey, Seq(maxLength))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }

  ".lastName" - {

    val fieldName   = "lastName"
    val requiredKey = "individualName.error.lastName.required"
    val lengthKey   = "individualName.error.lastName.length"
    val maxLength   = 35

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      stringsWithMaxLength(maxLength)
    )

    behave like fieldWithMaxLength(
      form,
      fieldName,
      maxLength = maxLength,
      lengthError = FormError(fieldName, lengthKey, Seq(maxLength))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
