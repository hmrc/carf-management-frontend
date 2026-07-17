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
import org.mockito.Mockito.{never, reset, verify, when}
import pages.SubmissionSucceededPage
import pages.remove.{RemoveOtherAccessPage, RemoveRcaspCachedDetails}
import play.api.data.FormBinding.Implicits.formBinding
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.AccountService
import types.ResultT
import viewmodels.remove.RemoveRcaspViewModel
import views.html.remove.RemoveRcaspView

import scala.concurrent.Future

class RemoveRcaspControllerSpec extends SpecBase {

  lazy val onPageLoadRoute: String = controllers.remove.routes.RemoveRcaspController.onPageLoad(rcaspId).url
  lazy val onSubmitRoute: String   = controllers.remove.routes.RemoveRcaspController.onSubmit(rcaspId).url

  private val formProvider = new GenericYesNoPageFormProvider()

  val mockAccountService: AccountService = mock[AccountService]

  private val rcaspDetails: RcaspDetails =
    organisationRcaspDetailsResponse.copy(RCASPID = rcaspId, IsRCASPUser = true)

  private val pageUnavailableUrl: String =
    controllers.routes.PlaceholderController.onPageLoad("/problem/page-unavailable (CARF-536)").url

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAccountService)
  }

  "RemoveRcaspController" - {

    "onPageLoad" - {

      "must return OK and render the view with the otherAccess wording when RemoveOtherAccessPage is true" in {
        val userAnswers = emptyUserAnswers
          .withPage(RemoveRcaspCachedDetails, rcaspDetails)
          .withPage(RemoveOtherAccessPage, true)

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[AccountService].toInstance(mockAccountService))
          .build()

        running(application) {
          val request = FakeRequest(GET, onPageLoadRoute)
          val result  = route(application, request).value

          val view = application.injector.instanceOf[RemoveRcaspView]
          val vm   = RemoveRcaspViewModel.from(rcaspDetails, otherAccessAnswer = true, formProvider)

          status(result)          mustEqual OK
          contentAsString(result) mustEqual view(
            vm.form,
            rcaspId,
            vm.titleKey,
            vm.headingKey,
            vm.rcaspName
          )(request, messages(application)).toString
        }
      }

      "must return OK and render the view with the default wording when RemoveOtherAccessPage is false" in {
        val userAnswers = emptyUserAnswers
          .withPage(RemoveRcaspCachedDetails, rcaspDetails)
          .withPage(RemoveOtherAccessPage, false)

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[AccountService].toInstance(mockAccountService))
          .build()

        running(application) {
          val request = FakeRequest(GET, onPageLoadRoute)
          val result  = route(application, request).value

          val view = application.injector.instanceOf[RemoveRcaspView]
          val vm   = RemoveRcaspViewModel.from(rcaspDetails, otherAccessAnswer = false, formProvider)

          status(result)          mustEqual OK
          contentAsString(result) mustEqual view(
            vm.form,
            rcaspId,
            vm.titleKey,
            vm.headingKey,
            vm.rcaspName
          )(request, messages(application)).toString
        }
      }

      "must redirect to page-unavailable placeholder when SubmissionSucceededPage is true" in {
        val userAnswers = emptyUserAnswers
          .withPage(RemoveRcaspCachedDetails, rcaspDetails)
          .withPage(RemoveOtherAccessPage, false)
          .withPage(SubmissionSucceededPage, true)

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[AccountService].toInstance(mockAccountService))
          .build()

        running(application) {
          val request = FakeRequest(GET, onPageLoadRoute)
          val result  = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual pageUnavailableUrl
        }
      }

      "must redirect to Journey Recovery when RemoveRcaspCachedDetails not in UserAnswers" in {
        val userAnswers = emptyUserAnswers.withPage(RemoveOtherAccessPage, false)

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[AccountService].toInstance(mockAccountService))
          .build()

        running(application) {
          val request = FakeRequest(GET, onPageLoadRoute)
          val result  = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery when RemoveOtherAccessPage not in UserAnswers" in {
        val userAnswers = emptyUserAnswers.withPage(RemoveRcaspCachedDetails, rcaspDetails)

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[AccountService].toInstance(mockAccountService))
          .build()

        running(application) {
          val request = FakeRequest(GET, onPageLoadRoute)
          val result  = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery when cached rcaspId does not match URL rcaspId" in {
        val differentDetails = organisationRcaspDetailsResponse.copy(RCASPID = "DIFFERENT-ID", IsRCASPUser = true)
        val userAnswers      = emptyUserAnswers
          .withPage(RemoveRcaspCachedDetails, differentDetails)
          .withPage(RemoveOtherAccessPage, false)

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[AccountService].toInstance(mockAccountService))
          .build()

        running(application) {
          val request = FakeRequest(GET, onPageLoadRoute)
          val result  = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery for a GET if no existing data is found" in {
        val application = applicationBuilder(userAnswers = None)
          .overrides(bind[AccountService].toInstance(mockAccountService))
          .build()

        running(application) {
          val request = FakeRequest(GET, onPageLoadRoute)
          val result  = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    "onSubmit" - {

      "must redirect to the homepage when 'No' is submitted, without calling removeRcasp" in {
        val userAnswers = emptyUserAnswers
          .withPage(RemoveRcaspCachedDetails, rcaspDetails)
          .withPage(RemoveOtherAccessPage, false)

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[AccountService].toInstance(mockAccountService))
          .build()

        running(application) {
          val request =
            FakeRequest(POST, onSubmitRoute)
              .withFormUrlEncodedBody(("value", "false"))

          val result = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.YourRcaspsController.onPageLoad().url

          verify(mockAccountService, never).removeRcasp(any(), any())(any(), any())
        }
      }

      "must call removeRcasp, update the session, and redirect to RcaspRemovedController when 'Yes' is submitted and the call succeeds" in {
        when(mockAccountService.removeRcasp(any(), any())(any(), any()))
          .thenReturn(ResultT.fromValue(()))

        when(mockSessionRepository.set(any()))
          .thenReturn(Future.successful(true))

        val userAnswers = emptyUserAnswers
          .withPage(RemoveRcaspCachedDetails, rcaspDetails)
          .withPage(RemoveOtherAccessPage, false)

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[AccountService].toInstance(mockAccountService))
          .build()

        running(application) {
          val request =
            FakeRequest(POST, onSubmitRoute)
              .withFormUrlEncodedBody(("value", "true"))

          val result = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual
            controllers.remove.routes.RcaspRemovedController.onPageLoad(rcaspId).url

          verify(mockAccountService).removeRcasp(eqTo(testCarfId), eqTo(rcaspId))(any(), any())
          verify(mockSessionRepository).set(any())
        }
      }

      "must redirect to Journey Recovery when 'Yes' is submitted but removeRcasp fails" in {
        when(mockAccountService.removeRcasp(any(), any())(any(), any()))
          .thenReturn(ResultT.fromError(InternalServerError))

        val userAnswers = emptyUserAnswers
          .withPage(RemoveRcaspCachedDetails, rcaspDetails)
          .withPage(RemoveOtherAccessPage, false)

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[AccountService].toInstance(mockAccountService))
          .build()

        running(application) {
          val request =
            FakeRequest(POST, onSubmitRoute)
              .withFormUrlEncodedBody(("value", "true"))

          val result = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

          verify(mockAccountService).removeRcasp(any(), any())(any(), any())
          verify(mockSessionRepository, never).set(any())
        }
      }

      "must return BadRequest and render the view with errors when invalid data is submitted" in {
        val userAnswers = emptyUserAnswers
          .withPage(RemoveRcaspCachedDetails, rcaspDetails)
          .withPage(RemoveOtherAccessPage, true)

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[AccountService].toInstance(mockAccountService))
          .build()

        running(application) {
          val request =
            FakeRequest(POST, onSubmitRoute)
              .withFormUrlEncodedBody(("value", ""))

          val result = route(application, request).value

          val view      = application.injector.instanceOf[RemoveRcaspView]
          val vm        = RemoveRcaspViewModel.from(rcaspDetails, otherAccessAnswer = true, formProvider)
          val boundForm = vm.form.bindFromRequest()(request, implicitly)

          status(result)          mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(
            boundForm,
            rcaspId,
            vm.titleKey,
            vm.headingKey,
            vm.rcaspName
          )(request, messages(application)).toString

          verify(mockAccountService, never).removeRcasp(any(), any())(any(), any())
        }
      }

      "must redirect to Journey Recovery when RemoveRcaspCachedDetails not in UserAnswers" in {
        val userAnswers = emptyUserAnswers.withPage(RemoveOtherAccessPage, false)

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[AccountService].toInstance(mockAccountService))
          .build()

        running(application) {
          val request =
            FakeRequest(POST, onSubmitRoute)
              .withFormUrlEncodedBody(("value", "true"))

          val result = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

          verify(mockAccountService, never).removeRcasp(any(), any())(any(), any())
        }
      }

      "must redirect to Journey Recovery when RemoveOtherAccessPage not in UserAnswers" in {
        val userAnswers = emptyUserAnswers.withPage(RemoveRcaspCachedDetails, rcaspDetails)

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[AccountService].toInstance(mockAccountService))
          .build()

        running(application) {
          val request =
            FakeRequest(POST, onSubmitRoute)
              .withFormUrlEncodedBody(("value", "true"))

          val result = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

          verify(mockAccountService, never).removeRcasp(any(), any())(any(), any())
        }
      }

      "must redirect to Journey Recovery when cached rcaspId does not match URL rcaspId" in {
        val differentDetails = organisationRcaspDetailsResponse.copy(RCASPID = "DIFFERENT-ID", IsRCASPUser = true)
        val userAnswers      = emptyUserAnswers
          .withPage(RemoveRcaspCachedDetails, differentDetails)
          .withPage(RemoveOtherAccessPage, false)

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[AccountService].toInstance(mockAccountService))
          .build()

        running(application) {
          val request =
            FakeRequest(POST, onSubmitRoute)
              .withFormUrlEncodedBody(("value", "true"))

          val result = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

          verify(mockAccountService, never).removeRcasp(any(), any())(any(), any())
        }
      }

      "must redirect to Journey Recovery for a POST if no existing data is found" in {
        val application = applicationBuilder(userAnswers = None)
          .overrides(bind[AccountService].toInstance(mockAccountService))
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
    }
  }
}
