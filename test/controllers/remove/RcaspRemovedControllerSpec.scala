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
import config.Constants.ukZoneId
import models.viewAndUpdateRcasp.RcaspDetails
import pages.SubmissionSucceededPage
import pages.remove.{RcaspRemovedDateTimePage, RemoveRcaspCachedDetails}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import utils.DateTimeFormats
import views.html.remove.RcaspRemovedView

import java.time.Instant

class RcaspRemovedControllerSpec extends SpecBase {

  lazy val onPageLoadRoute: String = controllers.remove.routes.RcaspRemovedController.onPageLoad().url

  private val rcaspDetails: RcaspDetails =
    organisationRcaspDetailsResponse.copy(RCASPID = rcaspId, IsRCASPUser = true)

  private val removedAt: Instant = Instant.parse("2027-03-01T13:11:00Z")

  private val journeyRecoveryUrl: String = controllers.routes.JourneyRecoveryController.onPageLoad().url

  "RcaspRemovedController" - {

    "onPageLoad" - {

      "must return OK and render the correct view when all required data is present" in {
        val userAnswers = emptyUserAnswers
          .withPage(SubmissionSucceededPage, true)
          .withPage(RemoveRcaspCachedDetails, rcaspDetails)
          .withPage(RcaspRemovedDateTimePage, removedAt)

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, onPageLoadRoute)
          val result  = route(application, request).value

          val view = application.injector.instanceOf[RcaspRemovedView]

          val datetime = removedAt.atZone(ukZoneId)

          status(result)          mustEqual OK
          contentAsString(result) mustEqual view(
            rcaspName = rcaspDetails.getName,
            rcaspId = rcaspDetails.RCASPID,
            formattedDate = DateTimeFormats.formatDate(datetime.toLocalDate),
            formattedTime = DateTimeFormats.formatTime(datetime.toLocalTime)
          )(request, messages(application)).toString
        }
      }

      "must redirect to Journey Recovery when SubmissionSucceededPage is not set" in {
        val userAnswers = emptyUserAnswers
          .withPage(RemoveRcaspCachedDetails, rcaspDetails)
          .withPage(RcaspRemovedDateTimePage, removedAt)

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, onPageLoadRoute)
          val result  = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual journeyRecoveryUrl
        }
      }

      "must redirect to Journey Recovery when SubmissionSucceededPage is false" in {
        val userAnswers = emptyUserAnswers
          .withPage(SubmissionSucceededPage, false)
          .withPage(RemoveRcaspCachedDetails, rcaspDetails)
          .withPage(RcaspRemovedDateTimePage, removedAt)

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, onPageLoadRoute)
          val result  = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual journeyRecoveryUrl
        }
      }

      "must redirect to Journey Recovery when RemoveRcaspCachedDetails is missing" in {
        val userAnswers = emptyUserAnswers
          .withPage(SubmissionSucceededPage, true)
          .withPage(RcaspRemovedDateTimePage, removedAt)

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, onPageLoadRoute)
          val result  = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual journeyRecoveryUrl
        }
      }

      "must redirect to Journey Recovery when RcaspRemovedDateTimePage is missing" in {
        val userAnswers = emptyUserAnswers
          .withPage(SubmissionSucceededPage, true)
          .withPage(RemoveRcaspCachedDetails, rcaspDetails)

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, onPageLoadRoute)
          val result  = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual journeyRecoveryUrl
        }
      }

      "must redirect to Journey Recovery when no userAnswers exist" in {
        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(GET, onPageLoadRoute)
          val result  = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual journeyRecoveryUrl
        }
      }
    }
  }
}
