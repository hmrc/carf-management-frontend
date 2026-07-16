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
import models.UserAnswers
import models.errors.ApiError.InternalServerError
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{times, verify, when}
import pages.SubmissionSucceededPage
import pages.changeDetails.ChangeRcaspCachedDetails
import pages.organisation.ReportForRegisteredBusinessPage
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
import utils.{DetailsHelper, RegisteredBusinessDetailsHelper}
import views.html.changeDetails.RegisteredBusinessChangeDetailsView

import scala.concurrent.Future

class RegisteredBusinessChangeDetailsControllerSpec extends SpecBase {

  private val testCompleteUserAnswers = emptyUserAnswers
    .withPage(ReportForRegisteredBusinessPage, true)
    .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate)

  lazy val changeDetailsRoute: String =
    controllers.changeDetails.routes.RegisteredBusinessChangeDetailsController.onPageLoad(rcaspId).url

  "RegisteredBusinessChangeDetails Controller" - {
    "onPageLoad" - {
      "must return OK and the correct view for a GET when all required questions have been answered" in new Setup(
        testCompleteUserAnswers
      ) {
        when(mockDetailsHelper.haveAnswersChangedFromApi(eqTo(testCompleteUserAnswers))).thenReturn(Some(false))

        when(
          mockRegisteredBusinessDetailsHelper
            .getRegisteredBusinessSection(eqTo(testCompleteUserAnswers), eqTo(true))(any())
        ).thenReturn(Some(testSection))

        val request                                   = FakeRequest(GET, changeDetailsRoute)
        val view: RegisteredBusinessChangeDetailsView =
          application.injector.instanceOf[RegisteredBusinessChangeDetailsView]
        val result: Future[Result]                    = route(application, request).value

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(testSection, rcaspId, hasDataChanged = false)(
          request,
          messages(application)
        ).toString
      }

      "must redirect to information is missing page for a GET when a section is none (answers missing)" in new Setup(
        testCompleteUserAnswers
      ) {
        when(mockDetailsHelper.haveAnswersChangedFromApi(eqTo(testCompleteUserAnswers))).thenReturn(Some(false))

        when(
          mockRegisteredBusinessDetailsHelper
            .getRegisteredBusinessSection(eqTo(testCompleteUserAnswers), eqTo(true))(any())
        ).thenReturn(None)

        val request                = FakeRequest(GET, changeDetailsRoute)
        val result: Future[Result] = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.InformationMissingController.onPageLoad().url
      }

      "must redirect to information is missing page for a GET when haveAnswersChangedFromApi returns None (answers missing)" in new Setup(
        testCompleteUserAnswers
      ) {
        when(mockDetailsHelper.haveAnswersChangedFromApi(eqTo(testCompleteUserAnswers))).thenReturn(None)

        when(
          mockRegisteredBusinessDetailsHelper
            .getRegisteredBusinessSection(eqTo(testCompleteUserAnswers), eqTo(true))(any())
        ).thenReturn(Some(testSection))

        val request                = FakeRequest(GET, changeDetailsRoute)
        val result: Future[Result] = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.InformationMissingController.onPageLoad().url
      }

      "must redirect to information is missing page for a GET when ChangeRcaspCachedDetails is missing" in new Setup(
        emptyUserAnswers
      ) {
        val request                = FakeRequest(GET, changeDetailsRoute)
        val result: Future[Result] = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          controllers.routes.InformationMissingController.onPageLoad().url
      }

      "must redirect to information is missing page for a GET when ChangeRcaspCachedDetails contains the wrong RCASPID" in new Setup(
        emptyUserAnswers.withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate.copy(RCASPID = "other"))
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
        testCompleteUserAnswers
      ) {
        when(mockRcaspService.updateRegisteredBusinessRcasp(any(), any(), eqTo(testCompleteUserAnswers))(any(), any()))
          .thenReturn(ResultT.fromValue(submitRcaspResponse))
        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        val request                = FakeRequest(POST, changeDetailsRoute)
        val result: Future[Result] = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.PlaceholderController
          .onPageLoad(s"Successful submission for $rcaspId. Should redirect to /details-updated (CARF-353)")
          .url

        verify(mockRcaspService, times(1))
          .updateRegisteredBusinessRcasp(any(), any(), eqTo(testCompleteUserAnswers))(any(), any())
        verify(mockSessionRepository, times(1)).set(
          eqTo(testCompleteUserAnswers.withPage(SubmissionSucceededPage, true))
        )
      }

      "must redirect to Journey Recovery if submission failed" in new Setup(testCompleteUserAnswers) {
        when(mockRcaspService.updateRegisteredBusinessRcasp(any(), any(), eqTo(testCompleteUserAnswers))(any(), any()))
          .thenReturn(ResultT.fromError(InternalServerError))

        val request                = FakeRequest(POST, changeDetailsRoute)
        val result: Future[Result] = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

        verify(mockRcaspService, times(1))
          .updateRegisteredBusinessRcasp(any(), any(), eqTo(testCompleteUserAnswers))(any(), any())
        verify(mockSessionRepository, times(0)).set(any())
      }

      "must redirect to Journey Recovery if CT UTR is not found in the request" in new Setup(
        testCompleteUserAnswers,
        requestUtr = None
      ) {
        val request                = FakeRequest(POST, changeDetailsRoute)
        val result: Future[Result] = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
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

  class Setup(userAnswers: UserAnswers, requestUtr: Option[String] = Some(testUtr.uniqueTaxPayerReference)) {
    final val mockDetailsHelper                   = mock[DetailsHelper]
    final val mockRegisteredBusinessDetailsHelper = mock[RegisteredBusinessDetailsHelper]
    final val mockRcaspService                    = mock[RcaspSubmissionService]

    val application: Application =
      applicationBuilder(userAnswers = Some(userAnswers), requestUtr = requestUtr)
        .overrides(
          bind[DetailsHelper].toInstance(mockDetailsHelper),
          bind[RegisteredBusinessDetailsHelper].toInstance(mockRegisteredBusinessDetailsHelper),
          bind[RcaspSubmissionService].toInstance(mockRcaspService)
        )
        .build()
  }
}
