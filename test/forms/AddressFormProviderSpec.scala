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

import config.Constants.{addressMaxLength, addressRegex}
import forms.behaviours.StringFieldBehaviours
import org.scalacheck.Gen
import play.api.data.FormError

class AddressFormProviderSpec extends StringFieldBehaviours {

  private val formProvider = new AddressFormProvider()
  private val form         = formProvider()

  val validAddressStringGen: Gen[String] = {
    val allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 &.,'-"
    for {
      length <- Gen.choose(1, addressMaxLength)
      chars  <- Gen.listOfN(length, Gen.oneOf(allowedChars))
    } yield chars.mkString
  }.suchThat(_.trim.nonEmpty)

  val baseFormData: Map[String, String] = Map(
    "addressLine1" -> "1 Test Street",
    "townOrCity"   -> "Testville"
  )

  ".addressLine1" - {
    val fieldName   = "addressLine1"
    val requiredKey = "address.addressLine1.error.required"
    val lengthKey   = "address.addressLine1.error.length"
    val invalidKey  = "address.addressLine1.error.invalid"
    behave like fieldThatBindsValidData(form, fieldName, validAddressStringGen)

    "must not bind strings longer than the max length" in {
      val longString = "a" * (addressMaxLength + 1)
      val result     = form.bind(Map(fieldName -> longString)).apply(fieldName)
      result.errors must contain(FormError(fieldName, lengthKey, Seq(addressMaxLength)))
    }

    behave like mandatoryField(form, fieldName, requiredError = FormError(fieldName, requiredKey))

    "must not bind strings with invalid characters" in {
      val invalidString = "123 Street!"
      val result        = form.bind(Map(fieldName -> invalidString)).apply(fieldName)
      result.errors.head.key     mustBe fieldName
      result.errors.head.message mustBe invalidKey
    }
  }

  ".addressLine2" - {
    val fieldName  = "addressLine2"
    val lengthKey  = "address.addressLine2.error.length"
    val invalidKey = "address.addressLine2.error.invalid"
    behave like fieldThatBindsValidData(form, fieldName, validAddressStringGen)
    "must not bind strings longer than the max length" in {
      val longString = "a" * (addressMaxLength + 1)
      val result     = form.bind(Map(fieldName -> longString)).apply(fieldName)
      result.errors must contain(FormError(fieldName, lengthKey, Seq.empty))
    }

    "must bind an empty string as valid" in {
      val result = form.bind(Map(fieldName -> "")).apply(fieldName)
      result.errors mustBe empty
    }

    "must bind a whitespace string as None" in {
      val result = form.bind(Map(fieldName -> "  ")).apply(fieldName)

      result.errors mustBe empty
    }

    "must not bind strings with invalid characters" in {
      val invalidString = "Apt 4!"
      val result        = form.bind(Map(fieldName -> invalidString)).apply(fieldName)
      result.errors must contain(FormError(fieldName, invalidKey, Seq.empty))
    }
  }

  ".addressLine3" - {
    val fieldName  = "addressLine3"
    val lengthKey  = "address.addressLine3.error.length"
    val invalidKey = "address.addressLine3.error.invalid"
    behave like fieldThatBindsValidData(form, fieldName, validAddressStringGen)
    "must not bind strings longer than the max length" in {
      val longString = "a" * (addressMaxLength + 1)
      val result     = form.bind(Map(fieldName -> longString)).apply(fieldName)
      result.errors must contain(FormError(fieldName, lengthKey, Seq.empty))
    }

    "must bind an empty string as valid" in {
      val result = form.bind(Map(fieldName -> "")).apply(fieldName)
      result.errors mustBe empty
    }

    "must bind a whitespace string as None" in {
      val result = form.bind(Map(fieldName -> "  ")).apply(fieldName)
      result.errors mustBe empty
    }

    "must not bind strings with invalid characters" in {
      val invalidString = "Apt 4!"
      val result        = form.bind(Map(fieldName -> invalidString)).apply(fieldName)
      result.errors must contain(FormError(fieldName, invalidKey, Seq.empty))
    }
  }

