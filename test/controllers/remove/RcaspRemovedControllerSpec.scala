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

package controllers.remove

import base.SpecBase
import models.viewAndUpdateRcasp.RcaspDetails
import pages.SubmissionSucceededPage
import pages.remove.{RemoveRcaspCachedDetails, RemoveRcaspRemovedDateTimePage}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import viewmodels.remove.RcaspRemovedViewModel
import views.html.remove.RcaspRemovedView

import java.time.Instant

class RcaspRemovedControllerSpec extends SpecBase {

  lazy val onPageLoadRoute: String = controllers.remove.routes.RcaspRemovedController.onPageLoad(rcaspId).url

  private val rcaspDetails: RcaspDetails =
    organisationRcaspDetailsResponse.copy(RCASPID = rcaspId, IsRCASPUser = true)

  private val removedAt: Instant = Instant.parse("2027-03-01T13:11:00Z")

  private val pageUnavailableUrl: String =
    controllers.routes.PlaceholderController.onPageLoad("/problem/page-unavailable (CARF-536)").url

  "RcaspRemovedController" - {

    "onPageLoad" - {

      "must return OK and render the correct view when all required data is present" in {
        val userAnswers = emptyUserAnswers
          .withPage(SubmissionSucceededPage, true)
          .withPage(RemoveRcaspCachedDetails, rcaspDetails)
          .withPage(RemoveRcaspRemovedDateTimePage, removedAt.toString)

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, onPageLoadRoute)
          val result  = route(application, request).value

          val view = application.injector.instanceOf[RcaspRemovedView]
          val vm   = RcaspRemovedViewModel.from(rcaspDetails, removedAt)

          status(result)          mustEqual OK
          contentAsString(result) mustEqual view(
            rcaspName = vm.rcaspName,
            rcaspId = vm.rcaspId,
            formattedDateTime = vm.formattedDateTime,
            manageRcaspsCall = controllers.routes.YourRcaspsController.onPageLoad(),
            manageReportsCall = controllers.home.routes.HomePageController.onPageLoad()
          )(request, messages(application)).toString
        }
      }

      "must redirect to page-unavailable placeholder when SubmissionSucceededPage is not set" in {
        val userAnswers = emptyUserAnswers
          .withPage(RemoveRcaspCachedDetails, rcaspDetails)
          .withPage(RemoveRcaspRemovedDateTimePage, removedAt.toString)

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, onPageLoadRoute)
          val result  = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual pageUnavailableUrl
        }
      }

      "must redirect to page-unavailable placeholder when SubmissionSucceededPage is false" in {
        val userAnswers = emptyUserAnswers
          .withPage(SubmissionSucceededPage, false)
          .withPage(RemoveRcaspCachedDetails, rcaspDetails)
          .withPage(RemoveRcaspRemovedDateTimePage, removedAt.toString)

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, onPageLoadRoute)
          val result  = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual pageUnavailableUrl
        }
      }

      "must redirect to page-unavailable placeholder when RemoveRcaspCachedDetails is missing" in {
        val userAnswers = emptyUserAnswers
          .withPage(SubmissionSucceededPage, true)
          .withPage(RemoveRcaspRemovedDateTimePage, removedAt.toString)

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, onPageLoadRoute)
          val result  = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual pageUnavailableUrl
        }
      }

      "must redirect to page-unavailable placeholder when cached rcaspId does not match URL rcaspId" in {
        val differentDetails = organisationRcaspDetailsResponse.copy(RCASPID = "DIFFERENT-ID", IsRCASPUser = true)

        val userAnswers = emptyUserAnswers
          .withPage(SubmissionSucceededPage, true)
          .withPage(RemoveRcaspCachedDetails, differentDetails)
          .withPage(RemoveRcaspRemovedDateTimePage, removedAt.toString)

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, onPageLoadRoute)
          val result  = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual pageUnavailableUrl
        }
      }

      "must redirect to page-unavailable placeholder when RemoveRcaspRemovedDateTimePage is missing" in {
        val userAnswers = emptyUserAnswers
          .withPage(SubmissionSucceededPage, true)
          .withPage(RemoveRcaspCachedDetails, rcaspDetails)

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, onPageLoadRoute)
          val result  = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual pageUnavailableUrl
        }
      }

      "must redirect to page-unavailable placeholder when RemoveRcaspRemovedDateTimePage cannot be parsed as an Instant" in {
        val userAnswers = emptyUserAnswers
          .withPage(SubmissionSucceededPage, true)
          .withPage(RemoveRcaspCachedDetails, rcaspDetails)
          .withPage(RemoveRcaspRemovedDateTimePage, "not-a-valid-instant")

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, onPageLoadRoute)
          val result  = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual pageUnavailableUrl
        }
      }

      "must redirect to page-unavailable placeholder when no userAnswers exist" in {
        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(GET, onPageLoadRoute)
          val result  = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual pageUnavailableUrl
        }
      }
    }
  }
}
