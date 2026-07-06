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
import controllers.actions.*
import models.OrganisationOrIndividual.{Individual, Organisation}
import models.UserAnswers
import models.errors.ApiError.InternalServerError
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{times, verify, when}
import pages.combined.OrganisationOrIndividualPage
import pages.individual.IndividualNamePage
import pages.organisation.OverwritableOrganisationName
import pages.{RcaspIdPage, SubmissionSucceededPage}
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import services.RcaspSubmissionService
import types.ResultT
import uk.gov.hmrc.auth.core.AffinityGroup
import utils.CheckDetailsHelper
import viewmodels.Section
import views.html.CheckDetailsView

import scala.concurrent.Future

class CheckDetailsControllerSpec extends SpecBase {

  private val individualCompleteUserAnswers   = emptyUserAnswers
    .withPage(OrganisationOrIndividualPage, Individual)
    .withPage(IndividualNamePage, testIndividualName)
  private val organisationCompleteUserAnswers = emptyUserAnswers
    .withPage(OrganisationOrIndividualPage, Organisation)
    .withPage(OverwritableOrganisationName, testOrgName)

  lazy val checkDetailsRoute: String = routes.CheckDetailsController.onPageLoad.url

  "Check Details Controller" - {
    "onPageLoad" - {
      "when Individual as RCASP" - {
        "must return OK and the correct view for a GET when all required questions have been answered" in new Setup(
          individualCompleteUserAnswers
        ) {

          when(mockCdHelper.getIndividualSectionMaybe(eqTo(individualCompleteUserAnswers))(any()))
            .thenReturn(Some(testSection))

          when(mockCdHelper.getIndividualContactDetailsMaybe(eqTo(individualCompleteUserAnswers))(any()))
            .thenReturn(Some(testSection))

          val request                = FakeRequest(GET, checkDetailsRoute)
          val view: CheckDetailsView = application.injector.instanceOf[CheckDetailsView]
          val result: Future[Result] = route(application, request).value

          status(result)          mustEqual OK
          contentAsString(result) mustEqual view(Seq(testSection, testSection), testIndividualName.fullName)(
            request,
            messages(application)
          ).toString
        }

        "must redirect to information is missing page for a GET when a section is none (answers missing)" in new Setup(
          individualCompleteUserAnswers
        ) {

          when(mockCdHelper.getIndividualSectionMaybe(eqTo(individualCompleteUserAnswers))(any()))
            .thenReturn(None)

          when(mockCdHelper.getIndividualContactDetailsMaybe(eqTo(individualCompleteUserAnswers))(any()))
            .thenReturn(Some(testSection))

          val request                = FakeRequest(GET, checkDetailsRoute)
          val result: Future[Result] = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual
            controllers.routes.InformationMissingController.onPageLoad().url
        }
      }

      "when Organisation as RCASP" - {
        "must return OK and the correct view for a GET when all required questions have been answered" in new Setup(
          organisationCompleteUserAnswers
        ) {
          when(mockCdHelper.getOrganisationSectionMaybe(eqTo(organisationCompleteUserAnswers))(any()))
            .thenReturn(Some(testSection))

          when(mockCdHelper.getOrganisationFirstContactDetailsMaybe(eqTo(organisationCompleteUserAnswers))(any()))
            .thenReturn(Some(testSection))

          when(mockCdHelper.getOrganisationSecondContactDetailsMaybe(eqTo(organisationCompleteUserAnswers))(any()))
            .thenReturn(Some(testSection))

          val request                = FakeRequest(GET, checkDetailsRoute)
          val view: CheckDetailsView = application.injector.instanceOf[CheckDetailsView]
          val result: Future[Result] = route(application, request).value

          status(result)          mustEqual OK
          contentAsString(result) mustEqual view(Seq(testSection, testSection, testSection), testOrgName)(
            request,
            messages(application)
          ).toString
        }

        "must redirect to information is missing page for a GET when a section is none (answers missing)" in new Setup(
          organisationCompleteUserAnswers
        ) {
          when(mockCdHelper.getOrganisationSectionMaybe(eqTo(organisationCompleteUserAnswers))(any()))
            .thenReturn(Some(testSection))

          when(mockCdHelper.getOrganisationFirstContactDetailsMaybe(eqTo(organisationCompleteUserAnswers))(any()))
            .thenReturn(Some(testSection))

          when(mockCdHelper.getOrganisationSecondContactDetailsMaybe(eqTo(organisationCompleteUserAnswers))(any()))
            .thenReturn(None)

          val request                = FakeRequest(GET, checkDetailsRoute)
          val result: Future[Result] = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.InformationMissingController.onPageLoad().url
        }
      }

      "must redirect to information is missing page for a GET when OrganisationOrIndividualPage is missing" in new Setup(
        emptyUserAnswers
      ) {

        val request                = FakeRequest(GET, checkDetailsRoute)
        val view: CheckDetailsView = application.injector.instanceOf[CheckDetailsView]
        val result: Future[Result] = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          controllers.routes.InformationMissingController.onPageLoad().url
      }

      "must redirect to Journey Recovery for a GET if no existing data is found" in {
        val application = applicationBuilder(userAnswers = None).build()
        running(application) {
          val request = FakeRequest(GET, checkDetailsRoute)
          val result  = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to the page unavailable placeholder for a GET when submission has already succeeded" in {
        val userAnswers = emptyUserAnswers.withPage(SubmissionSucceededPage, true)

        val application = new GuiceApplicationBuilder()
          .overrides(
            bind[DataRequiredAction].to[DataRequiredActionImpl],
            bind[IdentifierAction]
              .toInstance(new FakeIdentifierAction(injectedParsers, AffinityGroup.Individual, None)),
            bind[DataRetrievalAction].toInstance(new FakeDataRetrievalActionProvider(Some(userAnswers))),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, checkDetailsRoute)
          val result  = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.PlaceholderController
            .onPageLoad("Should nav to /problem/page-unavailable (CARF-308)")
            .url
        }
      }
    }

    "onSubmit" - {
      "must set the SubmissionSucceededPage flag as true and redirect to the RCASP added page if submission is successful" in new Setup(
        individualCompleteUserAnswers
      ) {
        when(mockRcaspService.submitRcasp(any(), eqTo(individualCompleteUserAnswers))(any(), any()))
          .thenReturn(ResultT.fromValue(submitRcaspResponse))
        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        val request                = FakeRequest(POST, checkDetailsRoute)
        val result: Future[Result] = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.RcaspAddedConfirmationController.onPageLoad().url

        verify(mockRcaspService, times(1)).submitRcasp(any(), eqTo(individualCompleteUserAnswers))(any(), any())
        verify(mockSessionRepository, times(1)).set(
          eqTo(
            individualCompleteUserAnswers
              .withPage(RcaspIdPage, rcaspId)
              .withPage(SubmissionSucceededPage, true)
          )
        )
      }

      "must redirect to Journey Recovery if submission failed" in new Setup(individualCompleteUserAnswers) {
        when(mockRcaspService.submitRcasp(any(), eqTo(individualCompleteUserAnswers))(any(), any()))
          .thenReturn(ResultT.fromError(InternalServerError))

        val request                = FakeRequest(POST, checkDetailsRoute)
        val result: Future[Result] = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

        verify(mockRcaspService, times(1)).submitRcasp(any(), eqTo(individualCompleteUserAnswers))(any(), any())
        verify(mockSessionRepository, times(0)).set(any())
      }

      "must redirect to Journey Recovery for a POST if no existing data is found" in {
        val application = applicationBuilder(userAnswers = None).build()
        running(application) {
          val request = FakeRequest(POST, checkDetailsRoute)

          val result = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to the page unavailable placeholder for a POST when submission has already succeeded" in {
        val userAnswers = emptyUserAnswers.withPage(SubmissionSucceededPage, true)

        val application = new GuiceApplicationBuilder()
          .overrides(
            bind[DataRequiredAction].to[DataRequiredActionImpl],
            bind[IdentifierAction]
              .toInstance(new FakeIdentifierAction(injectedParsers, AffinityGroup.Individual, None)),
            bind[DataRetrievalAction].toInstance(new FakeDataRetrievalActionProvider(Some(userAnswers))),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, checkDetailsRoute)
          val result  = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.PlaceholderController
            .onPageLoad("Should nav to /problem/page-unavailable (CARF-308)")
            .url
        }
      }
    }
  }

  class Setup(userAnswers: UserAnswers) {
    final val mockCdHelper     = mock[CheckDetailsHelper]
    final val mockRcaspService = mock[RcaspSubmissionService]

    val application: Application =
      applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(
          bind[CheckDetailsHelper].toInstance(mockCdHelper),
          bind[RcaspSubmissionService].toInstance(mockRcaspService)
        )
        .build()
  }
}
