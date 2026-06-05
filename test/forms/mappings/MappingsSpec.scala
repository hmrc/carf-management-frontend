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

package forms.mappings

import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.data.{Form, FormError}
import models.Enumerable

object MappingsSpec {

  sealed trait Foo
  case object Bar extends Foo
  case object Baz extends Foo

  object Foo {

    val values: Set[Foo] = Set(Bar, Baz)

    implicit val fooEnumerable: Enumerable[Foo] =
      Enumerable(values.toSeq.map(v => v.toString -> v): _*)
  }
}

class MappingsSpec extends AnyFreeSpec with Matchers with OptionValues with Mappings {

  import MappingsSpec._

  "text" - {

    val testForm: Form[String] =
      Form(
        "value" -> text()
      )

    "must bind a valid string" in {
      val result = testForm.bind(Map("value" -> "foobar"))
      result.get mustEqual "foobar"
    }

    "must not bind an empty string" in {
      val result = testForm.bind(Map("value" -> ""))
      result.errors must contain(FormError("value", "error.required"))
    }

    "must not bind a string of whitespace only" in {
      val result = testForm.bind(Map("value" -> " \t"))
      result.errors must contain(FormError("value", "error.required"))
    }

    "must not bind an empty map" in {
      val result = testForm.bind(Map.empty[String, String])
      result.errors must contain(FormError("value", "error.required"))
    }

    "must return a custom error message" in {
      val form   = Form("value" -> text("custom.error"))
      val result = form.bind(Map("value" -> ""))
      result.errors must contain(FormError("value", "custom.error"))
    }

    "must unbind a valid value" in {
      val result = testForm.fill("foobar")
      result.apply("value").value.value mustEqual "foobar"
    }
  }

  "boolean" - {

    val testForm: Form[Boolean] =
      Form(
        "value" -> boolean()
      )

    "must bind true" in {
      val result = testForm.bind(Map("value" -> "true"))
      result.get mustEqual true
    }

    "must bind false" in {
      val result = testForm.bind(Map("value" -> "false"))
      result.get mustEqual false
    }

    "must not bind a non-boolean" in {
      val result = testForm.bind(Map("value" -> "not a boolean"))
      result.errors must contain(FormError("value", "error.boolean"))
    }

    "must not bind an empty value" in {
      val result = testForm.bind(Map("value" -> ""))
      result.errors must contain(FormError("value", "error.required"))
    }

    "must not bind an empty map" in {
      val result = testForm.bind(Map.empty[String, String])
      result.errors must contain(FormError("value", "error.required"))
    }

    "must unbind" in {
      val result = testForm.fill(true)
      result.apply("value").value.value mustEqual "true"
    }
  }

  "int" - {

    val testForm: Form[Int] =
      Form(
        "value" -> int()
      )

    "must bind a valid integer" in {
      val result = testForm.bind(Map("value" -> "1"))
      result.get mustEqual 1
    }

    "must not bind an empty value" in {
      val result = testForm.bind(Map("value" -> ""))
      result.errors must contain(FormError("value", "error.required"))
    }

    "must not bind an empty map" in {
      val result = testForm.bind(Map.empty[String, String])
      result.errors must contain(FormError("value", "error.required"))
    }

    "must unbind a valid value" in {
      val result = testForm.fill(123)
      result.apply("value").value.value mustEqual "123"
    }
  }

  "enumerable" - {

    val testForm = Form(
      "value" -> enumerable[Foo]()
    )

    "must bind a valid option" in {
      val result = testForm.bind(Map("value" -> "Bar"))
      result.get mustEqual Bar
    }

    "must not bind an invalid option" in {
      val result = testForm.bind(Map("value" -> "Not Bar"))
      result.errors must contain(FormError("value", "error.invalid"))
    }

    "must not bind an empty map" in {
      val result = testForm.bind(Map.empty[String, String])
      result.errors must contain(FormError("value", "error.required"))
    }
  }

