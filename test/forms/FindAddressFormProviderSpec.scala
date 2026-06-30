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

import forms.behaviours.StringFieldBehaviours
import play.api.data.FormError

class FindAddressFormProviderSpec extends StringFieldBehaviours {

  val form = new FindAddressFormProvider()()

  ".postcode" - {

    val fieldName     = "postcode"
    val requiredKey   = "findAddress.postcode.error.required"
    val lengthKey     = "findAddress.postcode.error.length"
    val invalidKey    = "findAddress.postcode.error.invalid.characters"
    val invalidFormat = "findAddress.postcode.error.invalid.format"
    val notUK         = "findAddress.postcode.error.notUK"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      validGBOnlyNonCDPostcodes
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )

    behave like fieldWithInvalidData(
      form,
      fieldName,
      "xx9 9xx9",
      FormError(fieldName, invalidFormat)
    )

    behave like fieldWithInvalidData(
      form,
      fieldName,
      "!#2",
      FormError(fieldName, invalidKey),
      Some("format")
    )

    "must not bind postcodes longer than 10 characters" in {
      val result = form.bind(Map(fieldName -> "SW1A 1AAAAAAA"))
      result.errors must contain(FormError(fieldName, lengthKey))
    }

    "must bind postcodes with leading, trailing spaces and spaces between characters" in {
      val result = form.bind(Map(fieldName -> "  S W   1A 1AA  "))
      result.errors.filter(_.key == fieldName) mustBe empty
    }

    "must not bind postcodes with crown dependency codes" in {
      val result = form.bind(Map(fieldName -> "GY1 1AA"))
      result.errors must contain(FormError(fieldName, notUK))
    }
  }

  ".propertyNameOrNumber" - {

    val fieldName = "propertyNameOrNumber"
    val lengthKey = "findAddress.property.error.length"
    val maxLength = 35

    "must bind valid data" in {
      forAll(nonEmptyStringsWithMaxLength(maxLength) -> "validString") { string =>
        val result = form.bind(Map("postcode" -> "SW1A 1AA", fieldName -> string))
        result.errors.filter(_.key == fieldName) mustBe empty
      }
    }

    "must bind an empty string as None" in {
      val result = form.bind(Map("postcode" -> "SW1A 1AA", fieldName -> ""))
      result.value.flatMap(_.propertyNameOrNumber) mustBe None
    }

    "must not bind when field is missing" in {
      val result = form.bind(Map("postcode" -> "SW1A 1AA"))
      result.value.flatMap(_.propertyNameOrNumber) mustBe None
    }

    "must not bind strings longer than 35 characters" in {
      forAll(stringsLongerThan(maxLength) -> "longString") { string =>
        val result = form.bind(Map("postcode" -> "SW1A 1AA", fieldName -> string))
        result.errors must contain only FormError(fieldName, lengthKey, Seq(maxLength))
      }
    }

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      nonEmptyStringsWithMaxLength(maxLength)
    )

    behave like fieldWithMaxLength(
      form,
      fieldName,
      maxLength = maxLength,
      lengthError = FormError(fieldName, lengthKey, Seq(maxLength))
    )

  }
}
