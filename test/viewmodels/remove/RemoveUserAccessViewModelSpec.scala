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
import models.UserBusinessSubscriptionData
import models.viewAndUpdateRcasp.RcaspDetails
import play.api.i18n.Messages

class RemoveUserAccessViewModelSpec extends SpecBase {

  implicit val messages: Messages = messages(app)

  val formProvider = new GenericYesNoPageFormProvider()

  private val individualDetails: RcaspDetails =
    individualRcaspDetailsViewUpdate.copy(RCASPID = rcaspId, IsRCASPUser = false)

  private val rcaspIsUserDetails: RcaspDetails =
    organisationRcaspDetailsViewUpdate.copy(RCASPID = rcaspId, IsRCASPUser = true)

  private val otherOrgDetails: RcaspDetails =
    organisationRcaspDetailsViewUpdate.copy(RCASPID = rcaspId, IsRCASPUser = false)

  "RemoveUserAccessViewModel.from" - {

    "individual scenario" - {

      "must return correct keys and no userBusinessName when the user is an individual" in {
        val userInfo =
          UserBusinessSubscriptionData(hasOrganisationContactDetails = false, organisationName = None)
        val vm       = RemoveUserAccessViewModel.from(individualDetails, userInfo, formProvider)

        vm.titleKey         mustEqual "removeUserAccess.title.individual"
        vm.headingKey       mustEqual "removeUserAccess.heading.individual"
        vm.rcaspName        mustEqual "Timmy Jimmison"
        vm.userBusinessNameOpt mustBe None
        vm.form             mustEqual formProvider("removeUserAccess.error.required.individual")
      }

      "must return individual wording even when removing an organisation RCASP" in {
        val userInfo =
          UserBusinessSubscriptionData(hasOrganisationContactDetails = false, organisationName = None)
        val vm       = RemoveUserAccessViewModel.from(otherOrgDetails, userInfo, formProvider)

        vm.titleKey         mustEqual "removeUserAccess.title.individual"
        vm.headingKey       mustEqual "removeUserAccess.heading.individual"
        vm.rcaspName        mustEqual "Timmy Ltd"
        vm.userBusinessNameOpt mustBe None
        vm.form             mustEqual formProvider("removeUserAccess.error.required.individual")
      }
    }

    "rcaspIsUser scenario" - {

      "must return correct keys and no userBusinessName" in {
        val userInfo =
          UserBusinessSubscriptionData(hasOrganisationContactDetails = true, organisationName = Some("My Business"))
        val vm       = RemoveUserAccessViewModel.from(rcaspIsUserDetails, userInfo, formProvider)

        vm.titleKey         mustEqual "removeUserAccess.title.rcaspIsUser"
        vm.headingKey       mustEqual "removeUserAccess.heading.rcaspIsUser"
        vm.rcaspName        mustEqual testOrgName
        vm.userBusinessNameOpt mustBe None
        vm.form             mustEqual formProvider("removeUserAccess.error.required.rcaspIsUser")
      }
    }

    "otherOrg scenario" - {

      "must return correct keys and user business name when provided" in {
        val userInfo =
          UserBusinessSubscriptionData(hasOrganisationContactDetails = true, organisationName = Some("My Business"))
        val vm       = RemoveUserAccessViewModel.from(otherOrgDetails, userInfo, formProvider)

        vm.titleKey         mustEqual "removeUserAccess.title.otherOrg"
        vm.headingKey       mustEqual "removeUserAccess.heading.otherOrg"
        vm.rcaspName        mustEqual testOrgName
        vm.userBusinessNameOpt mustBe Some("My Business")
        vm.form             mustEqual formProvider("removeUserAccess.error.required.otherOrg")
      }

      "must return fallback business name when missing from API" in {
        val userInfo =
          UserBusinessSubscriptionData(hasOrganisationContactDetails = true, organisationName = None)

        val vm = RemoveUserAccessViewModel.from(otherOrgDetails, userInfo, formProvider)

        vm.titleKey         mustEqual "removeUserAccess.title.otherOrg"
        vm.headingKey       mustEqual "removeUserAccess.heading.otherOrg"
        vm.rcaspName        mustEqual testOrgName
        vm.userBusinessNameOpt mustBe Some(messages("homePage.contactDetails.org.fallbackBusinessName"))
        vm.form             mustEqual formProvider("removeUserAccess.error.required.otherOrg")
      }
    }
  }
}
