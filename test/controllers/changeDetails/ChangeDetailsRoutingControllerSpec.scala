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
import models.UserAnswers
import models.errors.ApiError.{InternalServerError, NotFoundError}
import models.viewAndUpdateRcasp.OrganisationRcaspDetails
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{times, verify, when}
import pages.SubmissionSucceededPage
import pages.changeDetails.ChangeRcaspCachedDetails
import pages.organisation.*
import play.api.Application
import play.api.inject.bind
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.AccountService
import types.ResultT
import utils.changeDetails.PopulateUserAnswersHelper

import scala.concurrent.Future

class ChangeDetailsRoutingControllerSpec extends SpecBase {

  lazy val changeDetailsRoutingRoute: String =
    controllers.changeDetails.routes.ChangeDetailsRoutingController.onPageLoad(rcaspId).url

  "ChangeDetailsRoutingController" - {
    "when ChangeRcaspCachedDetails is set for the selected RCASPID and ReportForRegisteredBusinessPage is present" - {
      "when reportForRegisteredBusiness is true and SubmissionSucceededPage is missing" in new Setup(
        emptyUserAnswers
          .withPage(ReportForRegisteredBusinessPage, true)
          .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate)
      ) {
        val request                = FakeRequest(GET, changeDetailsRoutingRoute)
        val result: Future[Result] = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          controllers.changeDetails.routes.RegisteredBusinessChangeDetailsController.onPageLoad(rcaspId).url

        verify(mockAccountService, times(0)).getRcaspDetails(any(), any())(any(), any())
      }

      "when reportForRegisteredBusiness is false and SubmissionSucceededPage is false" in new Setup(
        emptyUserAnswers
          .withPage(ReportForRegisteredBusinessPage, false)
          .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate)
          .withPage(SubmissionSucceededPage, false)
      ) {
        val request                = FakeRequest(GET, changeDetailsRoutingRoute)
        val result: Future[Result] = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.changeDetails.routes.ChangeDetailsController
          .onPageLoad(rcaspId)
          .url

        verify(mockAccountService, times(0)).getRcaspDetails(any(), any())(any(), any())
      }
    }

    "must call AccountService to get RCASP details" - {
      "when ReportForRegisteredBusinessPage is missing" in new Setup(
        emptyUserAnswers.withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate)
      ) {
        when(mockAccountService.getRcaspDetails(any(), eqTo(rcaspId))(any(), any()))
          .thenReturn(ResultT.fromError(InternalServerError))

        val request                = FakeRequest(GET, changeDetailsRoutingRoute)
        val result: Future[Result] = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

        withClue(
          "The point of this test is not the result, but to check that AccountService .getRcaspDetails has been called"
        ) {
          verify(mockAccountService, times(1)).getRcaspDetails(any(), eqTo(rcaspId))(any(), any())
        }
      }

      "when ChangeRcaspCachedDetails is missing" in new Setup(
        emptyUserAnswers.withPage(ReportForRegisteredBusinessPage, false)
      ) {
        when(mockAccountService.getRcaspDetails(any(), eqTo(rcaspId))(any(), any()))
          .thenReturn(ResultT.fromError(InternalServerError))

        val request                = FakeRequest(GET, changeDetailsRoutingRoute)
        val result: Future[Result] = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

        withClue(
          "The point of this test is not the result, but to check that AccountService .getRcaspDetails has been called"
        ) {
          verify(mockAccountService, times(1)).getRcaspDetails(any(), eqTo(rcaspId))(any(), any())
        }
      }

      "when ChangeRcaspCachedDetails contains a different RCASPID" in new Setup(
        emptyUserAnswers
          .withPage(ReportForRegisteredBusinessPage, false)
          .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate.copy(RCASPID = "other"))
      ) {
        when(mockAccountService.getRcaspDetails(any(), eqTo(rcaspId))(any(), any()))
          .thenReturn(ResultT.fromError(InternalServerError))

        val request                = FakeRequest(GET, changeDetailsRoutingRoute)
        val result: Future[Result] = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

        withClue(
          "The point of this test is not the result, but to check that AccountService .getRcaspDetails has been called"
        ) {
          verify(mockAccountService, times(1)).getRcaspDetails(any(), eqTo(rcaspId))(any(), any())
        }
      }

      "when SubmissionSucceededPage contains true" in new Setup(
        emptyUserAnswers
          .withPage(ReportForRegisteredBusinessPage, false)
          .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate)
          .withPage(SubmissionSucceededPage, true)
      ) {
        when(mockAccountService.getRcaspDetails(any(), eqTo(rcaspId))(any(), any()))
          .thenReturn(ResultT.fromError(InternalServerError))

        val request                = FakeRequest(GET, changeDetailsRoutingRoute)
        val result: Future[Result] = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

        withClue(
          "The point of this test is not the result, but to check that AccountService .getRcaspDetails has been called"
        ) {
          verify(mockAccountService, times(1)).getRcaspDetails(any(), eqTo(rcaspId))(any(), any())
        }
      }
    }

    "given that AccountService is called to get RCASP details" - {
      "must redirect to journey recovery when an error is returned" in new Setup(emptyUserAnswers) {
        when(mockAccountService.getRcaspDetails(any(), eqTo(rcaspId))(any(), any()))
          .thenReturn(ResultT.fromError(NotFoundError))

        val request                = FakeRequest(GET, changeDetailsRoutingRoute)
        val result: Future[Result] = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

        verify(mockAccountService, times(1)).getRcaspDetails(any(), eqTo(rcaspId))(any(), any())
      }

      "must redirect to journey recovery when an IndividualRcaspDetails is returned but IsRCASPUser = true" in new Setup(
        emptyUserAnswers
      ) {
        when(mockAccountService.getRcaspDetails(any(), eqTo(rcaspId))(any(), any()))
          .thenReturn(ResultT.fromValue(individualRcaspDetailsViewUpdate.copy(IsRCASPUser = true)))

        val request                = FakeRequest(GET, changeDetailsRoutingRoute)
        val result: Future[Result] = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

        verify(mockAccountService, times(1)).getRcaspDetails(any(), eqTo(rcaspId))(any(), any())
      }

      "must call PopulateUserAnswersHelper when an IndividualRcaspDetails is returned and IsRCASPUser = false" in new Setup(
        emptyUserAnswers
      ) {
        when(mockAccountService.getRcaspDetails(any(), eqTo(rcaspId))(any(), any()))
          .thenReturn(ResultT.fromValue(individualRcaspDetailsViewUpdate))
        when(
          mockPopulateUserAnswersHelper.populateUserAnswersForIndividual(any(), eqTo(individualRcaspDetailsViewUpdate))
        ).thenReturn(
          Future.successful(Redirect(controllers.changeDetails.routes.ChangeDetailsController.onPageLoad(rcaspId)))
        )

        val request                = FakeRequest(GET, changeDetailsRoutingRoute)
        val result: Future[Result] = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.changeDetails.routes.ChangeDetailsController
          .onPageLoad(rcaspId)
          .url

        verify(mockAccountService, times(1)).getRcaspDetails(any(), eqTo(rcaspId))(any(), any())
        verify(mockPopulateUserAnswersHelper, times(1))
          .populateUserAnswersForIndividual(any(), eqTo(individualRcaspDetailsViewUpdate))
      }

      "must call PopulateUserAnswersHelper when an OrganisationRcaspDetails is returned and IsRCASPUser = false" in new Setup(
        emptyUserAnswers
      ) {
        when(mockAccountService.getRcaspDetails(any(), eqTo(rcaspId))(any(), any()))
          .thenReturn(ResultT.fromValue(organisationRcaspDetailsViewUpdate))
        when(
          mockPopulateUserAnswersHelper.populateUserAnswersForOrganisation(
            any(),
            eqTo(organisationRcaspDetailsViewUpdate)
          )
        ).thenReturn(
          Future.successful(Redirect(controllers.changeDetails.routes.ChangeDetailsController.onPageLoad(rcaspId)))
        )

        val request                = FakeRequest(GET, changeDetailsRoutingRoute)
        val result: Future[Result] = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.changeDetails.routes.ChangeDetailsController
          .onPageLoad(rcaspId)
          .url

        verify(mockAccountService, times(1)).getRcaspDetails(any(), eqTo(rcaspId))(any(), any())
        verify(mockPopulateUserAnswersHelper, times(1))
          .populateUserAnswersForOrganisation(any(), eqTo(organisationRcaspDetailsViewUpdate))
      }

      "must call PopulateUserAnswersHelper when an OrganisationRcaspDetails is returned, IsRCASPUser = true and CT UTR is found in the request" in new Setup(
        emptyUserAnswers
      ) {
        val rcaspDetails: OrganisationRcaspDetails =
          organisationRcaspDetailsViewUpdate.copy(
            IsRCASPUser = true,
            PrimaryContactDetails = None,
            SecondaryContactDetails = None
          )

        when(mockAccountService.getRcaspDetails(any(), eqTo(rcaspId))(any(), any()))
          .thenReturn(ResultT.fromValue(rcaspDetails))
        when(
          mockPopulateUserAnswersHelper
            .populateUserAnswersForRegisteredBusiness(any(), eqTo(testUtr), eqTo(rcaspDetails))(any())
        )
          .thenReturn(
            Future.successful(
              Redirect(controllers.changeDetails.routes.RegisteredBusinessChangeDetailsController.onPageLoad(rcaspId))
            )
          )

        val request                = FakeRequest(GET, changeDetailsRoutingRoute)
        val result: Future[Result] = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          controllers.changeDetails.routes.RegisteredBusinessChangeDetailsController.onPageLoad(rcaspId).url

        verify(mockAccountService, times(1)).getRcaspDetails(any(), eqTo(rcaspId))(any(), any())
        verify(mockPopulateUserAnswersHelper, times(1))
          .populateUserAnswersForRegisteredBusiness(any(), eqTo(testUtr), eqTo(rcaspDetails))(any())
      }

      "must redirect to journey recovery when an OrganisationRcaspDetails is returned, IsRCASPUser = true and CT UTR is not found in the request" in new Setup(
        emptyUserAnswers,
        requestUtr = None
      ) {
        val rcaspDetails: OrganisationRcaspDetails =
          organisationRcaspDetailsViewUpdate.copy(
            IsRCASPUser = true,
            PrimaryContactDetails = None,
            SecondaryContactDetails = None
          )

        when(mockAccountService.getRcaspDetails(any(), eqTo(rcaspId))(any(), any()))
          .thenReturn(ResultT.fromValue(rcaspDetails))

        val request                = FakeRequest(GET, changeDetailsRoutingRoute)
        val result: Future[Result] = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

        verify(mockAccountService, times(1)).getRcaspDetails(any(), eqTo(rcaspId))(any(), any())
        verify(mockPopulateUserAnswersHelper, times(0))
          .populateUserAnswersForRegisteredBusiness(any(), any(), any())(any())
      }
    }
  }

  class Setup(userAnswers: UserAnswers, requestUtr: Option[String] = Some(testUtr.uniqueTaxPayerReference)) {
    val mockAccountService: AccountService                       = mock[AccountService]
    val mockPopulateUserAnswersHelper: PopulateUserAnswersHelper = mock[PopulateUserAnswersHelper]

    val application: Application =
      applicationBuilder(userAnswers = Some(userAnswers), requestUtr = requestUtr)
        .overrides(
          bind[AccountService].toInstance(mockAccountService),
          bind[PopulateUserAnswersHelper].toInstance(mockPopulateUserAnswersHelper)
        )
        .build()
  }
}
