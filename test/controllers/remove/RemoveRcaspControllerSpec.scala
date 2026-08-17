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
import forms.GenericYesNoPageFormProvider
import models.errors.ApiError.InternalServerError
import models.viewAndUpdateRcasp.RcaspDetails
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{never, reset, times, verify, when}
import pages.SubmissionSucceededPage
import pages.remove.*
import play.api.data.FormBinding.Implicits.formBinding
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.{AuditService, RcaspSubmissionService}
import types.ResultT
import views.html.remove.RemoveRcaspView

import java.time.{Clock, Instant}
import scala.concurrent.Future

class RemoveRcaspControllerSpec extends SpecBase {

  lazy val onPageLoadRoute: String = controllers.remove.routes.RemoveRcaspController.onPageLoad().url
  lazy val onSubmitRoute: String   = controllers.remove.routes.RemoveRcaspController.onSubmit().url

  private val formProvider = new GenericYesNoPageFormProvider()
  private val form         = formProvider("removeRcasp.error.required")

  val mockRcaspSubmissionService: RcaspSubmissionService = mock[RcaspSubmissionService]

  val mockAuditService: AuditService = mock[AuditService]

  private val rcaspDetails: RcaspDetails =
    organisationRcaspDetailsViewUpdate.copy(RCASPID = rcaspId, IsRCASPUser = true)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockRcaspSubmissionService, mockAuditService)
  }

  private def buildApplication(userAnswers: Option[models.UserAnswers]) =
    applicationBuilder(userAnswers = userAnswers)
      .overrides(
        bind[RcaspSubmissionService].toInstance(mockRcaspSubmissionService),
        bind[AuditService].toInstance(mockAuditService),
        bind[Clock].toInstance(clock)
      )
      .build()

  "RemoveRcaspController" - {

    "onPageLoad" - {

      "must return OK and render the view with otherAccessAnswer = true when RemoveOtherAccessPage is true" in {
        val userAnswers = emptyUserAnswers
          .withPage(RemoveRcaspCachedDetails, rcaspDetails)
          .withPage(RemoveOtherAccessPage, true)

        val application = buildApplication(Some(userAnswers))

        running(application) {
          val request = FakeRequest(GET, onPageLoadRoute)
          val result  = route(application, request).value

          val view = application.injector.instanceOf[RemoveRcaspView]

          status(result)          mustEqual OK
          contentAsString(result) mustEqual view(
            form,
            true,
            rcaspDetails.getName
          )(request, messages(application)).toString
        }
      }

      "must return OK and render the view with otherAccessAnswer = false when RemoveOtherAccessPage is false" in {
        val userAnswers = emptyUserAnswers
          .withPage(RemoveRcaspCachedDetails, rcaspDetails)
          .withPage(RemoveOtherAccessPage, false)

        val application = buildApplication(Some(userAnswers))

        running(application) {
          val request = FakeRequest(GET, onPageLoadRoute)
          val result  = route(application, request).value

          val view = application.injector.instanceOf[RemoveRcaspView]

          status(result)          mustEqual OK
          contentAsString(result) mustEqual view(
            form,
            false,
            rcaspDetails.getName
          )(request, messages(application)).toString
        }
      }

      "must populate the view with a filled form when RemoveRcaspPage has previously been answered" in {
        val userAnswers = emptyUserAnswers
          .withPage(RemoveRcaspCachedDetails, rcaspDetails)
          .withPage(RemoveOtherAccessPage, false)
          .withPage(RemoveRcaspPage, true)

        val application = buildApplication(Some(userAnswers))

        running(application) {
          val request = FakeRequest(GET, onPageLoadRoute)
          val result  = route(application, request).value

          val view = application.injector.instanceOf[RemoveRcaspView]

          status(result)          mustEqual OK
          contentAsString(result) mustEqual view(
            form.fill(true),
            false,
            rcaspDetails.getName
          )(request, messages(application)).toString
        }
      }

      "must redirect to Journey Recovery when RemoveRcaspCachedDetails not in UserAnswers" in {
        val userAnswers = emptyUserAnswers.withPage(RemoveOtherAccessPage, false)

        val application = buildApplication(Some(userAnswers))

        running(application) {
          val request = FakeRequest(GET, onPageLoadRoute)
          val result  = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery when RemoveOtherAccessPage not in UserAnswers" in {
        val userAnswers = emptyUserAnswers.withPage(RemoveRcaspCachedDetails, rcaspDetails)

        val application = buildApplication(Some(userAnswers))

        running(application) {
          val request = FakeRequest(GET, onPageLoadRoute)
          val result  = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery for a GET if no existing data is found" in {
        val application = buildApplication(None)

        running(application) {
          val request = FakeRequest(GET, onPageLoadRoute)
          val result  = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    "onSubmit" - {

      "must save the answer, redirect to Your RCASPs, and never call removeRcasp when 'No' is submitted" in {
        when(mockSessionRepository.set(any()))
          .thenReturn(Future.successful(true))

        val userAnswers = emptyUserAnswers
          .withPage(RemoveRcaspCachedDetails, rcaspDetails)
          .withPage(RemoveUserAccessPage, true)
          .withPage(RemoveOtherAccessPage, false)

        val application = buildApplication(Some(userAnswers))

        running(application) {
          val request =
            FakeRequest(POST, onSubmitRoute)
              .withFormUrlEncodedBody(("value", "false"))

          val result = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.YourRcaspsController.onPageLoad().url

          verify(mockRcaspSubmissionService, never).removeRcasp(any(), any())(any(), any())
          verify(mockSessionRepository).set(eqTo(userAnswers.withPage(RemoveRcaspPage, false)))
        }
      }

      "must call removeRcasp with the cached RCASPID, update the session, and redirect to RcaspRemovedController when 'Yes' is submitted and the call succeeds" in {
        when(mockRcaspSubmissionService.removeRcasp(any(), any())(any(), any()))
          .thenReturn(ResultT.fromValue(()))

        when(mockSessionRepository.set(any()))
          .thenReturn(Future.successful(true))

        when(mockAuditService.auditRemoveRcasp(any(), any(), any())(any()))
          .thenReturn(ResultT.fromValue(()))

        val userAnswers = emptyUserAnswers
          .withPage(RemoveRcaspCachedDetails, rcaspDetails)
          .withPage(RemoveUserAccessPage, true)
          .withPage(RemoveOtherAccessPage, false)

        val application = buildApplication(Some(userAnswers))

        running(application) {
          val request =
            FakeRequest(POST, onSubmitRoute)
              .withFormUrlEncodedBody(("value", "true"))

          val result = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual
            controllers.remove.routes.RcaspRemovedController.onPageLoad().url

          verify(mockRcaspSubmissionService).removeRcasp(eqTo(testCarfId), eqTo(rcaspDetails.RCASPID))(any(), any())
          verify(mockAuditService, times(1)).auditRemoveRcasp(any(), any(), any())(any())

          val expectedAnswers = userAnswers
            .withPage(RemoveRcaspPage, true)
            .withPage(SubmissionSucceededPage, true)
            .withPage(RcaspRemovedDateTimePage, Instant.now(clock))

          verify(mockSessionRepository).set(eqTo(expectedAnswers))
        }
      }

      "must redirect to Journey Recovery and not update the session when 'Yes' is submitted but removeRcasp fails" in {
        when(mockRcaspSubmissionService.removeRcasp(any(), any())(any(), any()))
          .thenReturn(ResultT.fromError(InternalServerError))

        val userAnswers = emptyUserAnswers
          .withPage(RemoveRcaspCachedDetails, rcaspDetails)
          .withPage(RemoveUserAccessPage, true)
          .withPage(RemoveOtherAccessPage, false)

        val application = buildApplication(Some(userAnswers))

        running(application) {
          val request =
            FakeRequest(POST, onSubmitRoute)
              .withFormUrlEncodedBody(("value", "true"))

          val result = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

          verify(mockRcaspSubmissionService).removeRcasp(any(), any())(any(), any())
          verify(mockSessionRepository, never).set(any())
        }
      }

      "must return BadRequest and render the view with errors when invalid data is submitted" in {
        val userAnswers = emptyUserAnswers
          .withPage(RemoveRcaspCachedDetails, rcaspDetails)
          .withPage(RemoveUserAccessPage, true)
          .withPage(RemoveOtherAccessPage, true)

        val application = buildApplication(Some(userAnswers))

        running(application) {
          val request =
            FakeRequest(POST, onSubmitRoute)
              .withFormUrlEncodedBody(("value", ""))

          val result = route(application, request).value

          val view      = application.injector.instanceOf[RemoveRcaspView]
          val boundForm = form.bindFromRequest()(request, implicitly)

          status(result)          mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(
            boundForm,
            true,
            rcaspDetails.getName
          )(request, messages(application)).toString

          verify(mockRcaspSubmissionService, never).removeRcasp(any(), any())(any(), any())
        }
      }

      "must redirect to Journey Recovery when RemoveRcaspCachedDetails not in UserAnswers" in {
        val userAnswers = emptyUserAnswers
          .withPage(RemoveUserAccessPage, true)
          .withPage(RemoveOtherAccessPage, false)

        val application = buildApplication(Some(userAnswers))

        running(application) {
          val request =
            FakeRequest(POST, onSubmitRoute)
              .withFormUrlEncodedBody(("value", "true"))

          val result = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

          verify(mockRcaspSubmissionService, never).removeRcasp(any(), any())(any(), any())
        }
      }

      "must redirect to Journey Recovery when RemoveUserAccessPage not in UserAnswers" in {
        val userAnswers = emptyUserAnswers
          .withPage(RemoveRcaspCachedDetails, rcaspDetails)
          .withPage(RemoveOtherAccessPage, false)

        val application = buildApplication(Some(userAnswers))

        running(application) {
          val request =
            FakeRequest(POST, onSubmitRoute)
              .withFormUrlEncodedBody(("value", "true"))

          val result = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

          verify(mockRcaspSubmissionService, never).removeRcasp(any(), any())(any(), any())
        }
      }

      "when the audit service call returns an error the request should still be successful" in {
        when(mockRcaspSubmissionService.removeRcasp(any(), any())(any(), any()))
          .thenReturn(ResultT.fromValue(()))

        when(mockSessionRepository.set(any()))
          .thenReturn(Future.successful(true))

        when(mockAuditService.auditRemoveRcasp(any(), any(), any())(any()))
          .thenReturn(ResultT.fromError(InternalServerError))

        val userAnswers = emptyUserAnswers
          .withPage(RemoveRcaspCachedDetails, rcaspDetails)
          .withPage(RemoveUserAccessPage, true)
          .withPage(RemoveOtherAccessPage, false)

        val application = buildApplication(Some(userAnswers))

        running(application) {
          val request =
            FakeRequest(POST, onSubmitRoute)
              .withFormUrlEncodedBody(("value", "true"))

          val result = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual
            controllers.remove.routes.RcaspRemovedController.onPageLoad().url

          verify(mockRcaspSubmissionService).removeRcasp(eqTo(testCarfId), eqTo(rcaspDetails.RCASPID))(any(), any())
          verify(mockAuditService, times(1)).auditRemoveRcasp(any(), any(), any())(any())

          val expectedAnswers = userAnswers
            .withPage(RemoveRcaspPage, true)
            .withPage(SubmissionSucceededPage, true)
            .withPage(RcaspRemovedDateTimePage, Instant.now(clock))

          verify(mockSessionRepository).set(eqTo(expectedAnswers))
        }
      }

      "must redirect to Journey Recovery when RemoveOtherAccessPage not in UserAnswers" in {
        val userAnswers = emptyUserAnswers
          .withPage(RemoveRcaspCachedDetails, rcaspDetails)
          .withPage(RemoveUserAccessPage, true)

        val application = buildApplication(Some(userAnswers))

        running(application) {
          val request =
            FakeRequest(POST, onSubmitRoute)
              .withFormUrlEncodedBody(("value", "true"))

          val result = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

          verify(mockRcaspSubmissionService, never).removeRcasp(any(), any())(any(), any())
        }
      }

      "must redirect to Journey Recovery for a POST if no existing data is found" in {
        val application = buildApplication(None)

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
