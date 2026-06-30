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
import org.scalacheck.Gen
import play.api.data.FormError

class IndividualNameFormProviderSpec extends StringFieldBehaviours {

  val form = new IndividualNameFormProvider()()

  val validIndividualNameChars: Gen[Char] = Gen.oneOf(
    "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ &'\\`^-".toSeq
  )

  val maxNameLength = 35

  val individualNameGen: Gen[String] = {
    for {
      length <- Gen.choose(1, maxNameLength)
      chars  <- Gen.listOfN(length, validIndividualNameChars)
    } yield chars.mkString
  }.suchThat(_.trim.nonEmpty)

  ".firstName" - {

    val fieldName   = "firstName"
    val requiredKey = "individualName.error.firstName.required"
    val lengthKey   = "individualName.error.firstName.length"
    val invalidKey  = "individualName.error.firstName.invalid"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      individualNameGen
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )

    "not bind strings with invalid characters" in {
      val invalidCharGen = Gen.oneOf("!\"#$%()*+,./:;<=>?@[]_{|}~").map(_.toString)
      forAll(invalidCharGen) { invalidChar =>
        val result = form.bind(Map(fieldName -> s"invalid${invalidChar}char")).apply(fieldName)
        result.errors must contain(FormError(fieldName, invalidKey))
      }
    }

    "not bind strings longer than the max length" in {
      val longString = "a" * (maxNameLength + 1)

      val result = form.bind(Map(fieldName -> longString, "lastName" -> "a"))
      result.errors must contain only FormError(fieldName, lengthKey, Seq.empty)
    }
  }

  ".lastName" - {

    val fieldName   = "lastName"
    val requiredKey = "individualName.error.lastName.required"
    val lengthKey   = "individualName.error.lastName.length"
    val invalidKey  = "individualName.error.lastName.invalid"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      individualNameGen
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )

    "not bind strings with invalid characters" in {
      val invalidCharGen = Gen.oneOf("!\"#$%()*+,./:;<=>?@[]_{|}~").map(_.toString)
      forAll(invalidCharGen) { invalidChar =>
        val result = form.bind(Map(fieldName -> s"invalid${invalidChar}char")).apply(fieldName)
        result.errors must contain(FormError(fieldName, invalidKey))
      }
    }

    "not bind strings longer than the max length" in {
      val longString = "a" * (maxNameLength + 1)

      val result = form.bind(Map(fieldName -> longString, "firstName" -> "a"))
      result.errors must contain only FormError(fieldName, lengthKey, Seq.empty)
    }
  }
}
