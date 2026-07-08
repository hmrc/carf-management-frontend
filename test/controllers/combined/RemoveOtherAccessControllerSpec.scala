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

package controllers.combined

import base.SpecBase
import connectors.RcaspConnector
import models.errors.ApiError.InternalServerError
import models.NormalMode
import models.responses.*
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import pages.remove.RemoveOtherAccessPage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import types.ResultT

import scala.concurrent.Future

class RemoveOtherAccessControllerSpec extends SpecBase {

  lazy val onPageLoadRoute: String =
    controllers.remove.routes.RemoveOtherAccessController.onPageLoad(rcaspId).url

  lazy val onSubmitRoute: String =
    controllers.remove.routes.RemoveOtherAccessController.onSubmit(rcaspId).url

  val mockRcaspConnector: RcaspConnector = mock[RcaspConnector]

  private def viewResponse(rcaspDetails: RcaspDetails): ViewRcaspResponse =
    ViewRcaspResponse(
      ViewRCASP = ViewRcasp(
        ResponseCommon = rcaspResponseCommon,
        ResponseDetails = RcaspResponseDetails(RCASPList = List(rcaspDetails))
      )
    )

  private val individualResponse: ViewRcaspResponse =
    viewResponse(
      individualRcaspDetailsResponse.copy(RCASPID = rcaspId, IsRCASPUser = false)
    )

  private val rcaspIsUserResponse: ViewRcaspResponse =
    viewResponse(
      organisationRcaspDetailsResponse.copy(RCASPID = rcaspId, IsRCASPUser = true)
    )

  private val otherOrgResponse: ViewRcaspResponse =
    viewResponse(
      organisationRcaspDetailsResponse.copy(RCASPID = rcaspId, IsRCASPUser = false)
    )

  private val emptyResponse: ViewRcaspResponse =
    ViewRcaspResponse(
      ViewRCASP = ViewRcasp(
        ResponseCommon = rcaspResponseCommon,
        ResponseDetails = RcaspResponseDetails(RCASPList = List.empty)
      )
    )

  "RemoveOtherAccessController" - {

    "onPageLoad" - {

      "must return OK and the correct view for an individual RCASP" in {
        when(mockRcaspConnector.viewRcasp(any())(any(), any()))
          .thenReturn(ResultT.fromValue(individualResponse))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[RcaspConnector].toInstance(mockRcaspConnector))
          .build()

        running(application) {
          val request = FakeRequest(GET, onPageLoadRoute)
          val result  = route(application, request).value

          status(result) mustEqual OK
        }
      }

      "must return OK and the correct view for a rcaspIsUser RCASP" in {
        when(mockRcaspConnector.viewRcasp(any())(any(), any()))
          .thenReturn(ResultT.fromValue(rcaspIsUserResponse))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[RcaspConnector].toInstance(mockRcaspConnector))
          .build()

        running(application) {
          val request = FakeRequest(GET, onPageLoadRoute)
          val result  = route(application, request).value

          status(result) mustEqual OK
        }
      }

      "must return OK and the correct view for an otherOrg RCASP" in {
        when(mockRcaspConnector.viewRcasp(any())(any(), any()))
          .thenReturn(ResultT.fromValue(otherOrgResponse))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[RcaspConnector].toInstance(mockRcaspConnector))
          .build()

        running(application) {
          val request = FakeRequest(GET, onPageLoadRoute)
          val result  = route(application, request).value

          status(result) mustEqual OK
        }
      }

      "must populate the view correctly on GET when question has previously been answered" in {
        when(mockRcaspConnector.viewRcasp(any())(any(), any()))
          .thenReturn(ResultT.fromValue(rcaspIsUserResponse))

        val userAnswers = emptyUserAnswers.withPage(RemoveOtherAccessPage, true)

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[RcaspConnector].toInstance(mockRcaspConnector))
          .build()

        running(application) {
          val request = FakeRequest(GET, onPageLoadRoute)
          val result  = route(application, request).value

          status(result) mustEqual OK
        }
      }

      "must redirect to Journey Recovery when RCASP connector returns an error" in {
        when(mockRcaspConnector.viewRcasp(any())(any(), any()))
          .thenReturn(ResultT.fromError(InternalServerError))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[RcaspConnector].toInstance(mockRcaspConnector))
          .build()

        running(application) {
          val request = FakeRequest(GET, onPageLoadRoute)
          val result  = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery when rcaspId is not found in the list" in {
        when(mockRcaspConnector.viewRcasp(any())(any(), any()))
          .thenReturn(ResultT.fromValue(emptyResponse))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[RcaspConnector].toInstance(mockRcaspConnector))
          .build()

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
    }

    "onSubmit" - {

      "must redirect to next page when valid data is submitted" in {
        when(mockRcaspConnector.viewRcasp(any())(any(), any()))
          .thenReturn(ResultT.fromValue(rcaspIsUserResponse))

        when(mockSessionRepository.set(any()))
          .thenReturn(Future.successful(true))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[RcaspConnector].toInstance(mockRcaspConnector))
          .build()

        running(application) {
          val request =
            FakeRequest(POST, onSubmitRoute)
              .withFormUrlEncodedBody(("value", "true"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
        }
      }

      "must return BadRequest when invalid data is submitted" in {
        when(mockRcaspConnector.viewRcasp(any())(any(), any()))
          .thenReturn(ResultT.fromValue(rcaspIsUserResponse))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[RcaspConnector].toInstance(mockRcaspConnector))
          .build()

        running(application) {
          val request =
            FakeRequest(POST, onSubmitRoute)
              .withFormUrlEncodedBody(("value", ""))

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
        }
      }

      "must redirect to Journey Recovery when RCASP connector returns an error" in {
        when(mockRcaspConnector.viewRcasp(any())(any(), any()))
          .thenReturn(ResultT.fromError(InternalServerError))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[RcaspConnector].toInstance(mockRcaspConnector))
          .build()

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
    }
  }
}
