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
import models.viewAndUpdateRcasp.RcaspDetails

import java.time.Instant

class RcaspRemovedViewModelSpec extends SpecBase {

  private val rcaspDetails: RcaspDetails =
    organisationRcaspDetailsResponse.copy(RCASPID = rcaspId, IsRCASPUser = true)

  "RcaspRemovedViewModel.from" - {

    "must return the correct rcaspName and rcaspId" in {
      val removedAt = Instant.parse("2027-03-01T13:11:00Z")

      val vm = RcaspRemovedViewModel.from(rcaspDetails, removedAt)

      vm.rcaspName mustEqual testOrgName
      vm.rcaspId   mustEqual rcaspId
    }

    "must format an ordinary afternoon time correctly (matches ticket example)" in {
      val removedAt = Instant.parse("2027-03-01T13:11:00Z")

      val vm = RcaspRemovedViewModel.from(rcaspDetails, removedAt)

      vm.formattedDateTime mustEqual "1 March 2027 at 1:11pm"
    }

    "must display 'midnight' with no minutes when time is 00:00" in {
      val removedAt = Instant.parse("2027-01-01T00:00:00Z")

      val vm = RcaspRemovedViewModel.from(rcaspDetails, removedAt)

      vm.formattedDateTime mustEqual "1 January 2027 at midnight"
    }

    "must display 'midday' with no minutes when time is 12:00" in {
      val removedAt = Instant.parse("2027-01-01T12:00:00Z")

      val vm = RcaspRemovedViewModel.from(rcaspDetails, removedAt)

      vm.formattedDateTime mustEqual "1 January 2027 at midday"
    }

    "must format a morning time with a leading-zero minute and no leading-zero hour" in {
      val removedAt = Instant.parse("2027-01-05T09:07:00Z")

      val vm = RcaspRemovedViewModel.from(rcaspDetails, removedAt)

      vm.formattedDateTime mustEqual "5 January 2027 at 9:07am"
    }

    "must format a time just before midnight as pm with hour converted from 24hr format" in {
      val removedAt = Instant.parse("2027-01-01T23:59:00Z")

      val vm = RcaspRemovedViewModel.from(rcaspDetails, removedAt)

      vm.formattedDateTime mustEqual "1 January 2027 at 11:59pm"
    }

    "must format a time just after midday as pm" in {
      val removedAt = Instant.parse("2027-01-01T12:01:00Z")

      val vm = RcaspRemovedViewModel.from(rcaspDetails, removedAt)

      vm.formattedDateTime mustEqual "1 January 2027 at 12:01pm"
    }

    "must format a time just after midnight as am" in {
      val removedAt = Instant.parse("2027-01-01T00:01:00Z")

      val vm = RcaspRemovedViewModel.from(rcaspDetails, removedAt)

      vm.formattedDateTime mustEqual "1 January 2027 at 12:01am"
    }

    "must not include a leading zero on the day of the month" in {
      val removedAt = Instant.parse("2027-01-09T10:00:00Z")

      val vm = RcaspRemovedViewModel.from(rcaspDetails, removedAt)

      vm.formattedDateTime must startWith("9 January 2027")
    }

    "must convert UTC to UK local time correctly during British Summer Time (BST, UTC+1)" in {
      val removedAt = Instant.parse("2026-07-17T09:14:00Z")

      val vm = RcaspRemovedViewModel.from(rcaspDetails, removedAt)

      vm.formattedDateTime mustEqual "17 July 2026 at 10:14am"
    }
  }
}
