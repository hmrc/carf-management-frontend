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

package controllers

import base.SpecBase
import models.NormalMode
import models.errors.InternalServerError
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.AccountService
import types.ResultT

import scala.concurrent.Future

class RoutingControllerSpec extends SpecBase {

  val mockAccountService: AccountService = mock[AccountService]

  lazy val routeUnderTest: String =
    controllers.routes.RoutingController.onPageLoad().url

  "RoutingController" - {

    "must redirect to OrganisationOrIndividual when user has RCASPs already added" in {
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockAccountService.getNumberOfRcaspsCurrentlyAdded(any()))
        .thenReturn(ResultT.fromValue(1))

      val application =
        applicationBuilder(
          userAnswers = Some(emptyUserAnswers),
          requestUtr = Some(testUtr.uniqueTaxPayerReference)
        )
          .overrides(bind[AccountService].toInstance(mockAccountService))
          .build()

      running(application) {
        val request = FakeRequest(GET, routeUnderTest)
        val result  = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          controllers.combined.routes.OrganisationOrIndividualController.onPageLoad(NormalMode).url
      }
    }

    "must redirect to ReportForRegisteredBusiness when user has zero RCASPs and has a CT UTR" in {
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockAccountService.getNumberOfRcaspsCurrentlyAdded(any()))
        .thenReturn(ResultT.fromValue(0))

      val application =
        applicationBuilder(
          userAnswers = Some(emptyUserAnswers),
          requestUtr = Some(testUtr.uniqueTaxPayerReference)
        )
          .overrides(bind[AccountService].toInstance(mockAccountService))
          .build()

      running(application) {
        val request = FakeRequest(GET, routeUnderTest)
        val result  = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          controllers.organisation.routes.ReportForRegisteredBusinessController.onPageLoad(NormalMode).url
      }
    }

    "must redirect to OrganisationOrIndividual when user has zero RCASPs and has no CT UTR" in {
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockAccountService.getNumberOfRcaspsCurrentlyAdded(any()))
        .thenReturn(ResultT.fromValue(0))

      val application =
        applicationBuilder(
          userAnswers = Some(emptyUserAnswers),
          requestUtr = None
        )
          .overrides(bind[AccountService].toInstance(mockAccountService))
          .build()

      running(application) {
        val request = FakeRequest(GET, routeUnderTest)
        val result  = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          controllers.combined.routes.OrganisationOrIndividualController.onPageLoad(NormalMode).url
      }
    }

    "must redirect to Journey Recovery when account service returns an error" in {
      when(mockAccountService.getNumberOfRcaspsCurrentlyAdded(any()))
        .thenReturn(ResultT.fromError(InternalServerError))

      val application =
        applicationBuilder(
          userAnswers = Some(emptyUserAnswers),
          requestUtr = Some(testUtr.uniqueTaxPayerReference)
        )
          .overrides(bind[AccountService].toInstance(mockAccountService))
          .build()

      running(application) {
        val request = FakeRequest(GET, routeUnderTest)
        val result  = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, routeUnderTest)
        val result  = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must create new UserAnswers and save to session when no existing answers found" in {
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockAccountService.getNumberOfRcaspsCurrentlyAdded(any()))
        .thenReturn(ResultT.fromValue(0))

      val application =
        applicationBuilder(
          userAnswers = Some(emptyUserAnswers),
          requestUtr = Some(testUtr.uniqueTaxPayerReference)
        )
          .overrides(bind[AccountService].toInstance(mockAccountService))
          .build()

      running(application) {
        val request = FakeRequest(GET, routeUnderTest)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
      }
    }
  }
}
