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
import models.errors.ApiError.NotFoundError
import models.viewAndUpdateRcasp.RcaspDetails
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, reset, verify, when}
import pages.remove.{RemoveRcaspCachedDetails, RemoveUserBusinessNameCached}
import play.api.data.FormBinding.Implicits.formBinding
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.AccountService
import types.ResultT
import viewmodels.remove.RemoveUserAccessViewModel
import views.html.remove.RemoveUserAccessView

import scala.concurrent.Future

class RemoveUserAccessControllerSpec extends SpecBase {

  lazy val onPageLoadRoute: String = controllers.remove.routes.RemoveUserAccessController.onPageLoad(rcaspId).url
  lazy val onSubmitRoute: String   = controllers.remove.routes.RemoveUserAccessController.onSubmit(rcaspId).url

  val mockAccountService: AccountService = mock[AccountService]

  private val formProvider = new GenericYesNoPageFormProvider()

  private val individualDetails: RcaspDetails =
    individualRcaspDetailsResponse.copy(RCASPID = rcaspId, IsRCASPUser = false)

  private val rcaspIsUserDetails: RcaspDetails =
    organisationRcaspDetailsResponse.copy(RCASPID = rcaspId, IsRCASPUser = true)

  private val otherOrgDetails: RcaspDetails =
    organisationRcaspDetailsResponse.copy(RCASPID = rcaspId, IsRCASPUser = false)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAccountService)
  }

  "RemoveUserAccessController" - {

    "onPageLoad" - {

      "cache miss" - {

        "must return OK and render the correct view for an individual RCASP" in {
          when(mockAccountService.getRcaspDetails(any(), any())(any(), any()))
            .thenReturn(ResultT.fromValue(individualDetails))

          when(mockAccountService.getUserBusinessName(any())(any(), any()))
            .thenReturn(ResultT.fromValue(Some("My Business")))

          when(mockSessionRepository.set(any()))
            .thenReturn(Future.successful(true))

          val application = applicationBuilder()
            .overrides(bind[AccountService].toInstance(mockAccountService))
            .build()

          running(application) {
            val request = FakeRequest(GET, onPageLoadRoute)
            val result  = route(application, request).value

            val view = application.injector.instanceOf[RemoveUserAccessView]
            val vm   = RemoveUserAccessViewModel.from(individualDetails, "My Business", formProvider)

            status(result)          mustEqual OK
            contentAsString(result) mustEqual view(
              vm.form,
              rcaspId,
              vm.titleKey,
              vm.headingKey,
              vm.errorKey,
              vm.rcaspName,
              vm.userBusinessName
            )(request, messages(application)).toString

            verify(mockAccountService).getRcaspDetails(any(), any())(any(), any())
            verify(mockAccountService).getUserBusinessName(any(), any())(any(), any())
          }
        }

        "must return OK and render the correct view for a rcaspIsUser RCASP" in {
          when(mockAccountService.getRcaspDetails(any(), any())(any(), any()))
            .thenReturn(ResultT.fromValue(rcaspIsUserDetails))

          when(mockAccountService.getUserBusinessName(any())(any(), any()))
            .thenReturn(ResultT.fromValue(Some("My Business")))

          when(mockSessionRepository.set(any()))
            .thenReturn(Future.successful(true))

          val application = applicationBuilder()
            .overrides(bind[AccountService].toInstance(mockAccountService))
            .build()

          running(application) {
            val request = FakeRequest(GET, onPageLoadRoute)
            val result  = route(application, request).value

            val view = application.injector.instanceOf[RemoveUserAccessView]
            val vm   = RemoveUserAccessViewModel.from(rcaspIsUserDetails, "My Business", formProvider)

            status(result)          mustEqual OK
            contentAsString(result) mustEqual view(
              vm.form,
              rcaspId,
              vm.titleKey,
              vm.headingKey,
              vm.errorKey,
              vm.rcaspName,
              vm.userBusinessName
            )(request, messages(application)).toString

            verify(mockAccountService).getRcaspDetails(any(), any())(any(), any())
            verify(mockAccountService).getUserBusinessName(any())(any(), any())
          }
        }

        "must return OK and render the correct view for an otherOrg RCASP" in {
          when(mockAccountService.getRcaspDetails(any(), any())(any(), any()))
            .thenReturn(ResultT.fromValue(otherOrgDetails))

          when(mockAccountService.getUserBusinessName(any())(any(), any()))
            .thenReturn(ResultT.fromValue(Some("My Business")))

          when(mockSessionRepository.set(any()))
            .thenReturn(Future.successful(true))

          val application = applicationBuilder()
            .overrides(bind[AccountService].toInstance(mockAccountService))
            .build()

          running(application) {
            val request = FakeRequest(GET, onPageLoadRoute)
            val result  = route(application, request).value

            val view = application.injector.instanceOf[RemoveUserAccessView]
            val vm   = RemoveUserAccessViewModel.from(otherOrgDetails, "My Business", formProvider)

            status(result)          mustEqual OK
            contentAsString(result) mustEqual view(
              vm.form,
              rcaspId,
              vm.titleKey,
              vm.headingKey,
              vm.errorKey,
              vm.rcaspName,
              vm.userBusinessName
            )(request, messages(application)).toString

            verify(mockAccountService).getRcaspDetails(any(), any())(any(), any())
            verify(mockAccountService).getUserBusinessName(any())(any(), any())
          }
        }

        "must return OK using fallback business name when getUserBusinessName returns None" in {
          when(mockAccountService.getRcaspDetails(any(), any())(any(), any()))
            .thenReturn(ResultT.fromValue(otherOrgDetails))

          when(mockAccountService.getUserBusinessName(any())(any(), any()))
            .thenReturn(ResultT.fromValue(None))

          when(mockSessionRepository.set(any()))
            .thenReturn(Future.successful(true))

          val application = applicationBuilder()
            .overrides(bind[AccountService].toInstance(mockAccountService))
            .build()

          running(application) {
            val request = FakeRequest(GET, onPageLoadRoute)
            val result  = route(application, request).value

            val view     = application.injector.instanceOf[RemoveUserAccessView]
            val fallback = messages(application)("homePage.contactDetails.org.fallbackBusinessName")
            val vm       = RemoveUserAccessViewModel.from(otherOrgDetails, fallback, formProvider)

            status(result)          mustEqual OK
            contentAsString(result) mustEqual view(
              vm.form,
              rcaspId,
              vm.titleKey,
              vm.headingKey,
              vm.errorKey,
              vm.rcaspName,
              vm.userBusinessName
            )(request, messages(application)).toString

            verify(mockAccountService).getRcaspDetails(any(), any())(any(), any())
            verify(mockAccountService).getUserBusinessName(any())(any(), any())
          }
        }

        "must redirect to Journey Recovery when getRcaspDetails returns an error" in {
          when(mockAccountService.getRcaspDetails(any(), any())(any(), any()))
            .thenReturn(ResultT.fromError(NotFoundError))

          when(mockSessionRepository.set(any()))
            .thenReturn(Future.successful(true))

          val application = applicationBuilder()
            .overrides(bind[AccountService].toInstance(mockAccountService))
            .build()

          running(application) {
            val request = FakeRequest(GET, onPageLoadRoute)
            val result  = route(application, request).value

            status(result)                 mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

            verify(mockAccountService).getRcaspDetails(any(), any())(any(), any())
            verify(mockAccountService, never).getUserBusinessName(any())(any(), any())
          }
        }

        "must redirect to Journey Recovery when getUserBusinessName returns an error" in {
          when(mockAccountService.getRcaspDetails(any(), any())(any(), any()))
            .thenReturn(ResultT.fromValue(otherOrgDetails))

          when(mockAccountService.getUserBusinessName(any())(any(), any()))
            .thenReturn(ResultT.fromError(NotFoundError))

          when(mockSessionRepository.set(any()))
            .thenReturn(Future.successful(true))

          val application = applicationBuilder()
            .overrides(bind[AccountService].toInstance(mockAccountService))
            .build()

          running(application) {
            val request = FakeRequest(GET, onPageLoadRoute)
            val result  = route(application, request).value

            status(result)                 mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

            verify(mockAccountService).getRcaspDetails(any(), any())(any(), any())
            verify(mockAccountService).getUserBusinessName(any())(any(), any())
          }
        }

        "must call APIs and return OK when cached RCASP details exist but business name is missing" in {
          when(mockAccountService.getRcaspDetails(any(), any())(any(), any()))
            .thenReturn(ResultT.fromValue(rcaspIsUserDetails))

          when(mockAccountService.getUserBusinessName(any())(any(), any()))
            .thenReturn(ResultT.fromValue(Some("My Business")))

          when(mockSessionRepository.set(any()))
            .thenReturn(Future.successful(true))

          val userAnswers = emptyUserAnswers
            .withPage(RemoveRcaspCachedDetails, rcaspIsUserDetails)

          val application = applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(bind[AccountService].toInstance(mockAccountService))
            .build()

          running(application) {
            val request = FakeRequest(GET, onPageLoadRoute)
            val result  = route(application, request).value

            val view = application.injector.instanceOf[RemoveUserAccessView]
            val vm   = RemoveUserAccessViewModel.from(rcaspIsUserDetails, "My Business", formProvider)

            status(result)          mustEqual OK
            contentAsString(result) mustEqual view(
              vm.form,
              rcaspId,
              vm.titleKey,
              vm.headingKey,
              vm.errorKey,
              vm.rcaspName,
              vm.userBusinessName
            )(request, messages(application)).toString

            verify(mockAccountService).getRcaspDetails(any(), any())(any(), any())
            verify(mockAccountService).getUserBusinessName(any())(any(), any())
          }
        }
      }

      "cache hit" - {

        "must return OK using cached details without calling APIs" in {
          val userAnswers = emptyUserAnswers
            .withPage(RemoveRcaspCachedDetails, rcaspIsUserDetails)
            .withPage(RemoveUserBusinessNameCached, "My Business")

          val application = applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(bind[AccountService].toInstance(mockAccountService))
            .build()

          running(application) {
            val request = FakeRequest(GET, onPageLoadRoute)
            val result  = route(application, request).value

            val view = application.injector.instanceOf[RemoveUserAccessView]
            val vm   = RemoveUserAccessViewModel.from(rcaspIsUserDetails, "My Business", formProvider)

            status(result)          mustEqual OK
            contentAsString(result) mustEqual view(
              vm.form,
              rcaspId,
              vm.titleKey,
              vm.headingKey,
              vm.errorKey,
              vm.rcaspName,
              vm.userBusinessName
            )(request, messages(application)).toString

            verify(mockAccountService, never).getRcaspDetails(any(), any())(any(), any())
            verify(mockAccountService, never).getUserBusinessName(any())(any(), any())
          }
        }

        "must return OK for an individual RCASP using cached details" in {
          val userAnswers = emptyUserAnswers
            .withPage(RemoveRcaspCachedDetails, individualDetails)
            .withPage(RemoveUserBusinessNameCached, "My Business")

          val application = applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(bind[AccountService].toInstance(mockAccountService))
            .build()

          running(application) {
            val request = FakeRequest(GET, onPageLoadRoute)
            val result  = route(application, request).value

            val view = application.injector.instanceOf[RemoveUserAccessView]
            val vm   = RemoveUserAccessViewModel.from(individualDetails, "My Business", formProvider)

            status(result)          mustEqual OK
            contentAsString(result) mustEqual view(
              vm.form,
              rcaspId,
              vm.titleKey,
              vm.headingKey,
              vm.errorKey,
              vm.rcaspName,
              vm.userBusinessName
            )(request, messages(application)).toString

            verify(mockAccountService, never).getRcaspDetails(any(), any())(any(), any())
            verify(mockAccountService, never).getUserBusinessName(any())(any(), any())
          }
        }

        "must return OK for an otherOrg RCASP using cached details" in {
          val userAnswers = emptyUserAnswers
            .withPage(RemoveRcaspCachedDetails, otherOrgDetails)
            .withPage(RemoveUserBusinessNameCached, "My Business")

          val application = applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(bind[AccountService].toInstance(mockAccountService))
            .build()

          running(application) {
            val request = FakeRequest(GET, onPageLoadRoute)
            val result  = route(application, request).value

            val view = application.injector.instanceOf[RemoveUserAccessView]
            val vm   = RemoveUserAccessViewModel.from(otherOrgDetails, "My Business", formProvider)

            status(result)          mustEqual OK
            contentAsString(result) mustEqual view(
              vm.form,
              rcaspId,
              vm.titleKey,
              vm.headingKey,
              vm.errorKey,
              vm.rcaspName,
              vm.userBusinessName
            )(request, messages(application)).toString

            verify(mockAccountService, never).getRcaspDetails(any(), any())(any(), any())
            verify(mockAccountService, never).getUserBusinessName(any())(any(), any())
          }
        }
      }
    }

    "onSubmit" - {

      "must redirect to RemoveOtherAccess when valid data is submitted" in {
        when(mockSessionRepository.set(any()))
          .thenReturn(Future.successful(true))

        val userAnswers = emptyUserAnswers
          .withPage(RemoveRcaspCachedDetails, rcaspIsUserDetails)
          .withPage(RemoveUserBusinessNameCached, "My Business")

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
            controllers.remove.routes.RemoveOtherAccessController.onPageLoad(rcaspId).url
        }
      }

      "must return BadRequest and render the view with errors when invalid data is submitted" in {
        val userAnswers = emptyUserAnswers
          .withPage(RemoveRcaspCachedDetails, rcaspIsUserDetails)
          .withPage(RemoveUserBusinessNameCached, "My Business")

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[AccountService].toInstance(mockAccountService))
          .build()

        running(application) {
          val request =
            FakeRequest(POST, onSubmitRoute)
              .withFormUrlEncodedBody(("value", ""))

          val result = route(application, request).value

          val view      = application.injector.instanceOf[RemoveUserAccessView]
          val vm        = RemoveUserAccessViewModel.from(rcaspIsUserDetails, "My Business", formProvider)
          val boundForm = vm.form.bindFromRequest()(request, implicitly)

          status(result)          mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(
            boundForm,
            rcaspId,
            vm.titleKey,
            vm.headingKey,
            vm.errorKey,
            vm.rcaspName,
            vm.userBusinessName
          )(request, messages(application)).toString
        }
      }

      "must redirect to Journey Recovery when RemoveRcaspCachedDetails not in cache" in {
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
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

      "must redirect to Journey Recovery when RemoveUserBusinessNameCached not in cache" in {
        val userAnswers = emptyUserAnswers
          .withPage(RemoveRcaspCachedDetails, rcaspIsUserDetails)

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
        }
      }

      "must redirect to Journey Recovery when no userAnswers exist on submit" in {
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
