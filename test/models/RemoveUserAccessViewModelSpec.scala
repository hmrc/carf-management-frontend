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

package models

import base.SpecBase
import forms.GenericYesNoPageFormProvider
import models.responses.*
import org.scalatest.EitherValues.convertEitherToValuable
import viewmodels.remove.RemoveUserAccessViewModel
import models.viewAndUpdateRcasp.RcaspDetails

class RemoveUserAccessViewModelSpec extends SpecBase {

  val formProvider = new GenericYesNoPageFormProvider()

  private def viewResponse(rcaspDetails: RcaspDetails): ViewRcaspResponse =
    ViewRcaspResponse(
      ViewRCASP = ViewRcasp(
        ResponseCommon = rcaspResponseCommon,
        ResponseDetails = RcaspResponseDetails(RCASPList = List(rcaspDetails))
      )
    )

  private val individualResponse =
    viewResponse(individualRcaspDetailsResponse.copy(RCASPID = rcaspId, IsRCASPUser = false))

  private val rcaspIsUserResponse =
    viewResponse(organisationRcaspDetailsResponse.copy(RCASPID = rcaspId, IsRCASPUser = true))

  private val otherOrgResponse =
    viewResponse(organisationRcaspDetailsResponse.copy(RCASPID = rcaspId, IsRCASPUser = false))

  "RemoveUserAccessViewModel.from" - {

    "individual scenario" - {

      "must return correct keys and no userBusinessName" in {
        val result = RemoveUserAccessViewModel.from(
          rcaspId,
          individualResponse,
          Some("My Business"),
          formProvider
        )

        result.isRight mustBe true

        val vm = result.value

        vm.titleKey      mustEqual "removeUserAccess.title.individual"
        vm.headingKey    mustEqual "removeUserAccess.heading.individual"
        vm.errorKey      mustEqual "removeUserAccess.error.required.individual"
        vm.rcaspName     mustEqual "Penny Cassiopeia"
        vm.userBusinessName mustBe None
      }
    }

    "rcaspIsUser scenario" - {

      "must return correct keys and rcaspName as userBusinessName" in {
        val result = RemoveUserAccessViewModel.from(
          rcaspId,
          rcaspIsUserResponse,
          Some("My Business"),
          formProvider
        )

        result.isRight mustBe true

        val vm = result.value

        vm.titleKey      mustEqual "removeUserAccess.title.rcaspIsUser"
        vm.headingKey    mustEqual "removeUserAccess.heading.rcaspIsUser"
        vm.errorKey      mustEqual "removeUserAccess.error.required.rcaspIsUser"
        vm.rcaspName     mustEqual "Mesagoza"
        vm.userBusinessName mustBe Some("Mesagoza")
      }
    }

    "otherOrg scenario" - {

      "must return correct keys and user business name" in {
        val result = RemoveUserAccessViewModel.from(
          rcaspId,
          otherOrgResponse,
          Some("My Business"),
          formProvider
        )

        result.isRight mustBe true

        val vm = result.value

        vm.titleKey      mustEqual "removeUserAccess.title.otherOrg"
        vm.headingKey    mustEqual "removeUserAccess.heading.otherOrg"
        vm.errorKey      mustEqual "removeUserAccess.error.required.otherOrg"
        vm.rcaspName     mustEqual "Mesagoza"
        vm.userBusinessName mustBe Some("My Business")
      }

      "must return Left when user business name is missing" in {
        val result = RemoveUserAccessViewModel.from(
          rcaspId,
          otherOrgResponse,
          None,
          formProvider
        )

        result.isLeft mustBe true
      }
    }

    "must return Left when rcaspId is not found" in {
      val result = RemoveUserAccessViewModel.from(
        "unknown-rcasp-id",
        rcaspIsUserResponse,
        Some("My Business"),
        formProvider
      )

      result.isLeft mustBe true
    }
  }
}
