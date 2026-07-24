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
import models.viewAndUpdateRcasp.RcaspDetails
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import pages.remove.{RemoveOtherAccessPage, RemoveRcaspCachedDetails, RemoveUserBusinessInfoCached}
import play.api.data.FormBinding.Implicits.formBinding
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import viewmodels.remove.RemoveOtherAccessViewModel
import views.html.remove.RemoveOtherAccessView

import scala.concurrent.Future

class RemoveOtherAccessControllerSpec extends SpecBase {

  lazy val onPageLoadRoute: String = controllers.remove.routes.RemoveOtherAccessController.onPageLoad().url
  lazy val onSubmitRoute: String   = controllers.remove.routes.RemoveOtherAccessController.onSubmit().url

  private val formProvider = new GenericYesNoPageFormProvider()

  private val rcaspIsUserDetails: RcaspDetails =
    organisationRcaspDetailsViewUpdate.copy(RCASPID = rcaspId, IsRCASPUser = true)

  private val otherOrgDetails: RcaspDetails =
    organisationRcaspDetailsViewUpdate.copy(RCASPID = rcaspId, IsRCASPUser = false)

  private val individualUserInfo   =
    UserBusinessSubscriptionData(hasOrganisationContactDetails = false, organisationName = None)
  private val organisationUserInfo =
    UserBusinessSubscriptionData(hasOrganisationContactDetails = true, organisationName = Some("My Business"))

  "RemoveOtherAccessController" - {

    "onPageLoad" - {

      "must return OK and render the correct view when the user is an individual" in {
        val userAnswers = emptyUserAnswers
          .withPage(RemoveRcaspCachedDetails, otherOrgDetails)
          .withPage(RemoveUserBusinessInfoCached, individualUserInfo)

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, onPageLoadRoute)
          val result  = route(application, request).value

          val view = application.injector.instanceOf[RemoveOtherAccessView]
          val vm   = RemoveOtherAccessViewModel.from(otherOrgDetails, isUserOrganisation = false, formProvider)

          status(result)          mustEqual OK
          contentAsString(result) mustEqual view(
            vm.form,
            vm.titleKey,
            vm.headingKey,
            vm.rcaspName
          )(request, messages(application)).toString
        }
      }

      "must return OK and render the correct view for a rcaspIsUser RCASP" in {
        val userAnswers = emptyUserAnswers
          .withPage(RemoveRcaspCachedDetails, rcaspIsUserDetails)
          .withPage(RemoveUserBusinessInfoCached, organisationUserInfo)

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, onPageLoadRoute)
          val result  = route(application, request).value

          val view = application.injector.instanceOf[RemoveOtherAccessView]
          val vm   = RemoveOtherAccessViewModel.from(rcaspIsUserDetails, isUserOrganisation = true, formProvider)

          status(result)          mustEqual OK
          contentAsString(result) mustEqual view(
            vm.form,
            vm.titleKey,
            vm.headingKey,
            vm.rcaspName
          )(request, messages(application)).toString
        }
      }

      "must return OK and render the correct view for an otherOrg RCASP" in {
        val userAnswers = emptyUserAnswers
          .withPage(RemoveRcaspCachedDetails, otherOrgDetails)
          .withPage(RemoveUserBusinessInfoCached, organisationUserInfo)

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, onPageLoadRoute)
          val result  = route(application, request).value

          val view = application.injector.instanceOf[RemoveOtherAccessView]
          val vm   = RemoveOtherAccessViewModel.from(otherOrgDetails, isUserOrganisation = true, formProvider)

          status(result)          mustEqual OK
          contentAsString(result) mustEqual view(
            vm.form,
            vm.titleKey,
            vm.headingKey,
            vm.rcaspName
          )(request, messages(application)).toString
        }
      }

      "must populate the view correctly on GET when question has previously been answered" in {
        val userAnswers = emptyUserAnswers
          .withPage(RemoveRcaspCachedDetails, rcaspIsUserDetails)
          .withPage(RemoveUserBusinessInfoCached, organisationUserInfo)
          .withPage(RemoveOtherAccessPage, true)

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, onPageLoadRoute)
          val result  = route(application, request).value

          val view = application.injector.instanceOf[RemoveOtherAccessView]
          val vm   = RemoveOtherAccessViewModel.from(rcaspIsUserDetails, isUserOrganisation = true, formProvider)

          status(result)          mustEqual OK
          contentAsString(result) mustEqual view(
            vm.form.fill(true),
            vm.titleKey,
            vm.headingKey,
            vm.rcaspName
          )(request, messages(application)).toString
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

      "must redirect to Journey Recovery when RemoveUserBusinessInfoCached not in UserAnswers" in {
        val userAnswers = emptyUserAnswers.withPage(RemoveRcaspCachedDetails, rcaspIsUserDetails)
        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

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

      "must redirect to RemoveRcaspController when 'Yes' is submitted" in {
        when(mockSessionRepository.set(any()))
          .thenReturn(Future.successful(true))

        val userAnswers = emptyUserAnswers
          .withPage(RemoveRcaspCachedDetails, rcaspIsUserDetails)
          .withPage(RemoveUserBusinessInfoCached, organisationUserInfo)

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request =
            FakeRequest(POST, onSubmitRoute)
              .withFormUrlEncodedBody(("value", "true"))

          val result = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual
            controllers.remove.routes.RemoveRcaspController.onPageLoad().url
        }
      }

      "must redirect to RemoveRcaspController when 'No' is submitted" in {
        when(mockSessionRepository.set(any()))
          .thenReturn(Future.successful(true))

        val userAnswers = emptyUserAnswers
          .withPage(RemoveRcaspCachedDetails, rcaspIsUserDetails)
          .withPage(RemoveUserBusinessInfoCached, organisationUserInfo)

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request =
            FakeRequest(POST, onSubmitRoute)
              .withFormUrlEncodedBody(("value", "false"))

          val result = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual
            controllers.remove.routes.RemoveRcaspController.onPageLoad().url
        }
      }

      "must return BadRequest and render the view with errors when invalid data is submitted" in {
        val userAnswers = emptyUserAnswers
          .withPage(RemoveRcaspCachedDetails, rcaspIsUserDetails)
          .withPage(RemoveUserBusinessInfoCached, organisationUserInfo)

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request =
            FakeRequest(POST, onSubmitRoute)
              .withFormUrlEncodedBody(("value", ""))

          val result = route(application, request).value

          val view      = application.injector.instanceOf[RemoveOtherAccessView]
          val vm        = RemoveOtherAccessViewModel.from(rcaspIsUserDetails, isUserOrganisation = true, formProvider)
          val boundForm = vm.form.bindFromRequest()(request, implicitly)

          status(result)          mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(
            boundForm,
            vm.titleKey,
            vm.headingKey,
            vm.rcaspName
          )(request, messages(application)).toString
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

      "must redirect to Journey Recovery when RemoveUserBusinessInfoCached not in UserAnswers" in {
        val userAnswers = emptyUserAnswers.withPage(RemoveRcaspCachedDetails, rcaspIsUserDetails)
        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

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