  ".townOrCity" - {
    val fieldName   = "townOrCity"
    val requiredKey = "address.townOrCity.error.required"
    val lengthKey   = "address.townOrCity.error.length"
    val invalidKey  = "address.townOrCity.error.invalid"

    behave like fieldThatBindsValidData(form, fieldName, validAddressStringGen)

    "must not bind strings longer than the max length" in {
      val longString = "a" * (addressMaxLength + 1)
      val result     = form.bind(Map(fieldName -> longString)).apply(fieldName)
      result.errors must contain(FormError(fieldName, lengthKey, Seq(addressMaxLength)))
    }

    behave like mandatoryField(form, fieldName, requiredError = FormError(fieldName, requiredKey))

    "must not bind strings with invalid characters" in {
      val invalidString = "Luton!"
      val result        = form.bind(Map(fieldName -> invalidString)).apply(fieldName)
      result.errors must contain(FormError(fieldName, invalidKey, Seq(addressRegex)))
    }
  }

  ".postcode" - {
    "must return a required error if postcode is empty" in {
      val formData = baseFormData ++ Map(
        "addressLine1" -> "addressLine1",
        "townOrCity"   -> "town",
        "postcode"     -> ""
      )
      val result   = form.bind(formData)

      result.errors must contain(
        FormError("postcode", "address.postcode.error.required")
      )
    }

    "must return a length error if postcode is too long" in {
      val postcode = "A" * 20
      val formData = Map(
        "addressLine1" -> "addressLine1",
        "townOrCity"   -> "town",
        "postcode"     -> postcode
      )
      val result   = form.bind(formData)
      result.errors must contain(FormError("postcode", "address.postcode.error.length"))
    }

    "must return an invalid character error if postcode contains invalid chars" in {
      val postcode = "!!??"
      val formData = baseFormData ++ Map(
        "addressLine1" -> "addressLine1",
        "townOrCity"   -> "town",
        "postcode"     -> postcode
      )
      val result   = form.bind(formData)
      result.errors must contain(FormError("postcode", "address.postcode.error.invalid"))
    }

    "must return an 'invalid format' error for N123456" in {
      val formData = Map(
        "addressLine1" -> "addressLine1",
        "townOrCity"   -> "town",
        "postcode"     -> "N123456"
      )
      val result   = form.bind(formData)
      result.errors must contain(FormError("postcode", "address.postcode.error.invalidFormat"))
    }

    "must return a 'not uk' if Jersey postcode is present" in {
      val formData = Map(
        "addressLine1" -> "addressLine1",
        "townOrCity"   -> "town",
        "postcode"     -> "JE4 1AA"
      )

      val result = form.bind(formData)
      result.errors must contain(FormError("postcode", "address.postcode.error.notUK"))
    }

    "must return a 'not uk' if Guernsey postcode is present" in {
      val formData = Map(
        "addressLine1" -> "addressLine1",
        "townOrCity"   -> "town",
        "postcode"     -> "GY4 1AA"
      )

      val result = form.bind(formData)
      result.errors must contain(FormError("postcode", "address.postcode.error.notUK"))
    }

    "must return a 'not uk' if Isle of Man postcode is present" in {
      val formData = Map(
        "addressLine1" -> "addressLine1",
        "townOrCity"   -> "town",
        "postcode"     -> "IM4 1AA"
      )

      val result = form.bind(formData)
      result.errors must contain(FormError("postcode", "address.postcode.error.notUK"))
    }

    "must be valid if postcode is provided in a valid format" in {
      val formData =
        Map("addressLine1" -> "addressLine1", "townOrCity" -> "town", "postcode" -> "NW4 1QS")
      val result   = form.bind(formData)
      result.hasErrors mustBe false
    }

    "must be valid if postcode is provided in a valid format in lowercase" in {
      val formData =
        Map("addressLine1" -> "addressLine1", "townOrCity" -> "town", "postcode" -> "NW4 1qs")
      val result   = form.bind(formData)
      result.hasErrors mustBe false
    }

    "must be valid if a Birmingham postcode is provided in a valid format" in {
      val formData =
        Map("addressLine1" -> "addressLine1", "townOrCity" -> "town", "postcode" -> "B23 2AZ")
      val result   = form.bind(formData)
      result.hasErrors mustBe false
    }
  }

}
