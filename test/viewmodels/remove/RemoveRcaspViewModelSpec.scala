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

package viewmodels.remove

import base.SpecBase
import forms.GenericYesNoPageFormProvider
import models.viewAndUpdateRcasp.RcaspDetails

class RemoveRcaspViewModelSpec extends SpecBase {

  val formProvider = new GenericYesNoPageFormProvider()

  private val rcaspDetails: RcaspDetails =
    organisationRcaspDetailsResponse.copy(RCASPID = rcaspId, IsRCASPUser = true)

  "RemoveRcaspViewModel.from" - {

    "when otherAccessAnswer is true" - {

      "must return the otherAccess title and heading keys" in {
        val vm = RemoveRcaspViewModel.from(rcaspDetails, otherAccessAnswer = true, formProvider)

        vm.titleKey   mustEqual "removeRcasp.title.otherAccess"
        vm.headingKey mustEqual "removeRcasp.heading.otherAccess"
        vm.rcaspName  mustEqual testOrgName
        vm.form       mustEqual formProvider("removeRcasp.error.required")
      }
    }

    "when otherAccessAnswer is false" - {

      "must return the default title and heading keys" in {
        val vm = RemoveRcaspViewModel.from(rcaspDetails, otherAccessAnswer = false, formProvider)

        vm.titleKey   mustEqual "removeRcasp.title"
        vm.headingKey mustEqual "removeRcasp.heading"
        vm.rcaspName  mustEqual testOrgName
        vm.form       mustEqual formProvider("removeRcasp.error.required")
      }
    }

    "must return the RCASP's name regardless of PartyType" in {
      val individualDetails = individualRcaspDetailsResponse.copy(RCASPID = rcaspId, IsRCASPUser = false)

      val vm = RemoveRcaspViewModel.from(individualDetails, otherAccessAnswer = false, formProvider)

      vm.rcaspName mustEqual individualDetails.getName
    }
  }
}
