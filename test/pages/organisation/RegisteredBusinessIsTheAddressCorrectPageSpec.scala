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

package pages.organisation

import base.SpecBase

class RegisteredBusinessIsTheAddressCorrectPageSpec extends SpecBase {

  "RegisteredBusinessIsTheAddressCorrectPage" - {

    "must have the correct toString" in {
      RegisteredBusinessIsTheAddressCorrectPage.toString mustBe "registeredBusinessIsTheAddressCorrect"
    }

    "must be able to set and retrieve a value" in {
      val ua = emptyUserAnswers
        .withPage(RegisteredBusinessIsTheAddressCorrectPage, true)

      ua.get(RegisteredBusinessIsTheAddressCorrectPage) mustBe Some(true)
    }

    "must be able to set false value" in {
      val ua = emptyUserAnswers
        .withPage(RegisteredBusinessIsTheAddressCorrectPage, false)

      ua.get(RegisteredBusinessIsTheAddressCorrectPage) mustBe Some(false)
    }

    "must return None when page not set" in {
      emptyUserAnswers.get(RegisteredBusinessIsTheAddressCorrectPage) mustBe None
    }

    "must be able to remove the value" in {
      val ua = emptyUserAnswers
        .withPage(RegisteredBusinessIsTheAddressCorrectPage, true)

      val result = ua.remove(RegisteredBusinessIsTheAddressCorrectPage).success.value

      result.get(RegisteredBusinessIsTheAddressCorrectPage) mustBe None
    }
  }
}
