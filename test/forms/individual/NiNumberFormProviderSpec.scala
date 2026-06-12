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

import forms.behaviours.StringFieldBehaviours
import play.api.data.FormError

class NiNumberFormProviderSpec extends StringFieldBehaviours {

  val requiredKey = "niNumber.error.required"
  val invalidKey  = "niNumber.error.invalid"
  val notRealKey  = "niNumber.error.notReal"

  val form = new NiNumberFormProvider()()

  ".value" - {

    val fieldName = "value"

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )

    "must bind NINOs that pass both regex checks" in {
      val validNinos = Seq(
        "AB123456C",
        "BA123456A",
        "CE123456A",
        "GH123456B"
      )

      validNinos.foreach { validNino =>
        val result = form.bind(Map(fieldName -> validNino))
        result.errors mustBe empty
        result.get mustEqual validNino
      }
    }

    "must not bind NINOs that fail ninoFormatRegex regex - Invalid Error" in {
      val invalidFormats = Seq(
        "A1234567B", // 8
        "AB12345678C", // 10
        "1B123456C", // starts with int
        "A1123456C", // second digit is int
        "AB1234567", // 9 but doesn't end in letter
        "AB-123456-C", // hyphens
        "ABCDEFGHI", // All letters
        "123456789", // All numbers
        "!B123456C", // Invalid first char
        "A@123456C", // Invalid second char
        "AB£23456C", // Invalid third char
        "AB1$3456C", // Invalid fourth char
        "AB12%456C", // Invalid fifth char
        "AB123^56C", // Invalid sixth char
        "AB1234&6C", // Invalid seventh char
        "AB12345*C", // Invalid eighth char
        "AB123456(" // Invalid ninth char
      )

      invalidFormats.foreach { invalidNino =>
        val result = form.bind(Map(fieldName -> invalidNino))
        result.errors must contain(FormError(fieldName, invalidKey))
      }
    }

    "must not bind NINOs that fail ninoRegex - have invalid prefixes (like example QQ123456C) - Not Real Error" in {
      val notReal = Seq(
        "QQ123456C",
        "BG123456C",
        "GB123456C",
        "NK123456C"
      )

      notReal.foreach { invalidNino =>
        val result = form.bind(Map(fieldName -> invalidNino))
        result.errors must contain(FormError(fieldName, notRealKey))
      }
    }

    "must normalize user input (spaces, convert to uppercase)" in {
      val normalizationCases = Seq(
        ("AB 12 34 56 C", "AB123456C"),
        ("ab123456c", "AB123456C"),
        ("Ab123456C", "AB123456C"),
        ("AB123456 C", "AB123456C"),
        (" AB123456C", "AB123456C")
      )

      normalizationCases.foreach { case (input, expected) =>
        val result = form.bind(Map(fieldName -> input))
        result.errors mustBe empty
        result.get mustEqual expected
      }
    }
  }
}