  "currency" - {

    val testForm: Form[BigDecimal] =
      Form(
        "value" -> currency()
      )

    "must bind a valid integer" in {
      val result = testForm.bind(Map("value" -> "1"))
      result.get mustEqual 1
    }

    "must bind a valid decimal with 1 decimal place" in {
      val result = testForm.bind(Map("value" -> "1.2"))
      result.get mustEqual 1.2
    }

    "must bind a valid decimal with 2 decimal places" in {
      val result = testForm.bind(Map("value" -> "1.23"))
      result.get mustEqual 1.23
    }

    "must bind a valid number with spaces, commas and `£` characters" in {
      val result = testForm.bind(Map("value" -> "£ 1,234 . 01"))
      result.get mustEqual 1234.01
    }

    "must not bind values with a `£` after any numbers" in {
      val result = testForm.bind(Map("value" -> "123 £456"))
      result.errors must contain only FormError("value", "error.nonNumeric")
    }

    "must not bind values with non-numeric characters except commas, spaces and `£`s" in {
      val result = testForm.bind(Map("value" -> "abc"))
      result.errors must contain only FormError("value", "error.nonNumeric")
    }

    "must not bind an empty value" in {
      val result = testForm.bind(Map("value" -> ""))
      result.errors must contain(FormError("value", "error.required"))
    }

    "must not bind an empty map" in {
      val result = testForm.bind(Map.empty[String, String])
      result.errors must contain(FormError("value", "error.required"))
    }

    "must not bind a number with more than 2 decimal places" in {
      val result = testForm.bind(Map("value" -> "1.234"))
      result.errors must contain only FormError("value", "error.invalidNumeric")
    }

    "must not bind negative numbers" in {
      val result = testForm.bind(Map("value" -> "-1"))
      result.errors must contain only FormError("value", "error.nonNumeric")
    }

    "must unbind a valid value" in {
      val result = testForm.fill(1)
      result.apply("value").value.value mustEqual "1"
    }
  }

  "validatedTextFormatter" - {

    val testMaxLength   = 99
    val testMinLength   = 2
    val testRegex       = """^[a-zA-Z0-9 &'\\`^\-]*$"""
    val testInvalidKey  = "test.invalidKey"
    val testRequiredKey = "test.requiredKey"
    val testLengthKey   = "test.lengthKey"

    val testForm: Form[String] =
      Form(
        "value" -> validatedText(
          requiredKey = testRequiredKey,
          invalidKey = testInvalidKey,
          lengthKey = testLengthKey,
          regex = testRegex,
          maxLength = testMaxLength,
          minLength = testMinLength
        )
      )

    "must bind a valid string" in {
      val result = testForm.bind(Map("value" -> "foobar"))
      result.get mustEqual "foobar"
    }

    "must bind a valid of min length" in {
      val result = testForm.bind(Map("value" -> "a" * testMinLength))
      result.get mustEqual "a" * testMinLength
    }

    "must bind a valid of max length" in {
      val result = testForm.bind(Map("value" -> "a" * testMaxLength))
      result.get mustEqual "a" * testMaxLength
    }

    "must replace non-breaking spaces with regular spaces" in {
      val result = testForm.bind(Map("value" -> "a\u00A0a"))
      result.get mustEqual "a a"
    }

    "must replace multiple non-breaking spaces with regular spaces" in {
      val result = testForm.bind(Map("value" -> "hello\u00A0world\u00A0foo"))
      result.get mustEqual "hello world foo"
    }

    "must not bind an empty string" in {
      val result = testForm.bind(Map("value" -> ""))
      result.errors must contain(FormError("value", testRequiredKey))
    }

    "must not bind a string too long" in {
      val result = testForm.bind(Map("value" -> "a" * (testMaxLength + 1)))
      result.errors must contain(FormError("value", testLengthKey))
    }

    "must not bind a string too short" in {
      val result = testForm.bind(Map("value" -> "a" * (testMinLength - 1)))
      result.errors must contain(FormError("value", testLengthKey))
    }

    "must not bind a string that fails the regex check" in {
      val result = testForm.bind(Map("value" -> "???"))
      result.errors must contain(FormError("value", testInvalidKey))
    }

    "must not bind a string of whitespace only" in {
      val result = testForm.bind(Map("value" -> " \t"))
      result.errors must contain(FormError("value", testRequiredKey))
    }

    "must not bind an empty map" in {
      val result = testForm.bind(Map.empty[String, String])
      result.errors must contain(FormError("value", testRequiredKey))
    }

    "must unbind a valid value" in {
      val result = testForm.fill("foobar")
      result.apply("value").value.value mustEqual "foobar"
    }
  }

  "nationalInsuranceNumberFormatter" - {

    val testMaxLength   = 9
    val testRequiredKey = "test.requiredKey"
    val testInvalidKey  = "test.invalidKey"
    val testNotRealKey  = "test.notRealKey"

    val testForm: Form[String] = Form(
      "value" -> nationalInsuranceNumber(
        requiredKey = testRequiredKey,
        invalidKey = testInvalidKey,
        notRealKey = testNotRealKey
      )
    )

    "must bind a valid string" in {
      val result = testForm.bind(Map("value" -> "BA123456A"))
      result.get mustEqual "BA123456A"
    }

    "must bind a valid string with spaces at the start and end" in {
      val result = testForm.bind(Map("value" -> " BA123456A "))
      result.get mustEqual "BA123456A"
    }

    "must bind a valid string with spaces throughout" in {
      val result = testForm.bind(Map("value" -> " BA  1 2   34 56 A "))
      result.get mustEqual "BA123456A"
    }

    "must not bind an empty map" in {
      val result = testForm.bind(Map.empty[String, String])
      result.errors must contain(FormError("value", testRequiredKey))
    }

    "must not bind an empty string" in {
      val result = testForm.bind(Map("value" -> ""))
      result.errors must contain(FormError("value", testRequiredKey))
    }

    "must not bind an string with just spaces" in {
      val result = testForm.bind(Map("value" -> "         "))
      result.errors must contain(FormError("value", testRequiredKey))
    }

    "must not bind a string too long" in {
      val result = testForm.bind(Map("value" -> "a" * (testMaxLength + 1)))
      result.errors must contain(FormError("value", testInvalidKey))
    }

    "must not bind a string that fails that has invalid characters" in {
      val result = testForm.bind(Map("value" -> "???"))
      result.errors must contain(FormError("value", testInvalidKey))
    }

    "must not bind a string that has valid characters but is an example ni number" in {
      val result = testForm.bind(Map("value" -> "QQ 12 34 56 C"))
      result.errors must contain(FormError("value", testNotRealKey))
    }

    "must not bind a string that has valid characters but is not real" in {
      val result = testForm.bind(Map("value" -> "DD123456D"))
      result.errors must contain(FormError("value", testNotRealKey))
    }

    "must unbind a valid value" in {
      val result = testForm.fill("foobar")
      result.apply("value").value.value mustEqual "foobar"
    }
  }

