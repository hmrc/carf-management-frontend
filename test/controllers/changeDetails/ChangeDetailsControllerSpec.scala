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

package controllers.changeDetails

import base.SpecBase
import controllers.actions.*
import models.OrganisationOrIndividual.{Individual, Organisation}
import models.UserAnswers
import models.errors.ApiError.InternalServerError
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{times, verify, when}
import pages.SubmissionSucceededPage
import pages.changeDetails.ChangeRcaspCachedDetails
import pages.combined.OrganisationOrIndividualPage
import pages.individual.IndividualNamePage
import pages.organisation.{OverwritableOrganisationName, ReportForRegisteredBusinessPage}
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
import utils.DetailsHelper
import viewmodels.Section
import views.html.changeDetails.ChangeDetailsView

import scala.concurrent.Future

class ChangeDetailsControllerSpec extends SpecBase {

  private val individualCompleteUserAnswers   = emptyUserAnswers
    .withPage(ReportForRegisteredBusinessPage, false)
    .withPage(OrganisationOrIndividualPage, Individual)
    .withPage(IndividualNamePage, testIndividualName)
    .withPage(ChangeRcaspCachedDetails, individualRcaspDetailsViewUpdate)
  private val organisationCompleteUserAnswers = emptyUserAnswers
    .withPage(ReportForRegisteredBusinessPage, false)
    .withPage(OrganisationOrIndividualPage, Organisation)
    .withPage(OverwritableOrganisationName, testOrgName)
    .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate)

  lazy val changeDetailsRoute: String = controllers.changeDetails.routes.ChangeDetailsController.onPageLoad(rcaspId).url

  "ChangeDetails Controller" - {
    "onPageLoad" - {
      "when changing details for an Individual RCASP" - {
        "must return OK and the correct view for a GET when all required questions have been answered" in new Setup(
          individualCompleteUserAnswers
        ) {
          when(mockCdHelper.haveAnswersChangedFromApi(eqTo(individualCompleteUserAnswers))).thenReturn(Some(true))

          when(mockCdHelper.getIndividualSectionMaybe(eqTo(individualCompleteUserAnswers), eqTo(true))(any()))
            .thenReturn(Some(testSection))

          when(mockCdHelper.getIndividualContactDetailsMaybe(eqTo(individualCompleteUserAnswers))(any()))
            .thenReturn(Some(testSection))

          val request                 = FakeRequest(GET, changeDetailsRoute)
          val view: ChangeDetailsView = application.injector.instanceOf[ChangeDetailsView]
          val result: Future[Result]  = route(application, request).value

          status(result)          mustEqual OK
          contentAsString(result) mustEqual view(
            sections = Seq(testSection, testSection),
            testIndividualName.fullName,
            rcaspId,
            hasDataChanged = true
          )(request, messages(application)).toString
        }

        "must redirect to information is missing page for a GET when a section is none (answers missing)" in new Setup(
          individualCompleteUserAnswers
        ) {
          when(mockCdHelper.haveAnswersChangedFromApi(eqTo(individualCompleteUserAnswers))).thenReturn(Some(true))

          when(mockCdHelper.getIndividualSectionMaybe(eqTo(individualCompleteUserAnswers), eqTo(true))(any()))
            .thenReturn(None)

          when(mockCdHelper.getIndividualContactDetailsMaybe(eqTo(individualCompleteUserAnswers))(any()))
            .thenReturn(Some(testSection))

          val request                = FakeRequest(GET, changeDetailsRoute)
          val result: Future[Result] = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual
            controllers.routes.InformationMissingController.onPageLoad().url
        }

        "must redirect to information is missing page for a GET when haveAnswersChangedFromApi returns None (answers missing)" in new Setup(
          individualCompleteUserAnswers
        ) {
          when(mockCdHelper.haveAnswersChangedFromApi(eqTo(individualCompleteUserAnswers))).thenReturn(None)

          when(mockCdHelper.getIndividualSectionMaybe(eqTo(individualCompleteUserAnswers), eqTo(true))(any()))
            .thenReturn(Some(testSection))

          when(mockCdHelper.getIndividualContactDetailsMaybe(eqTo(individualCompleteUserAnswers))(any()))
            .thenReturn(Some(testSection))

          val request                = FakeRequest(GET, changeDetailsRoute)
          val result: Future[Result] = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual
            controllers.routes.InformationMissingController.onPageLoad().url
        }
      }

      "when changing details for an Organisation RCASP" - {
        "must return OK and the correct view for a GET when all required questions have been answered" in new Setup(
          organisationCompleteUserAnswers
        ) {
          when(mockCdHelper.haveAnswersChangedFromApi(eqTo(organisationCompleteUserAnswers))).thenReturn(Some(false))

          when(mockCdHelper.getOrganisationSectionMaybe(eqTo(organisationCompleteUserAnswers), eqTo(true))(any()))
            .thenReturn(Some(testSection))

          when(mockCdHelper.getOrganisationFirstContactDetailsMaybe(eqTo(organisationCompleteUserAnswers))(any()))
            .thenReturn(Some(testSection))

          when(mockCdHelper.getOrganisationSecondContactDetailsMaybe(eqTo(organisationCompleteUserAnswers))(any()))
            .thenReturn(Some(testSection))

          val request                 = FakeRequest(GET, changeDetailsRoute)
          val view: ChangeDetailsView = application.injector.instanceOf[ChangeDetailsView]
          val result: Future[Result]  = route(application, request).value

          status(result)          mustEqual OK
          contentAsString(result) mustEqual view(
            sections = Seq(testSection, testSection, testSection),
            testOrgName,
            rcaspId,
            hasDataChanged = false
          )(request, messages(application)).toString
        }

        "must redirect to information is missing page for a GET when a section is none (answers missing)" in new Setup(
          organisationCompleteUserAnswers
        ) {
          when(mockCdHelper.haveAnswersChangedFromApi(eqTo(organisationCompleteUserAnswers))).thenReturn(Some(false))

          when(mockCdHelper.getOrganisationSectionMaybe(eqTo(organisationCompleteUserAnswers), eqTo(true))(any()))
            .thenReturn(Some(testSection))

          when(mockCdHelper.getOrganisationFirstContactDetailsMaybe(eqTo(organisationCompleteUserAnswers))(any()))
            .thenReturn(Some(testSection))

          when(mockCdHelper.getOrganisationSecondContactDetailsMaybe(eqTo(organisationCompleteUserAnswers))(any()))
            .thenReturn(None)

          val request                = FakeRequest(GET, changeDetailsRoute)
          val result: Future[Result] = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.InformationMissingController.onPageLoad().url
        }

        "must redirect to information is missing page for a GET when haveAnswersChangedFromApi returns None (answers missing)" in new Setup(
          organisationCompleteUserAnswers
        ) {
          when(mockCdHelper.haveAnswersChangedFromApi(eqTo(organisationCompleteUserAnswers))).thenReturn(None)

          when(mockCdHelper.getOrganisationSectionMaybe(eqTo(organisationCompleteUserAnswers), eqTo(true))(any()))
            .thenReturn(Some(testSection))

          when(mockCdHelper.getOrganisationFirstContactDetailsMaybe(eqTo(organisationCompleteUserAnswers))(any()))
            .thenReturn(Some(testSection))

          when(mockCdHelper.getOrganisationSecondContactDetailsMaybe(eqTo(organisationCompleteUserAnswers))(any()))
            .thenReturn(Some(testSection))

          val request                = FakeRequest(GET, changeDetailsRoute)
          val result: Future[Result] = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.InformationMissingController.onPageLoad().url
        }
      }

      "must redirect to information is missing page for a GET when OrganisationOrIndividualPage is missing" in new Setup(
        emptyUserAnswers.withPage(ChangeRcaspCachedDetails, individualRcaspDetailsViewUpdate)
      ) {
        val request                = FakeRequest(GET, changeDetailsRoute)
        val result: Future[Result] = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          controllers.routes.InformationMissingController.onPageLoad().url
      }

      "must redirect to information is missing page for a GET when ChangeRcaspCachedDetails is missing" in new Setup(
        emptyUserAnswers.withPage(OrganisationOrIndividualPage, Individual)
      ) {
        val request                = FakeRequest(GET, changeDetailsRoute)
        val result: Future[Result] = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          controllers.routes.InformationMissingController.onPageLoad().url
      }

      "must redirect to information is missing page for a GET when ChangeRcaspCachedDetails contains the wrong RCASPID" in new Setup(
        emptyUserAnswers
          .withPage(OrganisationOrIndividualPage, Individual)
          .withPage(ChangeRcaspCachedDetails, individualRcaspDetailsViewUpdate.copy(RCASPID = "other"))
      ) {
        val request                = FakeRequest(GET, changeDetailsRoute)
        val result: Future[Result] = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          controllers.routes.InformationMissingController.onPageLoad().url
      }

      "must redirect to Journey Recovery for a GET if no existing data is found" in {
        val application = applicationBuilder(userAnswers = None).build()
        running(application) {
          val request = FakeRequest(GET, changeDetailsRoute)
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
          val request = FakeRequest(GET, changeDetailsRoute)
          val result  = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.PlaceholderController
            .onPageLoad("Should nav to /problem/page-unavailable (CARF-308)")
            .url
        }
      }
    }

    "onSubmit" - {
      "must set the SubmissionSucceededPage flag as true and redirect to the details updated page if submission is successful" in new Setup(
        individualCompleteUserAnswers
      ) {
        when(mockRcaspService.updateRcasp(any(), eqTo(individualCompleteUserAnswers))(any(), any()))
          .thenReturn(ResultT.fromValue(updateDeleteRcaspResponse))
        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        val request                = FakeRequest(POST, changeDetailsRoute)
        val result: Future[Result] = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          controllers.changeDetails.routes.RcaspUpdatedConfirmationController.onPageLoad().url

        verify(mockRcaspService, times(1)).updateRcasp(any(), eqTo(individualCompleteUserAnswers))(any(), any())
        verify(mockSessionRepository, times(1)).set(
          eqTo(individualCompleteUserAnswers.withPage(SubmissionSucceededPage, true))
        )
      }

      "must redirect to Journey Recovery if submission failed" in new Setup(individualCompleteUserAnswers) {
        when(mockRcaspService.updateRcasp(any(), eqTo(individualCompleteUserAnswers))(any(), any()))
          .thenReturn(ResultT.fromError(InternalServerError))

        val request                = FakeRequest(POST, changeDetailsRoute)
        val result: Future[Result] = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

        verify(mockRcaspService, times(1)).updateRcasp(any(), eqTo(individualCompleteUserAnswers))(any(), any())
        verify(mockSessionRepository, times(0)).set(any())
      }

      "must redirect to Journey Recovery for a POST if no existing data is found" in {
        val application = applicationBuilder(userAnswers = None).build()
        running(application) {
          val request = FakeRequest(POST, changeDetailsRoute)

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
          val request = FakeRequest(POST, changeDetailsRoute)
          val result  = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.PlaceholderController
            .onPageLoad("Should nav to /problem/page-unavailable (CARF-308)")
            .url
        }
      }
    }
  }

  class Setup(userAnswers: UserAnswers) {
    final val mockCdHelper     = mock[DetailsHelper]
    final val mockRcaspService = mock[RcaspSubmissionService]

    val application: Application =
      applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(
          bind[DetailsHelper].toInstance(mockCdHelper),
          bind[RcaspSubmissionService].toInstance(mockRcaspService)
        )
        .build()
  }
}
