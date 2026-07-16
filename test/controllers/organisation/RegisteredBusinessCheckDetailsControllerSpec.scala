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

package controllers.organisation

import base.SpecBase
import controllers.actions.*
import models.UserAnswers
import models.errors.ApiError.InternalServerError
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{times, verify, when}
import pages.organisation.{OverwritableOrganisationName, ReportForRegisteredBusinessPage}
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
import utils.RegisteredBusinessDetailsHelper
import views.html.organisation.RegisteredBusinessCheckDetailsView

import scala.concurrent.Future

class RegisteredBusinessCheckDetailsControllerSpec extends SpecBase {

  lazy val cdRoute: String = controllers.organisation.routes.RegisteredBusinessCheckDetailsController.onPageLoad.url

  "RegisteredBusinessCheckDetailsController" - {

    "onPageLoad" - {

      "must return OK and the correct view for a GET when all answers are present" in new Setup(
        emptyUserAnswers
          .withPage(ReportForRegisteredBusinessPage, true)
          .withPage(OverwritableOrganisationName, "Test Business Ltd")
      ) {
        when(
          mockHelper.getRegisteredBusinessSection(
            eqTo(
              emptyUserAnswers
                .withPage(ReportForRegisteredBusinessPage, true)
                .withPage(OverwritableOrganisationName, "Test Business Ltd")
            ),
            eqTo(false)
          )(any())
        )
          .thenReturn(Some(testSection))

        val request                                  = FakeRequest(GET, cdRoute)
        val view: RegisteredBusinessCheckDetailsView =
          application.injector.instanceOf[RegisteredBusinessCheckDetailsView]
        val result: Future[Result]                   = route(application, request).value

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(testSection, "Test Business Ltd")(
          request,
          messages(application)
        ).toString
      }

      "must redirect to InformationMissing when section is None" in new Setup(
        emptyUserAnswers
          .withPage(ReportForRegisteredBusinessPage, true)
          .withPage(OverwritableOrganisationName, "Test Business Ltd")
      ) {
        when(mockHelper.getRegisteredBusinessSection(any(), any())(any())).thenReturn(None)

        val request                = FakeRequest(GET, cdRoute)
        val result: Future[Result] = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.InformationMissingController.onPageLoad().url
      }

      "must redirect to InformationMissing when OverwritableOrganisationName is missing" in new Setup(
        emptyUserAnswers.withPage(ReportForRegisteredBusinessPage, true)
      ) {
        val request                = FakeRequest(GET, cdRoute)
        val result: Future[Result] = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.InformationMissingController.onPageLoad().url
      }

      "must redirect to InformationMissing when ReportForRegisteredBusiness is false" in new Setup(
        emptyUserAnswers.withPage(ReportForRegisteredBusinessPage, false)
      ) {
        val request                = FakeRequest(GET, cdRoute)
        val result: Future[Result] = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.InformationMissingController.onPageLoad().url
      }

      "must redirect to InformationMissing when ReportForRegisteredBusiness is None" in new Setup(
        emptyUserAnswers.withPage(OverwritableOrganisationName, "Test Business Ltd")
      ) {
        val request                = FakeRequest(GET, cdRoute)
        val result: Future[Result] = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.InformationMissingController.onPageLoad().url
      }

      "must redirect to Journey Recovery for a GET if no existing data is found" in {
        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(GET, cdRoute)
          val result  = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    "onSubmit" - {
      "must set the SubmissionSucceededPage flag as true redirect to the RCASP added page if submission is successful" in new Setup(
        emptyUserAnswers
      ) {
        when(mockRcaspSubmissionService.submitRegisteredBusinessRcasp(any(), any(), any())(any(), any()))
          .thenReturn(ResultT.fromValue(submitRcaspResponse))
        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        val request                = FakeRequest(POST, cdRoute)
        val result: Future[Result] = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.RcaspAddedConfirmationController.onPageLoad().url

        verify(mockRcaspSubmissionService, times(1)).submitRegisteredBusinessRcasp(any(), any(), any())(any(), any())
        verify(mockSessionRepository, times(1)).set(
          eqTo(
            emptyUserAnswers
              .withPage(RcaspIdPage, rcaspId)
              .withPage(SubmissionSucceededPage, true)
          )
        )
      }

      "must redirect to Journey Recovery if submission failed" in new Setup(emptyUserAnswers) {
        when(mockRcaspSubmissionService.submitRegisteredBusinessRcasp(any(), any(), any())(any(), any()))
          .thenReturn(ResultT.fromError(InternalServerError))

        val request                = FakeRequest(POST, cdRoute)
        val result: Future[Result] = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

        verify(mockRcaspSubmissionService, times(1)).submitRegisteredBusinessRcasp(any(), any(), any())(any(), any())
        verify(mockSessionRepository, times(0)).set(eqTo(emptyUserAnswers))
      }

      "must redirect to Journey Recovery if CT UTR is not found in the request" in new Setup(
        emptyUserAnswers,
        requestUtr = None
      ) {
        val request                = FakeRequest(POST, cdRoute)
        val result: Future[Result] = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }

      "must redirect to Journey Recovery for a POST if no existing data is found" in {
        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(POST, cdRoute)

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
              .toInstance(
                new FakeIdentifierAction(
                  injectedParsers,
                  AffinityGroup.Organisation,
                  Some(testUtr.uniqueTaxPayerReference)
                )
              ),
            bind[DataRetrievalAction].toInstance(new FakeDataRetrievalActionProvider(Some(userAnswers))),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, cdRoute)
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
    final val mockHelper: RegisteredBusinessDetailsHelper        = mock[RegisteredBusinessDetailsHelper]
    final val mockRcaspSubmissionService: RcaspSubmissionService = mock[RcaspSubmissionService]

    val application: Application =
      applicationBuilder(userAnswers = Some(userAnswers), requestUtr = requestUtr)
        .overrides(
          bind[RegisteredBusinessDetailsHelper].toInstance(mockHelper),
          bind[RcaspSubmissionService].toInstance(mockRcaspSubmissionService)
        )
        .build()
  }
}
