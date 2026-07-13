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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import pages.remove.{RemoveOtherAccessPage, RemoveRcaspCachedDetails}
import play.api.test.FakeRequest
import play.api.test.Helpers.*

import scala.concurrent.Future

class RemoveOtherAccessControllerSpec extends SpecBase {

  lazy val onPageLoadRoute: String = controllers.remove.routes.RemoveOtherAccessController.onPageLoad(rcaspId).url

  lazy val onSubmitRoute: String = controllers.remove.routes.RemoveOtherAccessController.onSubmit(rcaspId).url

  private val individualDetails: RcaspDetails =
    individualRcaspDetailsResponse.copy(RCASPID = rcaspId, IsRCASPUser = false)

  private val rcaspIsUserDetails: RcaspDetails =
    organisationRcaspDetailsResponse.copy(RCASPID = rcaspId, IsRCASPUser = true)

  private val otherOrgDetails: RcaspDetails =
    organisationRcaspDetailsResponse.copy(RCASPID = rcaspId, IsRCASPUser = false)

  "RemoveOtherAccessController" - {

    "onPageLoad" - {

      "must return OK for an individual RCASP" in {
        val userAnswers = emptyUserAnswers.withPage(RemoveRcaspCachedDetails, individualDetails)
        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, onPageLoadRoute)
          val result  = route(application, request).value

          status(result) mustEqual OK
        }
      }

      "must return OK for a rcaspIsUser RCASP" in {
        val userAnswers = emptyUserAnswers.withPage(RemoveRcaspCachedDetails, rcaspIsUserDetails)
        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, onPageLoadRoute)
          val result  = route(application, request).value

          status(result) mustEqual OK
        }
      }

      "must return OK for an otherOrg RCASP" in {
        val userAnswers = emptyUserAnswers.withPage(RemoveRcaspCachedDetails, otherOrgDetails)
        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, onPageLoadRoute)
          val result  = route(application, request).value

          status(result) mustEqual OK
        }
      }

      "must populate the view correctly on GET when question has previously been answered" in {
        val userAnswers = emptyUserAnswers
          .withPage(RemoveRcaspCachedDetails, rcaspIsUserDetails)
          .withPage(RemoveOtherAccessPage, true)

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, onPageLoadRoute)
          val result  = route(application, request).value

          status(result) mustEqual OK
        }
      }

      "must redirect to Journey Recovery when RemoveRcaspCachedDetails not in UserAnswers" in {
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, onPageLoadRoute)
          val result  = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery for a GET if no existing data is found" in {
        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(GET, onPageLoadRoute)
          val result  = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery when cached rcaspId does not match URL rcaspId" in {
        val differentDetails = organisationRcaspDetailsResponse.copy(RCASPID = "DIFFERENT-ID", IsRCASPUser = true)
        val userAnswers      = emptyUserAnswers.withPage(RemoveRcaspCachedDetails, differentDetails)
        val application      = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, onPageLoadRoute)
          val result  = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    "onSubmit" - {

      "must redirect to next page when valid data is submitted" in {
        when(mockSessionRepository.set(any()))
          .thenReturn(Future.successful(true))

        val userAnswers = emptyUserAnswers.withPage(RemoveRcaspCachedDetails, rcaspIsUserDetails)
        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request =
            FakeRequest(POST, onSubmitRoute)
              .withFormUrlEncodedBody(("value", "true"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
        }
      }

      "must return BadRequest when invalid data is submitted" in {
        val userAnswers = emptyUserAnswers.withPage(RemoveRcaspCachedDetails, rcaspIsUserDetails)
        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request =
            FakeRequest(POST, onSubmitRoute)
              .withFormUrlEncodedBody(("value", ""))

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
        }
      }

      "must redirect to Journey Recovery when RemoveRcaspCachedDetails not in UserAnswers" in {
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request =
            FakeRequest(POST, onSubmitRoute)
              .withFormUrlEncodedBody(("value", "true"))

          val result = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery for a POST if no existing data is found" in {
        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request =
            FakeRequest(POST, onSubmitRoute)
              .withFormUrlEncodedBody(("value", "true"))

          val result = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery when cached rcaspId does not match URL rcaspId" in {
        val differentDetails = organisationRcaspDetailsResponse.copy(RCASPID = "DIFFERENT-ID", IsRCASPUser = true)
        val userAnswers      = emptyUserAnswers.withPage(RemoveRcaspCachedDetails, differentDetails)
        val application      = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request =
            FakeRequest(POST, onSubmitRoute)
              .withFormUrlEncodedBody(("value", "true"))

          val result = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }
  }
}
