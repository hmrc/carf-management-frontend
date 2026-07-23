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
import models.UserBusinessSubscriptionData
import models.errors.ApiError.NotFoundError
import models.viewAndUpdateRcasp.RcaspDetails
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, reset, verify, when}
import pages.SubmissionSucceededPage
import pages.remove.RemoveRcaspCachedDetails
import pages.remove.RemoveUserBusinessInfoCached
import play.api.data.FormBinding.Implicits.formBinding
import play.api.i18n.Messages
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

  implicit val messages: Messages = messages(app)

  private val formProvider = new GenericYesNoPageFormProvider()

  private val individualDetails: RcaspDetails =
    individualRcaspDetailsViewUpdate.copy(RCASPID = rcaspId, IsRCASPUser = false)

  private val rcaspIsUserDetails: RcaspDetails =
    organisationRcaspDetailsViewUpdate.copy(RCASPID = rcaspId, IsRCASPUser = true)

  private val otherOrgDetails: RcaspDetails =
    organisationRcaspDetailsViewUpdate.copy(RCASPID = rcaspId, IsRCASPUser = false)

  private val individualUserInfo   =
    UserBusinessSubscriptionData(hasOrganisationContactDetails = false, organisationName = None)
  private val organisationUserInfo =
    UserBusinessSubscriptionData(hasOrganisationContactDetails = true, organisationName = Some("My Business"))

  private val pageUnavailableUrl: String =
    controllers.routes.PlaceholderController.onPageLoad("Should nav to /problem/page-unavailable (CARF-308)").url

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAccountService)
  }

  "RemoveUserAccessController" - {

    "onPageLoad" - {

      "cache miss" - {

        "must return OK and render the correct view for an individual user" in {
          when(mockAccountService.getRcaspDetails(any(), any())(any(), any()))
            .thenReturn(ResultT.fromValue(individualDetails))

          when(mockAccountService.getUserBusinessSubscriptionData(any())(any(), any()))
            .thenReturn(ResultT.fromValue(individualUserInfo))

          when(mockSessionRepository.set(any()))
            .thenReturn(Future.successful(true))

          val application = applicationBuilder()
            .overrides(bind[AccountService].toInstance(mockAccountService))
            .build()

          running(application) {
            val request = FakeRequest(GET, onPageLoadRoute)
            val result  = route(application, request).value

            val view = application.injector.instanceOf[RemoveUserAccessView]
            val vm   = RemoveUserAccessViewModel.from(individualDetails, individualUserInfo, formProvider)

            status(result)          mustEqual OK
            contentAsString(result) mustEqual view(
              vm.form,
              rcaspId,
              vm.titleKey,
              vm.headingKey,
              vm.rcaspName,
              vm.userBusinessNameOpt
            )(request, messages(application)).toString

            verify(mockAccountService).getRcaspDetails(any(), any())(any(), any())
            verify(mockAccountService).getUserBusinessSubscriptionData(any())(any(), any())
          }
        }

        "must return OK and render the correct view for a rcaspIsUser RCASP" in {
          when(mockAccountService.getRcaspDetails(any(), any())(any(), any()))
            .thenReturn(ResultT.fromValue(rcaspIsUserDetails))

          when(mockAccountService.getUserBusinessSubscriptionData(any())(any(), any()))
            .thenReturn(ResultT.fromValue(organisationUserInfo))

          when(mockSessionRepository.set(any()))
            .thenReturn(Future.successful(true))

          val application = applicationBuilder()
            .overrides(bind[AccountService].toInstance(mockAccountService))
            .build()

          running(application) {
            val request = FakeRequest(GET, onPageLoadRoute)
            val result  = route(application, request).value

            val view = application.injector.instanceOf[RemoveUserAccessView]
            val vm   = RemoveUserAccessViewModel.from(rcaspIsUserDetails, organisationUserInfo, formProvider)

            status(result)          mustEqual OK
            contentAsString(result) mustEqual view(
              vm.form,
              rcaspId,
              vm.titleKey,
              vm.headingKey,
              vm.rcaspName,
              vm.userBusinessNameOpt
            )(request, messages(application)).toString

            verify(mockAccountService).getRcaspDetails(any(), any())(any(), any())
            verify(mockAccountService).getUserBusinessSubscriptionData(any())(any(), any())
          }
        }

        "must return OK and render the correct view for an otherOrg RCASP" in {
          when(mockAccountService.getRcaspDetails(any(), any())(any(), any()))
            .thenReturn(ResultT.fromValue(otherOrgDetails))

          when(mockAccountService.getUserBusinessSubscriptionData(any())(any(), any()))
            .thenReturn(ResultT.fromValue(organisationUserInfo))

          when(mockSessionRepository.set(any()))
            .thenReturn(Future.successful(true))

          val application = applicationBuilder()
            .overrides(bind[AccountService].toInstance(mockAccountService))
            .build()

          running(application) {
            val request = FakeRequest(GET, onPageLoadRoute)
            val result  = route(application, request).value

            val view = application.injector.instanceOf[RemoveUserAccessView]
            val vm   = RemoveUserAccessViewModel.from(otherOrgDetails, organisationUserInfo, formProvider)

            status(result)          mustEqual OK
            contentAsString(result) mustEqual view(
              vm.form,
              rcaspId,
              vm.titleKey,
              vm.headingKey,
              vm.rcaspName,
              vm.userBusinessNameOpt
            )(request, messages(application)).toString

            verify(mockAccountService).getRcaspDetails(any(), any())(any(), any())
            verify(mockAccountService).getUserBusinessSubscriptionData(any())(any(), any())
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
            verify(mockAccountService, never).getUserBusinessSubscriptionData(any())(any(), any())
          }
        }

        "must redirect to Journey Recovery when getUserBusinessSubscriptionData returns an error" in {
          when(mockAccountService.getRcaspDetails(any(), any())(any(), any()))
            .thenReturn(ResultT.fromValue(otherOrgDetails))

          when(mockAccountService.getUserBusinessSubscriptionData(any())(any(), any()))
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
            verify(mockAccountService).getUserBusinessSubscriptionData(any())(any(), any())
          }
        }

        "must call APIs and return OK when cached RCASP details exist but user business info is missing" in {
          when(mockAccountService.getRcaspDetails(any(), any())(any(), any()))
            .thenReturn(ResultT.fromValue(rcaspIsUserDetails))

          when(mockAccountService.getUserBusinessSubscriptionData(any())(any(), any()))
            .thenReturn(ResultT.fromValue(organisationUserInfo))

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
            val vm   = RemoveUserAccessViewModel.from(rcaspIsUserDetails, organisationUserInfo, formProvider)

            status(result)          mustEqual OK
            contentAsString(result) mustEqual view(
              vm.form,
              rcaspId,
              vm.titleKey,
              vm.headingKey,
              vm.rcaspName,
              vm.userBusinessNameOpt
            )(request, messages(application)).toString

            verify(mockAccountService).getRcaspDetails(any(), any())(any(), any())
            verify(mockAccountService).getUserBusinessSubscriptionData(any())(any(), any())
          }
        }

        "must ignore cached details when cached rcaspId does not match URL rcaspId" in {
          val differentDetails = organisationRcaspDetailsViewUpdate.copy(RCASPID = "DIFFERENT-ID", IsRCASPUser = true)

          val userAnswers = emptyUserAnswers
            .withPage(RemoveRcaspCachedDetails, differentDetails)
            .withPage(
              RemoveUserBusinessInfoCached,
              UserBusinessSubscriptionData(
                hasOrganisationContactDetails = true,
                organisationName = Some("Old Cached Business")
              )
            )

          when(mockAccountService.getRcaspDetails(any(), any())(any(), any()))
            .thenReturn(ResultT.fromValue(rcaspIsUserDetails))

          when(mockAccountService.getUserBusinessSubscriptionData(any())(any(), any()))
            .thenReturn(
              ResultT.fromValue(
                UserBusinessSubscriptionData(
                  hasOrganisationContactDetails = true,
                  organisationName = Some("Fresh API Business")
                )
              )
            )

          when(mockSessionRepository.set(any()))
            .thenReturn(Future.successful(true))

          val application = applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(bind[AccountService].toInstance(mockAccountService))
            .build()

          running(application) {
            val request = FakeRequest(GET, onPageLoadRoute)
            val result  = route(application, request).value

            val view = application.injector.instanceOf[RemoveUserAccessView]
            val vm   = RemoveUserAccessViewModel.from(
              rcaspIsUserDetails,
              UserBusinessSubscriptionData(
                hasOrganisationContactDetails = true,
                organisationName = Some("Fresh API Business")
              ),
              formProvider
            )

            status(result)          mustEqual OK
            contentAsString(result) mustEqual view(
              vm.form,
              rcaspId,
              vm.titleKey,
              vm.headingKey,
              vm.rcaspName,
              vm.userBusinessNameOpt
            )(request, messages(application)).toString

            verify(mockAccountService).getRcaspDetails(any(), any())(any(), any())
            verify(mockAccountService).getUserBusinessSubscriptionData(any())(any(), any())
            verify(mockSessionRepository).set(any())
          }
        }

        "must fetch fresh details when SubmissionSucceededPage is true but cached RCASPID differs from URL rcaspId (new journey)" in {
          val differentDetails = organisationRcaspDetailsViewUpdate.copy(RCASPID = "DIFFERENT-ID", IsRCASPUser = true)

          val userAnswers = emptyUserAnswers
            .withPage(SubmissionSucceededPage, true)
            .withPage(RemoveRcaspCachedDetails, differentDetails)
            .withPage(RemoveUserBusinessInfoCached, organisationUserInfo)

          when(mockAccountService.getRcaspDetails(any(), any())(any(), any()))
            .thenReturn(ResultT.fromValue(rcaspIsUserDetails))

          when(mockAccountService.getUserBusinessSubscriptionData(any())(any(), any()))
            .thenReturn(ResultT.fromValue(organisationUserInfo))

          when(mockSessionRepository.set(any()))
            .thenReturn(Future.successful(true))

          val application = applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(bind[AccountService].toInstance(mockAccountService))
            .build()

          running(application) {
            val request = FakeRequest(GET, onPageLoadRoute)
            val result  = route(application, request).value

            status(result) mustEqual OK

            verify(mockAccountService).getRcaspDetails(any(), any())(any(), any())
            verify(mockAccountService).getUserBusinessSubscriptionData(any())(any(), any())
          }
        }
      }

      "cache hit" - {

        "must return OK using cached details without calling APIs" in {
          val userAnswers = emptyUserAnswers
            .withPage(RemoveRcaspCachedDetails, rcaspIsUserDetails)
            .withPage(RemoveUserBusinessInfoCached, organisationUserInfo)

          val application = applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(bind[AccountService].toInstance(mockAccountService))
            .build()

          running(application) {
            val request = FakeRequest(GET, onPageLoadRoute)
            val result  = route(application, request).value

            val view = application.injector.instanceOf[RemoveUserAccessView]
            val vm   = RemoveUserAccessViewModel.from(rcaspIsUserDetails, organisationUserInfo, formProvider)

            status(result)          mustEqual OK
            contentAsString(result) mustEqual view(
              vm.form,
              rcaspId,
              vm.titleKey,
              vm.headingKey,
              vm.rcaspName,
              vm.userBusinessNameOpt
            )(request, messages(application)).toString

            verify(mockAccountService, never).getRcaspDetails(any(), any())(any(), any())
            verify(mockAccountService, never).getUserBusinessSubscriptionData(any())(any(), any())
          }
        }

        "must return OK for an individual user using cached details" in {
          val userAnswers = emptyUserAnswers
            .withPage(RemoveRcaspCachedDetails, individualDetails)
            .withPage(RemoveUserBusinessInfoCached, individualUserInfo)

          val application = applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(bind[AccountService].toInstance(mockAccountService))
            .build()

          running(application) {
            val request = FakeRequest(GET, onPageLoadRoute)
            val result  = route(application, request).value

            val view = application.injector.instanceOf[RemoveUserAccessView]
            val vm   = RemoveUserAccessViewModel.from(individualDetails, individualUserInfo, formProvider)

            status(result)          mustEqual OK
            contentAsString(result) mustEqual view(
              vm.form,
              rcaspId,
              vm.titleKey,
              vm.headingKey,
              vm.rcaspName,
              vm.userBusinessNameOpt
            )(request, messages(application)).toString

            verify(mockAccountService, never).getRcaspDetails(any(), any())(any(), any())
            verify(mockAccountService, never).getUserBusinessSubscriptionData(any())(any(), any())
          }
        }

        "must return OK for an otherOrg RCASP using cached details" in {
          val userAnswers = emptyUserAnswers
            .withPage(RemoveRcaspCachedDetails, otherOrgDetails)
            .withPage(RemoveUserBusinessInfoCached, organisationUserInfo)

          val application = applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(bind[AccountService].toInstance(mockAccountService))
            .build()

          running(application) {
            val request = FakeRequest(GET, onPageLoadRoute)
            val result  = route(application, request).value

            val view = application.injector.instanceOf[RemoveUserAccessView]
            val vm   = RemoveUserAccessViewModel.from(otherOrgDetails, organisationUserInfo, formProvider)

            status(result)          mustEqual OK
            contentAsString(result) mustEqual view(
              vm.form,
              rcaspId,
              vm.titleKey,
              vm.headingKey,
              vm.rcaspName,
              vm.userBusinessNameOpt
            )(request, messages(application)).toString

            verify(mockAccountService, never).getRcaspDetails(any(), any())(any(), any())
            verify(mockAccountService, never).getUserBusinessSubscriptionData(any())(any(), any())
          }
        }
      }

      "already submitted for this RCASP" - {

        "must redirect to page-unavailable placeholder without calling any APIs when cached RCASPID matches the URL and SubmissionSucceededPage is true" in {
          val userAnswers = emptyUserAnswers
            .withPage(SubmissionSucceededPage, true)
            .withPage(RemoveRcaspCachedDetails, rcaspIsUserDetails)
            .withPage(RemoveUserBusinessInfoCached, organisationUserInfo)

          val application = applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(bind[AccountService].toInstance(mockAccountService))
            .build()

          running(application) {
            val request = FakeRequest(GET, onPageLoadRoute)
            val result  = route(application, request).value

            status(result)                 mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual pageUnavailableUrl

            verify(mockAccountService, never).getRcaspDetails(any(), any())(any(), any())
            verify(mockAccountService, never).getUserBusinessSubscriptionData(any())(any(), any())
            verify(mockSessionRepository, never).set(any())
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
          .withPage(RemoveUserBusinessInfoCached, organisationUserInfo)

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
            controllers.remove.routes.RemoveOtherAccessController.onPageLoad().url
        }
      }

      "must return BadRequest and render the view with errors when invalid data is submitted" in {
        val userAnswers = emptyUserAnswers
          .withPage(RemoveRcaspCachedDetails, rcaspIsUserDetails)
          .withPage(RemoveUserBusinessInfoCached, organisationUserInfo)

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[AccountService].toInstance(mockAccountService))
          .build()

        running(application) {
          val request =
            FakeRequest(POST, onSubmitRoute)
              .withFormUrlEncodedBody(("value", ""))

          val result = route(application, request).value

          val view      = application.injector.instanceOf[RemoveUserAccessView]
          val vm        = RemoveUserAccessViewModel.from(rcaspIsUserDetails, organisationUserInfo, formProvider)
          val boundForm = vm.form.bindFromRequest()(request, implicitly)

          status(result)          mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(
            boundForm,
            rcaspId,
            vm.titleKey,
            vm.headingKey,
            vm.rcaspName,
            vm.userBusinessNameOpt
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

      "must redirect to Journey Recovery when RemoveUserBusinessInfoCached not in cache" in {
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

      "must redirect to Journey Recovery when cached rcaspId does not match URL rcaspId" in {
        val differentDetails = organisationRcaspDetailsViewUpdate.copy(RCASPID = "DIFFERENT-ID", IsRCASPUser = true)
        val userAnswers      = emptyUserAnswers
          .withPage(RemoveRcaspCachedDetails, differentDetails)
          .withPage(RemoveUserBusinessInfoCached, organisationUserInfo)

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
    }
  }
}
