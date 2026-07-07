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
import models.OrganisationOrIndividual.{Individual, Organisation}
import models.UserAnswers
import models.errors.ApiError.{InternalServerError, NotFoundError}
import models.responses.{IndividualRcaspDetails, OrganisationRcaspDetails}
import org.mockito.ArgumentMatchers.{any, argThat, eq as eqTo}
import org.mockito.Mockito.{times, verify, when}
import pages.{SubmissionSucceededPage, UkAddressInUserAnswers}
import pages.changeDetails.ChangeRcaspCachedDetails
import pages.combined.OrganisationOrIndividualPage
import pages.individual.*
import pages.organisation.*
import play.api.Application
import play.api.inject.bind
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.AccountService
import types.ResultT

import scala.concurrent.Future

class ChangeDetailsRoutingControllerSpec extends SpecBase {

  lazy val changeDetailsRoutingRoute: String =
    controllers.changeDetails.routes.ChangeDetailsRoutingController.onPageLoad(rcaspId).url

  "ChangeDetailsRoutingController" - {
    "when ChangeRcaspCachedDetails is set for the selected RCASPID and ReportForRegisteredBusinessPage is present" - {
      "when reportForRegisteredBusiness is true and SubmissionSucceededPage is missing" in new Setup(
        emptyUserAnswers
          .withPage(ReportForRegisteredBusinessPage, true)
          .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsResponse)
      ) {
        val request                = FakeRequest(GET, changeDetailsRoutingRoute)
        val result: Future[Result] = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.PlaceholderController
          .onPageLoad(s"Should nav to registered-business/change-answers/$rcaspId (CARF-350)")
          .url

        verify(mockAccountService, times(0)).getRcaspDetails(any(), any())(any(), any())
      }

      "when reportForRegisteredBusiness is false and SubmissionSucceededPage is false" in new Setup(
        emptyUserAnswers
          .withPage(ReportForRegisteredBusinessPage, false)
          .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsResponse)
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
        emptyUserAnswers.withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsResponse)
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
          .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsResponse.copy(RCASPID = "other"))
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
          .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsResponse)
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
          .thenReturn(ResultT.fromValue(individualRcaspDetailsResponse.copy(IsRCASPUser = true)))

        val request                = FakeRequest(GET, changeDetailsRoutingRoute)
        val result: Future[Result] = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

        verify(mockAccountService, times(1)).getRcaspDetails(any(), eqTo(rcaspId))(any(), any())
      }

      "when an IndividualRcaspDetails is returned and IsRCASPUser = false" - {
        "must populate user answers when all fields are present" in new Setup(emptyUserAnswers) {
          val rcaspDetails: IndividualRcaspDetails = individualRcaspDetailsResponse

          when(mockAccountService.getRcaspDetails(any(), eqTo(rcaspId))(any(), any()))
            .thenReturn(ResultT.fromValue(rcaspDetails))
          when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

          val request                = FakeRequest(GET, changeDetailsRoutingRoute)
          val result: Future[Result] = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.changeDetails.routes.ChangeDetailsController
            .onPageLoad(rcaspId)
            .url

          verify(mockAccountService, times(1)).getRcaspDetails(any(), eqTo(rcaspId))(any(), any())
          verify(mockSessionRepository, times(1)).set(
            argThat { ua =>
              !ua.rcaspIsRegisteredBusiness &&
              ua.get(ReportForRegisteredBusinessPage).contains(false) &&
              ua.get(OrganisationOrIndividualPage).contains(Individual) &&
              ua.get(IndividualNamePage).contains(testIndividualName) &&
              ua.get(NiNumberPage).contains(testNiNumber) &&
              ua.get(UkAddressInUserAnswers).contains(testAddressUk) &&
              ua.get(IndividualEmailPage).contains(testEmail) &&
              ua.get(IndividualHavePhonePage).contains(true) &&
              ua.get(IndividualPhonePage).contains(testPhone) &&
              ua.get(ChangeRcaspCachedDetails).contains(rcaspDetails)
            }
          )
        }

        "must populate user answers when phone number is absent" in new Setup(emptyUserAnswers) {
          val rcaspDetails: IndividualRcaspDetails =
            individualRcaspDetailsResponse.copy(
              PrimaryContactDetails = Some(rcaspContactDetails.copy(PhoneNumber = None))
            )

          when(mockAccountService.getRcaspDetails(any(), eqTo(rcaspId))(any(), any()))
            .thenReturn(ResultT.fromValue(rcaspDetails))
          when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

          val request                = FakeRequest(GET, changeDetailsRoutingRoute)
          val result: Future[Result] = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.changeDetails.routes.ChangeDetailsController
            .onPageLoad(rcaspId)
            .url

          verify(mockAccountService, times(1)).getRcaspDetails(any(), eqTo(rcaspId))(any(), any())
          verify(mockSessionRepository, times(1)).set(
            argThat { ua =>
              !ua.rcaspIsRegisteredBusiness &&
              ua.get(ReportForRegisteredBusinessPage).contains(false) &&
              ua.get(OrganisationOrIndividualPage).contains(Individual) &&
              ua.get(IndividualNamePage).contains(testIndividualName) &&
              ua.get(NiNumberPage).contains(testNiNumber) &&
              ua.get(UkAddressInUserAnswers).contains(testAddressUk) &&
              ua.get(IndividualEmailPage).contains(testEmail) &&
              ua.get(IndividualHavePhonePage).contains(false) &&
              ua.get(IndividualPhonePage).isEmpty &&
              ua.get(ChangeRcaspCachedDetails).contains(rcaspDetails)
            }
          )
        }

        "must redirect to journey recovery" - {
          "when TINDetails is missing" in new Setup(emptyUserAnswers) {
            when(mockAccountService.getRcaspDetails(any(), eqTo(rcaspId))(any(), any())).thenReturn(
              ResultT.fromValue(individualRcaspDetailsResponse.copy(TINDetails = None))
            )

            val request                = FakeRequest(GET, changeDetailsRoutingRoute)
            val result: Future[Result] = route(application, request).value

            status(result)                 mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

            verify(mockAccountService, times(1)).getRcaspDetails(any(), eqTo(rcaspId))(any(), any())
          }

          "when TINDetails contains an empty list" in new Setup(emptyUserAnswers) {
            when(mockAccountService.getRcaspDetails(any(), eqTo(rcaspId))(any(), any())).thenReturn(
              ResultT.fromValue(individualRcaspDetailsResponse.copy(TINDetails = Some(List.empty)))
            )

            val request                = FakeRequest(GET, changeDetailsRoutingRoute)
            val result: Future[Result] = route(application, request).value

            status(result)                 mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

            verify(mockAccountService, times(1)).getRcaspDetails(any(), eqTo(rcaspId))(any(), any())
          }

          "when PrimaryContactDetails is missing" in new Setup(emptyUserAnswers) {
            when(mockAccountService.getRcaspDetails(any(), eqTo(rcaspId))(any(), any())).thenReturn(
              ResultT.fromValue(individualRcaspDetailsResponse.copy(PrimaryContactDetails = None))
            )

            val request                = FakeRequest(GET, changeDetailsRoutingRoute)
            val result: Future[Result] = route(application, request).value

            status(result)                 mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

            verify(mockAccountService, times(1)).getRcaspDetails(any(), eqTo(rcaspId))(any(), any())
          }

          "when address cannot be converted to AddressUk" in new Setup(emptyUserAnswers) {
            when(mockAccountService.getRcaspDetails(any(), eqTo(rcaspId))(any(), any())).thenReturn(
              ResultT.fromValue(individualRcaspDetailsResponse.copy(AddressDetails = rcaspAddressEmptyOptionals))
            )

            val request                = FakeRequest(GET, changeDetailsRoutingRoute)
            val result: Future[Result] = route(application, request).value

            status(result)                 mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

            verify(mockAccountService, times(1)).getRcaspDetails(any(), eqTo(rcaspId))(any(), any())
          }
        }
      }

      "when an OrganisationRcaspDetails is returned and IsRCASPUser = false" - {
        "must populate user answers when all fields are present" in new Setup(emptyUserAnswers) {
          val rcaspDetails: OrganisationRcaspDetails = organisationRcaspDetailsResponse

          when(mockAccountService.getRcaspDetails(any(), eqTo(rcaspId))(any(), any()))
            .thenReturn(ResultT.fromValue(rcaspDetails))
          when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

          val request                = FakeRequest(GET, changeDetailsRoutingRoute)
          val result: Future[Result] = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.changeDetails.routes.ChangeDetailsController
            .onPageLoad(rcaspId)
            .url

          verify(mockAccountService, times(1)).getRcaspDetails(any(), eqTo(rcaspId))(any(), any())
          verify(mockSessionRepository, times(1)).set(
            argThat { ua =>
              !ua.rcaspIsRegisteredBusiness &&
              ua.get(ReportForRegisteredBusinessPage).contains(false) &&
              ua.get(OrganisationOrIndividualPage).contains(Organisation) &&
              ua.get(OrganisationNamePage).contains(testOrgName) &&
              ua.get(OverwritableOrganisationName).contains(testOrgName) &&
              ua.get(HaveTradingNamePage).contains(true) &&
              ua.get(TradingNamePage).contains(testTradingName) &&
              ua.get(UtrPage).contains(testUtr.uniqueTaxPayerReference) &&
              ua.get(UkAddressInUserAnswers).contains(testAddressUk) &&
              ua.get(OrganisationFirstContactNamePage).contains(testIndividualName.fullName) &&
              ua.get(OrganisationFirstContactEmailPage).contains(testEmail) &&
              ua.get(OrganisationFirstContactHavePhonePage).contains(true) &&
              ua.get(OrganisationFirstContactPhoneNumberPage).contains(testPhone) &&
              ua.get(OrganisationHaveSecondContactPage).contains(true) &&
              ua.get(OrganisationSecondContactNamePage).contains("Prof Turo") &&
              ua.get(OrganisationSecondContactEmailPage).contains(testEmail) &&
              ua.get(OrganisationSecondContactHavePhonePage).contains(true) &&
              ua.get(OrganisationSecondContactPhoneNumberPage).contains(testPhone) &&
              ua.get(ChangeRcaspCachedDetails).contains(rcaspDetails)
            }
          )
        }

        "must populate user answers when separate trading name, first contact phone and second contact details are absent" in new Setup(
          emptyUserAnswers
        ) {
          val rcaspDetails: OrganisationRcaspDetails =
            organisationRcaspDetailsResponse.copy(
              TradingName = testOrgName,
              PrimaryContactDetails = Some(rcaspContactDetails.copy(PhoneNumber = None)),
              SecondaryContactDetails = None
            )

          when(mockAccountService.getRcaspDetails(any(), eqTo(rcaspId))(any(), any()))
            .thenReturn(ResultT.fromValue(rcaspDetails))
          when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

          val request                = FakeRequest(GET, changeDetailsRoutingRoute)
          val result: Future[Result] = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.changeDetails.routes.ChangeDetailsController
            .onPageLoad(rcaspId)
            .url

          verify(mockAccountService, times(1)).getRcaspDetails(any(), eqTo(rcaspId))(any(), any())
          verify(mockSessionRepository, times(1)).set(
            argThat { ua =>
              !ua.rcaspIsRegisteredBusiness &&
              ua.get(ReportForRegisteredBusinessPage).contains(false) &&
              ua.get(OrganisationOrIndividualPage).contains(Organisation) &&
              ua.get(OrganisationNamePage).contains(testOrgName) &&
              ua.get(OverwritableOrganisationName).contains(testOrgName) &&
              ua.get(HaveTradingNamePage).contains(false) &&
              ua.get(TradingNamePage).isEmpty &&
              ua.get(UtrPage).contains(testUtr.uniqueTaxPayerReference) &&
              ua.get(UkAddressInUserAnswers).contains(testAddressUk) &&
              ua.get(OrganisationFirstContactNamePage).contains(testIndividualName.fullName) &&
              ua.get(OrganisationFirstContactEmailPage).contains(testEmail) &&
              ua.get(OrganisationFirstContactHavePhonePage).contains(false) &&
              ua.get(OrganisationFirstContactPhoneNumberPage).isEmpty &&
              ua.get(OrganisationHaveSecondContactPage).contains(false) &&
              ua.get(OrganisationSecondContactNamePage).isEmpty &&
              ua.get(OrganisationSecondContactEmailPage).isEmpty &&
              ua.get(OrganisationSecondContactHavePhonePage).isEmpty &&
              ua.get(OrganisationSecondContactPhoneNumberPage).isEmpty &&
              ua.get(ChangeRcaspCachedDetails).contains(rcaspDetails)
            }
          )
        }

        "must populate user answers when only second contact phone is absent" in new Setup(emptyUserAnswers) {
          val rcaspDetails: OrganisationRcaspDetails =
            organisationRcaspDetailsResponse.copy(
              SecondaryContactDetails = Some(rcaspContactDetails.copy(ContactName = "Prof Turo", PhoneNumber = None))
            )

          when(mockAccountService.getRcaspDetails(any(), eqTo(rcaspId))(any(), any()))
            .thenReturn(ResultT.fromValue(rcaspDetails))
          when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

          val request                = FakeRequest(GET, changeDetailsRoutingRoute)
          val result: Future[Result] = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.changeDetails.routes.ChangeDetailsController
            .onPageLoad(rcaspId)
            .url

          verify(mockAccountService, times(1)).getRcaspDetails(any(), eqTo(rcaspId))(any(), any())
          verify(mockSessionRepository, times(1)).set(
            argThat { ua =>
              !ua.rcaspIsRegisteredBusiness &&
              ua.get(ReportForRegisteredBusinessPage).contains(false) &&
              ua.get(OrganisationOrIndividualPage).contains(Organisation) &&
              ua.get(OrganisationNamePage).contains(testOrgName) &&
              ua.get(OverwritableOrganisationName).contains(testOrgName) &&
              ua.get(HaveTradingNamePage).contains(true) &&
              ua.get(TradingNamePage).contains(testTradingName) &&
              ua.get(UtrPage).contains(testUtr.uniqueTaxPayerReference) &&
              ua.get(UkAddressInUserAnswers).contains(testAddressUk) &&
              ua.get(OrganisationFirstContactNamePage).contains(testIndividualName.fullName) &&
              ua.get(OrganisationFirstContactEmailPage).contains(testEmail) &&
              ua.get(OrganisationFirstContactHavePhonePage).contains(true) &&
              ua.get(OrganisationFirstContactPhoneNumberPage).contains(testPhone) &&
              ua.get(OrganisationHaveSecondContactPage).contains(true) &&
              ua.get(OrganisationSecondContactNamePage).contains("Prof Turo") &&
              ua.get(OrganisationSecondContactEmailPage).contains(testEmail) &&
              ua.get(OrganisationSecondContactHavePhonePage).contains(false) &&
              ua.get(OrganisationSecondContactPhoneNumberPage).isEmpty &&
              ua.get(ChangeRcaspCachedDetails).contains(rcaspDetails)
            }
          )
        }

        "must redirect to journey recovery" - {
          "when TINDetails is missing" in new Setup(emptyUserAnswers) {
            when(mockAccountService.getRcaspDetails(any(), eqTo(rcaspId))(any(), any())).thenReturn(
              ResultT.fromValue(organisationRcaspDetailsResponse.copy(TINDetails = None))
            )

            val request                = FakeRequest(GET, changeDetailsRoutingRoute)
            val result: Future[Result] = route(application, request).value

            status(result)                 mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

            verify(mockAccountService, times(1)).getRcaspDetails(any(), eqTo(rcaspId))(any(), any())
          }

          "when TINDetails contains an empty list" in new Setup(emptyUserAnswers) {
            when(mockAccountService.getRcaspDetails(any(), eqTo(rcaspId))(any(), any())).thenReturn(
              ResultT.fromValue(organisationRcaspDetailsResponse.copy(TINDetails = Some(List.empty)))
            )

            val request                = FakeRequest(GET, changeDetailsRoutingRoute)
            val result: Future[Result] = route(application, request).value

            status(result)                 mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

            verify(mockAccountService, times(1)).getRcaspDetails(any(), eqTo(rcaspId))(any(), any())
          }

          "when PrimaryContactDetails is missing" in new Setup(emptyUserAnswers) {
            when(mockAccountService.getRcaspDetails(any(), eqTo(rcaspId))(any(), any())).thenReturn(
              ResultT.fromValue(organisationRcaspDetailsResponse.copy(PrimaryContactDetails = None))
            )

            val request                = FakeRequest(GET, changeDetailsRoutingRoute)
            val result: Future[Result] = route(application, request).value

            status(result)                 mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

            verify(mockAccountService, times(1)).getRcaspDetails(any(), eqTo(rcaspId))(any(), any())
          }

          "when address cannot be converted to AddressUk" in new Setup(emptyUserAnswers) {
            when(mockAccountService.getRcaspDetails(any(), eqTo(rcaspId))(any(), any())).thenReturn(
              ResultT.fromValue(organisationRcaspDetailsResponse.copy(AddressDetails = rcaspAddressEmptyOptionals))
            )

            val request                = FakeRequest(GET, changeDetailsRoutingRoute)
            val result: Future[Result] = route(application, request).value

            status(result)                 mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

            verify(mockAccountService, times(1)).getRcaspDetails(any(), eqTo(rcaspId))(any(), any())
          }
        }
      }

      "when an OrganisationRcaspDetails is returned and IsRCASPUser = true (registered business)" - {
        "must populate user answers when trading name is different from RCASP name" in new Setup(emptyUserAnswers) {
          val rcaspDetails: OrganisationRcaspDetails =
            organisationRcaspDetailsResponse.copy(
              IsRCASPUser = true,
              PrimaryContactDetails = None,
              SecondaryContactDetails = None
            )

          when(mockAccountService.getRcaspDetails(any(), eqTo(rcaspId))(any(), any()))
            .thenReturn(ResultT.fromValue(rcaspDetails))
          when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

          val request                = FakeRequest(GET, changeDetailsRoutingRoute)
          val result: Future[Result] = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.PlaceholderController
            .onPageLoad(s"Should nav to registered-business/change-answers/$rcaspId (CARF-350)")
            .url

          verify(mockAccountService, times(1)).getRcaspDetails(any(), eqTo(rcaspId))(any(), any())
          verify(mockSessionRepository, times(1)).set(
            argThat { ua =>
              ua.rcaspIsRegisteredBusiness &&
              ua.get(ReportForRegisteredBusinessPage).contains(true) &&
              ua.get(OrganisationNamePage).contains(testOrgName) &&
              ua.get(OverwritableOrganisationName).contains(testOrgName) &&
              ua.get(HaveTradingNamePage).contains(true) &&
              ua.get(TradingNamePage).contains(testTradingName) &&
              ua.get(UtrPage).contains(testUtr.uniqueTaxPayerReference) &&
              ua.get(RegisteredBusinessIsTheAddressCorrectPage).contains(true) &&
              ua.get(CachedBusinessDetailsPage).contains(cachedBusinessDetails) &&
              ua.get(ChangeRcaspCachedDetails).contains(rcaspDetails)
            }
          )
        }

        "must populate user answers when trading name is the same as RCASP name" in new Setup(emptyUserAnswers) {
          val rcaspDetails: OrganisationRcaspDetails =
            organisationRcaspDetailsResponse.copy(
              IsRCASPUser = true,
              TradingName = testOrgName,
              PrimaryContactDetails = None,
              SecondaryContactDetails = None
            )

          when(mockAccountService.getRcaspDetails(any(), eqTo(rcaspId))(any(), any()))
            .thenReturn(ResultT.fromValue(rcaspDetails))
          when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

          val request                = FakeRequest(GET, changeDetailsRoutingRoute)
          val result: Future[Result] = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.PlaceholderController
            .onPageLoad(s"Should nav to registered-business/change-answers/$rcaspId (CARF-350)")
            .url

          verify(mockAccountService, times(1)).getRcaspDetails(any(), eqTo(rcaspId))(any(), any())
          verify(mockSessionRepository, times(1)).set(
            argThat { ua =>
              ua.rcaspIsRegisteredBusiness &&
              ua.get(ReportForRegisteredBusinessPage).contains(true) &&
              ua.get(OrganisationNamePage).contains(testOrgName) &&
              ua.get(OverwritableOrganisationName).contains(testOrgName) &&
              ua.get(HaveTradingNamePage).contains(false) &&
              ua.get(TradingNamePage).isEmpty &&
              ua.get(UtrPage).contains(testUtr.uniqueTaxPayerReference) &&
              ua.get(RegisteredBusinessIsTheAddressCorrectPage).contains(true) &&
              ua.get(CachedBusinessDetailsPage).contains(cachedBusinessDetails) &&
              ua.get(ChangeRcaspCachedDetails).contains(rcaspDetails)
            }
          )
        }

        "must redirect to journey recovery" - {
          "when TINDetails is missing" in new Setup(emptyUserAnswers) {
            when(mockAccountService.getRcaspDetails(any(), eqTo(rcaspId))(any(), any())).thenReturn(
              ResultT.fromValue(
                organisationRcaspDetailsResponse.copy(
                  IsRCASPUser = true,
                  PrimaryContactDetails = None,
                  SecondaryContactDetails = None,
                  TINDetails = None
                )
              )
            )

            val request                = FakeRequest(GET, changeDetailsRoutingRoute)
            val result: Future[Result] = route(application, request).value

            status(result)                 mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

            verify(mockAccountService, times(1)).getRcaspDetails(any(), eqTo(rcaspId))(any(), any())
          }

          "when TINDetails contains an empty list" in new Setup(emptyUserAnswers) {
            when(mockAccountService.getRcaspDetails(any(), eqTo(rcaspId))(any(), any())).thenReturn(
              ResultT.fromValue(
                organisationRcaspDetailsResponse.copy(
                  IsRCASPUser = true,
                  PrimaryContactDetails = None,
                  SecondaryContactDetails = None,
                  TINDetails = Some(List.empty)
                )
              )
            )

            val request                = FakeRequest(GET, changeDetailsRoutingRoute)
            val result: Future[Result] = route(application, request).value

            status(result)                 mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

            verify(mockAccountService, times(1)).getRcaspDetails(any(), eqTo(rcaspId))(any(), any())
          }
        }
      }
    }
  }

  class Setup(userAnswers: UserAnswers) {
    val mockAccountService: AccountService = mock[AccountService]

    val application: Application =
      applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[AccountService].toInstance(mockAccountService))
        .build()
  }
}