  "phoneNumber" - {

    val testRequiredKey           = "firstContactPhoneNumber.error.required"
    val testInvalidKey            = "firstContactPhoneNumber.error.invalid"
    val testLengthKey             = "firstContactPhoneNumber.error.length"
    val testNotRealPhoneNumberKey = "firstContactPhoneNumber.error.notRealNumber"

    val notRealNumbers = Seq(
      "+44795634982",
      "09956349826",
      "+1 555 0101",
      "+1 760-412-7",
      "01632 960 001", // test only local area number
      "07700 900 982", // test only number
      "07700 990 982", // unallocated number valid format
      "+44 808 157 0192", // specifically treated as notReal because it is in the user error messages.
      "08081570192"
    )

    val invalidNumbers = Seq(
      "abcdefg",
      "+999999999",
      "+44",
      "071234567890", // too long
      "+44 123"
    )

    val testPhoneNumberForm: Form[String] = Form(
      "value" -> phoneNumber(
        requiredKey = testRequiredKey,
        invalidKey = testInvalidKey,
        lengthKey = testLengthKey,
        notRealPhoneNumberKey = testNotRealPhoneNumberKey
      )
    )

    "must bind a valid Phone Number that is 24 characters long" in {
      val result = testPhoneNumberForm.bind(Map("value" -> validPhoneNumber24Chars))
      result.get mustBe validPhoneNumber24Chars
    }

    "must bind a valid Phone Number that is 105 characters long with spaces either side" in {
      val result = testPhoneNumberForm.bind(Map("value" -> s"     $validPhoneNumber24Chars    "))
      result.get mustBe validPhoneNumber24Chars
    }

    "must not bind an empty Phone Number" in {
      val result = testPhoneNumberForm.bind(Map("value" -> ""))
      result.errors must contain(
        FormError("value", testRequiredKey)
      )
    }

    "must not bind Phone Number that is too long" in {
      val result = testPhoneNumberForm.bind(Map("value" -> invalidPhoneNumber25Chars))
      result.errors must contain(
        FormError("value", testLengthKey)
      )
    }

    "not bind phone numbers with invalid characters" in {
      invalidNumbers.foreach { invalidPhoneNumber =>
        val result = testPhoneNumberForm.bind(Map("value" -> invalidPhoneNumber))

        withClue(s"Expected error for invalid phone number: '$invalidPhoneNumber'") {
          result.errors mustBe Seq(FormError("value", testInvalidKey))
        }
      }
    }

    "not bind notReal phone numbers" in {
      notRealNumbers.foreach { notRealPhoneNumber =>
        val result = testPhoneNumberForm.bind(Map("value" -> notRealPhoneNumber))

        withClue(s"Expected error for invalid phone number: '$notRealPhoneNumber'") {
          result.errors mustBe Seq(FormError("value", testNotRealPhoneNumberKey))
        }
      }
    }

    "bind valid phone numbers" in {
      val validNumbers = Seq(
        "07123456789",
        "+447123456789",
        "02079460000",
        "+1 650 253 0000",
        "+33 1 42 68 53 00",
        "+49 30 123456",
        "+91 98765 43210",
        "07400111222 ext 5",
        "++447123456789", // google lib tries to recover extra punctuation where possible, like parsing ++44 as +44
        "+1 (650) 253-0000 x123",
        "07700 899 999", // one below test numbers
        "07700a899g999",
        "+447700a899g999"
      )

      validNumbers.foreach { validPhoneNumber =>
        val testValue = validPhoneNumber
        val result    = testPhoneNumberForm.bind(Map("value" -> testValue))

        withClue(s"Expected no errors for valid phone number: '$testValue'") {
          result.errors.isEmpty mustBe true
        }
      }
    }

  }

}
