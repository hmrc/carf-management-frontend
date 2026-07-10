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
import models.errors.ApiError.NotFoundError
import models.viewAndUpdateRcasp.RcaspDetails
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import pages.remove.RemoveRcaspCachedDetails
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.AccountService
import types.ResultT

import scala.concurrent.Future

class RemoveUserAccessControllerSpec extends SpecBase {

  lazy val onPageLoadRoute: String = controllers.remove.routes.RemoveUserAccessController.onPageLoad(rcaspId).url

  lazy val onSubmitRoute: String = controllers.remove.routes.RemoveUserAccessController.onSubmit(rcaspId).url

  val mockAccountService: AccountService = mock[AccountService]

  private val individualDetails: RcaspDetails =
    individualRcaspDetailsResponse.copy(RCASPID = rcaspId, IsRCASPUser = false)

  private val rcaspIsUserDetails: RcaspDetails =
    organisationRcaspDetailsResponse.copy(RCASPID = rcaspId, IsRCASPUser = true)

  private val otherOrgDetails: RcaspDetails =
    organisationRcaspDetailsResponse.copy(RCASPID = rcaspId, IsRCASPUser = false)

  "RemoveUserAccessController" - {

    "onPageLoad" - {

      "cache miss" - {

        "must return OK for an individual RCASP" in {
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

            status(result) mustEqual OK
          }
        }

        "must return OK for a rcaspIsUser RCASP" in {
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

            status(result) mustEqual OK
          }
        }

        "must return OK for an otherOrg RCASP" in {
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

            status(result) mustEqual OK
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
          }
        }

        "must redirect to Journey Recovery when user business name is missing for otherOrg" in {
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

            status(result)                 mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
          }
        }
      }

      "cache hit" - {

        "must return OK using cached details without calling API" in {
          when(mockAccountService.getUserBusinessName(any())(any(), any()))
            .thenReturn(ResultT.fromValue(Some("My Business")))

          val userAnswers = emptyUserAnswers.withPage(RemoveRcaspCachedDetails, rcaspIsUserDetails)

          val application = applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(bind[AccountService].toInstance(mockAccountService))
            .build()

          running(application) {
            val request = FakeRequest(GET, onPageLoadRoute)
            val result  = route(application, request).value

            status(result) mustEqual OK
          }
        }

        "must redirect to Journey Recovery when getUserBusinessName returns an error" in {
          when(mockAccountService.getUserBusinessName(any())(any(), any()))
            .thenReturn(ResultT.fromError(NotFoundError))

          val userAnswers = emptyUserAnswers.withPage(RemoveRcaspCachedDetails, rcaspIsUserDetails)

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

        "must redirect to Journey Recovery when user business name is missing for otherOrg (cache hit)" in {
          when(mockAccountService.getUserBusinessName(any())(any(), any()))
            .thenReturn(ResultT.fromValue(None))

          val userAnswers = emptyUserAnswers.withPage(RemoveRcaspCachedDetails, otherOrgDetails)

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
      }

      "onSubmit" - {

        "must redirect to RemoveOtherAccess when valid data is submitted" in {
          when(mockAccountService.getUserBusinessName(any())(any(), any()))
            .thenReturn(ResultT.fromValue(Some("My Business")))

          when(mockSessionRepository.set(any()))
            .thenReturn(Future.successful(true))

          val userAnswers = emptyUserAnswers.withPage(RemoveRcaspCachedDetails, rcaspIsUserDetails)

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

        "must return BadRequest when invalid data is submitted" in {
          when(mockAccountService.getUserBusinessName(any())(any(), any()))
            .thenReturn(ResultT.fromValue(Some("My Business")))

          val userAnswers = emptyUserAnswers.withPage(RemoveRcaspCachedDetails, rcaspIsUserDetails)

          val application = applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(bind[AccountService].toInstance(mockAccountService))
            .build()

          running(application) {
            val request =
              FakeRequest(POST, onSubmitRoute)
                .withFormUrlEncodedBody(("value", ""))

            val result = route(application, request).value

            status(result) mustEqual BAD_REQUEST
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

        "must redirect to Journey Recovery when getUserBusinessName returns an error on submit" in {
          when(mockAccountService.getUserBusinessName(any())(any(), any()))
            .thenReturn(ResultT.fromError(NotFoundError))

          val userAnswers = emptyUserAnswers.withPage(RemoveRcaspCachedDetails, rcaspIsUserDetails)

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

        "must redirect to Journey Recovery when buildViewModel returns Left on submit" in {
          when(mockAccountService.getUserBusinessName(any())(any(), any()))
            .thenReturn(ResultT.fromValue(None))

          val userAnswers = emptyUserAnswers.withPage(RemoveRcaspCachedDetails, otherOrgDetails)

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
}
