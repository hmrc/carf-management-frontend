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
import models.errors.ApiError.InternalServerError
import models.{ChangeMode, NormalMode, UserAnswers}
import org.mockito.ArgumentMatchers.{any, argThat}
import org.mockito.Mockito.{verify, when}
import pages.organisation.OverwritableOrganisationName
import pages.{RcaspIdPage, SubmissionSucceededPage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.AccountService
import types.ResultT

import scala.concurrent.Future

class RoutingControllerSpec extends SpecBase {

  val mockAccountService: AccountService = mock[AccountService]

  lazy val routeUnderTest: String =
    controllers.routes.RoutingController.onPageLoad(NormalMode).url

  lazy val changeRouteUnderTest: String =
    controllers.routes.RoutingController.onPageLoad(ChangeMode).url

  "RoutingController" - {

    "must redirect to OrganisationOrIndividual when user has RCASPs already added" in {
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockAccountService.getNumberOfRcaspsCurrentlyAdded(any())(any(), any()))
        .thenReturn(ResultT.fromValue(1))

      val application =
        applicationBuilder(
          userAnswers = Some(emptyUserAnswers),
          requestUtr = Some(testUtr.uniqueTaxPayerReference)
        ).overrides(bind[AccountService].toInstance(mockAccountService)).build()

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
      when(mockAccountService.getNumberOfRcaspsCurrentlyAdded(any())(any(), any()))
        .thenReturn(ResultT.fromValue(0))

      val application =
        applicationBuilder(
          userAnswers = Some(emptyUserAnswers),
          requestUtr = Some(testUtr.uniqueTaxPayerReference)
        ).overrides(bind[AccountService].toInstance(mockAccountService)).build()

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
      when(mockAccountService.getNumberOfRcaspsCurrentlyAdded(any())(any(), any()))
        .thenReturn(ResultT.fromValue(0))

      val application =
        applicationBuilder(
          userAnswers = Some(emptyUserAnswers),
          requestUtr = None
        ).overrides(bind[AccountService].toInstance(mockAccountService)).build()

      running(application) {
        val request = FakeRequest(GET, routeUnderTest)
        val result  = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          controllers.combined.routes.OrganisationOrIndividualController.onPageLoad(NormalMode).url
      }
    }

    "must redirect to Journey Recovery when account service returns an error" in {
      when(mockAccountService.getNumberOfRcaspsCurrentlyAdded(any())(any(), any()))
        .thenReturn(ResultT.fromError(InternalServerError))

      val application =
        applicationBuilder(
          userAnswers = Some(emptyUserAnswers),
          requestUtr = Some(testUtr.uniqueTaxPayerReference)
        ).overrides(bind[AccountService].toInstance(mockAccountService)).build()

      running(application) {
        val request = FakeRequest(GET, routeUnderTest)
        val result  = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to the next page in NormalMode when submission has already succeeded" in {
      val staleUserAnswers = emptyUserAnswers
        .withPage(SubmissionSucceededPage, true)
        .withPage(RcaspIdPage, rcaspId)
        .withPage(OverwritableOrganisationName, testOrgName)

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockAccountService.getNumberOfRcaspsCurrentlyAdded(any())(any(), any()))
        .thenReturn(ResultT.fromValue(1))

      val application =
        applicationBuilder(
          userAnswers = Some(staleUserAnswers),
          requestUtr = Some(testUtr.uniqueTaxPayerReference)
        ).overrides(bind[AccountService].toInstance(mockAccountService)).build()

      running(application) {
        val request = FakeRequest(GET, routeUnderTest)
        val result  = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          controllers.combined.routes.OrganisationOrIndividualController.onPageLoad(NormalMode).url
      }
    }

    "must save fresh UserAnswers in NormalMode when stale session data exists" in {
      val staleUserAnswers = emptyUserAnswers
        .withPage(SubmissionSucceededPage, true)
        .withPage(RcaspIdPage, rcaspId)
        .withPage(OverwritableOrganisationName, testOrgName)

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockAccountService.getNumberOfRcaspsCurrentlyAdded(any())(any(), any()))
        .thenReturn(ResultT.fromValue(0))

      val application =
        applicationBuilder(
          userAnswers = Some(staleUserAnswers),
          requestUtr = Some(testUtr.uniqueTaxPayerReference)
        ).overrides(bind[AccountService].toInstance(mockAccountService)).build()

      running(application) {
        val request = FakeRequest(GET, routeUnderTest)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER

        verify(mockSessionRepository).set(
          argThat((answers: UserAnswers) =>
            answers.get(SubmissionSucceededPage).isEmpty &&
              answers.get(RcaspIdPage).isEmpty &&
              answers.get(OverwritableOrganisationName).isEmpty
          )
        )
      }
    }

    "must preserve UserAnswers in NormalMode when SubmissionSucceededPage is false" in {
      val existingUserAnswers = emptyUserAnswers
        .withPage(SubmissionSucceededPage, false)
        .withPage(OverwritableOrganisationName, testOrgName)

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockAccountService.getNumberOfRcaspsCurrentlyAdded(any())(any(), any()))
        .thenReturn(ResultT.fromValue(0))

      val application =
        applicationBuilder(
          userAnswers = Some(existingUserAnswers),
          requestUtr = Some(testUtr.uniqueTaxPayerReference)
        ).overrides(bind[AccountService].toInstance(mockAccountService)).build()

      running(application) {
        val request = FakeRequest(GET, routeUnderTest)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER

        verify(mockSessionRepository).set(
          argThat(_.get(OverwritableOrganisationName).contains(testOrgName))
        )
      }
    }

    "must preserve UserAnswers in NormalMode when SubmissionSucceededPage is None" in {
      val existingUserAnswers = emptyUserAnswers
        .withPage(OverwritableOrganisationName, testOrgName)

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockAccountService.getNumberOfRcaspsCurrentlyAdded(any())(any(), any()))
        .thenReturn(ResultT.fromValue(0))

      val application =
        applicationBuilder(
          userAnswers = Some(existingUserAnswers),
          requestUtr = Some(testUtr.uniqueTaxPayerReference)
        ).overrides(bind[AccountService].toInstance(mockAccountService)).build()

      running(application) {
        val request = FakeRequest(GET, routeUnderTest)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER

        verify(mockSessionRepository).set(
          argThat(_.get(OverwritableOrganisationName).contains(testOrgName))
        )
      }
    }

    "must preserve existing UserAnswers in ChangeMode" in {
      val existingUserAnswers = emptyUserAnswers.withPage(OverwritableOrganisationName, testOrgName)

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockAccountService.getNumberOfRcaspsCurrentlyAdded(any())(any(), any()))
        .thenReturn(ResultT.fromValue(1))

      val application =
        applicationBuilder(
          userAnswers = Some(existingUserAnswers),
          requestUtr = Some(testUtr.uniqueTaxPayerReference)
        )
          .overrides(bind[AccountService].toInstance(mockAccountService))
          .build()

      running(application) {
        val request = FakeRequest(GET, changeRouteUnderTest)
        val result  = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          controllers.combined.routes.OrganisationOrIndividualController.onPageLoad(ChangeMode).url

        verify(mockSessionRepository).set(
          argThat((answers: UserAnswers) => answers.get(OverwritableOrganisationName).contains(testOrgName))
        )
      }
    }

    "must create new UserAnswers in ChangeMode when no existing answers are found" in {
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockAccountService.getNumberOfRcaspsCurrentlyAdded(any())(any(), any()))
        .thenReturn(ResultT.fromValue(0))

      val application =
        applicationBuilder(
          userAnswers = None,
          requestUtr = Some(testUtr.uniqueTaxPayerReference)
        )
          .overrides(bind[AccountService].toInstance(mockAccountService))
          .build()

      running(application) {
        val request = FakeRequest(GET, changeRouteUnderTest)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER

        verify(mockSessionRepository).set(
          argThat((answers: UserAnswers) =>
            answers.id == userAnswersId &&
              answers.get(OverwritableOrganisationName).isEmpty
          )
        )
      }
    }
  }
}
