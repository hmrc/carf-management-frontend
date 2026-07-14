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

class RemoveUserAccessViewModelSpec extends SpecBase {

  val formProvider = new GenericYesNoPageFormProvider()

  private val individualDetails: RcaspDetails =
    individualRcaspDetailsResponse.copy(RCASPID = rcaspId, IsRCASPUser = false)

  private val rcaspIsUserDetails: RcaspDetails =
    organisationRcaspDetailsResponse.copy(RCASPID = rcaspId, IsRCASPUser = true)

  private val otherOrgDetails: RcaspDetails =
    organisationRcaspDetailsResponse.copy(RCASPID = rcaspId, IsRCASPUser = false)

  "RemoveUserAccessViewModel.from" - {

    "individual scenario" - {

      "must return correct keys and no userBusinessName" in {
        val vm = RemoveUserAccessViewModel.from(individualDetails, Some("My Business"), formProvider)

        vm.titleKey      mustEqual "removeUserAccess.title.individual"
        vm.headingKey    mustEqual "removeUserAccess.heading.individual"
        vm.errorKey      mustEqual "removeUserAccess.error.required.individual"
        vm.rcaspName     mustEqual "Timmy Jimmison"
        vm.userBusinessName mustBe None
      }
    }

    "rcaspIsUser scenario" - {

      "must return correct keys and no userBusinessName" in {
        val vm = RemoveUserAccessViewModel.from(rcaspIsUserDetails, Some("My Business"), formProvider)

        vm.titleKey      mustEqual "removeUserAccess.title.rcaspIsUser"
        vm.headingKey    mustEqual "removeUserAccess.heading.rcaspIsUser"
        vm.errorKey      mustEqual "removeUserAccess.error.required.rcaspIsUser"
        vm.rcaspName     mustEqual testOrgName
        vm.userBusinessName mustBe None
      }
    }

    "otherOrg scenario" - {

      "must return correct keys and user business name when provided" in {
        val vm = RemoveUserAccessViewModel.from(otherOrgDetails, Some("My Business"), formProvider)

        vm.titleKey      mustEqual "removeUserAccess.title.otherOrg"
        vm.headingKey    mustEqual "removeUserAccess.heading.otherOrg"
        vm.errorKey      mustEqual "removeUserAccess.error.required.otherOrg"
        vm.rcaspName     mustEqual testOrgName
        vm.userBusinessName mustBe Some("My Business")
        vm.form          mustEqual formProvider("removeUserAccess.error.required.otherOrg")
      }
    }
  }
}
