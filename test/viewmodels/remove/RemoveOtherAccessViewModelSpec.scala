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

class RemoveOtherAccessViewModelSpec extends SpecBase {

  val formProvider = new GenericYesNoPageFormProvider()

  private val individualDetails: RcaspDetails =
    individualRcaspDetailsResponse.copy(RCASPID = rcaspId, IsRCASPUser = false)

  private val rcaspIsUserDetails: RcaspDetails =
    organisationRcaspDetailsResponse.copy(RCASPID = rcaspId, IsRCASPUser = true)

  private val otherOrgDetails: RcaspDetails =
    organisationRcaspDetailsResponse.copy(RCASPID = rcaspId, IsRCASPUser = false)

  "RemoveOtherAccessViewModel.from" - {

    "individual scenario" - {

      "must return correct keys and rcaspName when the user is an individual" in {
        val vm = RemoveOtherAccessViewModel.from(otherOrgDetails, isUserOrganisation = false, formProvider)

        vm.titleKey   mustEqual "removeOtherAccess.title.individual"
        vm.headingKey mustEqual "removeOtherAccess.heading.individual"
        vm.rcaspName  mustEqual testOrgName
        vm.form       mustEqual formProvider("removeOtherAccess.error.required.individual")
      }

      "must return individual wording even when the RCASP itself is registered to the individual" in {
        val vm = RemoveOtherAccessViewModel.from(individualDetails, isUserOrganisation = false, formProvider)

        vm.titleKey   mustEqual "removeOtherAccess.title.individual"
        vm.headingKey mustEqual "removeOtherAccess.heading.individual"
      }
    }

    "rcaspIsUser scenario" - {

      "must return correct keys and rcaspName" in {
        val vm = RemoveOtherAccessViewModel.from(rcaspIsUserDetails, isUserOrganisation = true, formProvider)

        vm.titleKey   mustEqual "removeOtherAccess.title.rcaspIsUser"
        vm.headingKey mustEqual "removeOtherAccess.heading.rcaspIsUser"
        vm.rcaspName  mustEqual testOrgName
        vm.form       mustEqual formProvider("removeOtherAccess.error.required.rcaspIsUser")
      }
    }

    "otherOrg scenario" - {

      "must return correct keys and rcaspName" in {
        val vm = RemoveOtherAccessViewModel.from(otherOrgDetails, isUserOrganisation = true, formProvider)

        vm.titleKey   mustEqual "removeOtherAccess.title.otherOrg"
        vm.headingKey mustEqual "removeOtherAccess.heading.otherOrg"
        vm.rcaspName  mustEqual testOrgName
        vm.form       mustEqual formProvider("removeOtherAccess.error.required.otherOrg")
      }
    }
  }
}
