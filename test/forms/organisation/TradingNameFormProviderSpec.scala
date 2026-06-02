package forms.organisation

import forms.behaviours.StringFieldBehaviours
import play.api.data.FormError

class TradingNameFormProviderSpec extends StringFieldBehaviours {

  val requiredKey = "tradingName.error.required"
  val lengthKey = "tradingName.error.length"
  val maxLength = 105

  val form = new TradingNameFormProvider()()

  ".value" - {

    val fieldName = "tradingName-input"

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
