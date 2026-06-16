package forms

import forms.mappings.Mappings
import play.api.data.Form

import javax.inject.Inject

class FindAddressFormProvider @Inject() extends Mappings {

  def apply(): Form[String] =
    Form(
      "value" -> text("findAddress.error.required")
        .verifying(maxLength(35, "findAddress.error.length"))
    )
}
