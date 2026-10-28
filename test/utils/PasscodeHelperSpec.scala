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

package utils

import base.SpecBase
import config.FrontendAppConfig
import models.UserAnswers
import org.mockito.Mockito.*
import pages.PasscodePage

class PasscodeHelperSpec extends SpecBase {

  val mockAppConfig: FrontendAppConfig = mock[FrontendAppConfig]

  val testHelper: PasscodeHelper = PasscodeHelper(mockAppConfig)

  ".handlePasscodePersistence" - {

    "must persist passcode in user answers when passcode check is enabled" in {
      when(mockAppConfig.passcodeCheckEnabled).thenReturn(true)

      val expectedUserAnswers = emptyUserAnswers.withPage(PasscodePage, "foo")

      val result: UserAnswers = testHelper.handlePasscodePersistence(emptyUserAnswers, Some("foo")).futureValue

      result mustBe expectedUserAnswers
    }

    "must do nothing to user answers when passcode check is disabled" in {
      when(mockAppConfig.passcodeCheckEnabled).thenReturn(false)

      val result: UserAnswers = testHelper.handlePasscodePersistence(emptyUserAnswers, Some("foo")).futureValue

      result mustBe emptyUserAnswers
    }
  }
}
