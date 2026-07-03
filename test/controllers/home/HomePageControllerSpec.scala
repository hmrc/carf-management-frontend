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

package controllers.home

import base.SpecBase
import config.FrontendAppConfig
import models.UserAnswers
import models.errors.ApiError.InternalServerError
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when}
import play.api.Application
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.{AccountService, AddressLookupService, UploadInformationService}
import types.ResultT
import viewmodels.HomePageViewModel
import views.html.HomePageView

import java.time.Clock
import scala.concurrent.Future

class HomePageControllerSpec extends SpecBase {

  val basicViewModel: HomePageViewModel = HomePageViewModel(
    isBusiness = true,
    hasZeroRcaspsAdded = true,
    hasSentFilesInLast28Days = true,
    organisationName = Some("Timmy Ltd"),
    ctUtr = Some(testUtr),
    carfId = testCarfId
  )

  val negativeViewModel: HomePageViewModel = HomePageViewModel(
    isBusiness = false,
    hasZeroRcaspsAdded = false,
    hasSentFilesInLast28Days = false,
    organisationName = None,
    ctUtr = None,
    carfId = testCarfId
  )

  "HomePageController" - {
    "must return OK, instantiate user answers, and return the correct view for a GET when all service calls are successful with a ct utr" in new Setup(
      requestUtr = Some(testUtr.uniqueTaxPayerReference)
    ) {
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      when(mockAppConfig.aeoiEmailAddress) thenReturn "aeoi.enquiries@hmrc.gov.uk"
      when(mockAppConfig.changeContactDetailsIndexUrl) thenReturn "bbb"
      when(mockAppConfig.feedbackUrl(any())) thenReturn "ccc"

      when(mockUploadInformationService.hasUserUploadedFilesInLast28Days(any())).thenReturn(ResultT.fromValue(true))

      when(mockAccountService.getNumberOfRcaspsCurrentlyAdded(any())(any(), any())).thenReturn(ResultT.fromValue(0))
      when(mockAccountService.hasOrganisationContactDetails(any())).thenReturn(ResultT.fromValue(true))
      when(mockAccountService.getOrganisationName(any())).thenReturn(ResultT.fromValue(Some("Timmy Ltd")))

      running(application) {
        val request = FakeRequest(GET, routes.HomePageController.onPageLoad().url)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[HomePageView]

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(basicViewModel, "aeoi.enquiries@hmrc.gov.uk", "bbb")(
          request,
          messages(application)
        ).toString
      }
    }

    "must return OK and the correct view for a GET when all service calls are successful with a ct utr and organisationName is None" in new Setup(
      requestUtr = Some(testUtr.uniqueTaxPayerReference)
    ) {
      when(mockAppConfig.aeoiEmailAddress) thenReturn "aeoi.enquiries@hmrc.gov.uk"
      when(mockAppConfig.changeContactDetailsIndexUrl) thenReturn "bbb"
      when(mockAppConfig.feedbackUrl(any())) thenReturn "ccc"

      when(mockUploadInformationService.hasUserUploadedFilesInLast28Days(any())).thenReturn(ResultT.fromValue(true))

      when(mockAccountService.getNumberOfRcaspsCurrentlyAdded(any())(any(), any())).thenReturn(ResultT.fromValue(0))
      when(mockAccountService.hasOrganisationContactDetails(any())).thenReturn(ResultT.fromValue(true))
      when(mockAccountService.getOrganisationName(any())).thenReturn(ResultT.fromValue(None))

      val expectedViewModel: HomePageViewModel = basicViewModel.copy(organisationName = None)

      running(application) {
        val request = FakeRequest(GET, routes.HomePageController.onPageLoad().url)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[HomePageView]

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(expectedViewModel, "aeoi.enquiries@hmrc.gov.uk", "bbb")(
          request,
          messages(application)
        ).toString
      }
    }

    "must return OK and the correct view for a GET when all service calls are successful for an individual with multiple rcasps" in new Setup(
      requestUtr = None
    ) {
      when(mockAppConfig.aeoiEmailAddress) thenReturn "aeoi.enquiries@hmrc.gov.uk"
      when(mockAppConfig.changeContactDetailsIndexUrl) thenReturn "bbb"
      when(mockAppConfig.feedbackUrl(any())) thenReturn "ccc"

      when(mockUploadInformationService.hasUserUploadedFilesInLast28Days(any())).thenReturn(ResultT.fromValue(false))

      when(mockAccountService.getNumberOfRcaspsCurrentlyAdded(any())(any(), any())).thenReturn(ResultT.fromValue(2))
      when(mockAccountService.hasOrganisationContactDetails(any())).thenReturn(ResultT.fromValue(false))

      running(application) {
        val request = FakeRequest(GET, routes.HomePageController.onPageLoad().url)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[HomePageView]

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(negativeViewModel, "aeoi.enquiries@hmrc.gov.uk", "bbb")(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to journey recovery when the call to getNumberOfRcaspsCurrentlyAdded fails" in new Setup(
      requestUtr = Some(testUtr.uniqueTaxPayerReference)
    ) {
      when(mockAppConfig.aeoiEmailAddress) thenReturn "aeoi.enquiries@hmrc.gov.uk"
      when(mockAppConfig.changeContactDetailsIndexUrl) thenReturn "bbb"
      when(mockAppConfig.feedbackUrl(any())) thenReturn "ccc"

      when(mockAccountService.getNumberOfRcaspsCurrentlyAdded(any())(any(), any()))
        .thenReturn(ResultT.fromError(InternalServerError))
      when(mockAccountService.hasOrganisationContactDetails(any())).thenReturn(ResultT.fromValue(true))
      when(mockAccountService.getOrganisationName(any())).thenReturn(ResultT.fromValue(Some("Timmy Ltd")))

      when(mockUploadInformationService.hasUserUploadedFilesInLast28Days(any())).thenReturn(ResultT.fromValue(true))

      running(application) {
        val request = FakeRequest(GET, routes.HomePageController.onPageLoad().url)
        val result  = route(application, request).value

        status(result)              mustEqual SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
        verify(mockAccountService).getNumberOfRcaspsCurrentlyAdded(any())(any(), any())
        verify(mockAccountService, times(0)).hasOrganisationContactDetails(any())
        verify(mockAccountService, times(0)).getOrganisationName(any())
        verify(mockUploadInformationService, times(0)).hasUserUploadedFilesInLast28Days(any())
      }
    }

    "must redirect to journey recovery when the call to hasOrganisationContactDetails fails" in new Setup(
      requestUtr = Some(testUtr.uniqueTaxPayerReference)
    ) {
      when(mockAppConfig.aeoiEmailAddress) thenReturn "aeoi.enquiries@hmrc.gov.uk"
      when(mockAppConfig.changeContactDetailsIndexUrl) thenReturn "bbb"
      when(mockAppConfig.feedbackUrl(any())) thenReturn "ccc"

      when(mockAccountService.getNumberOfRcaspsCurrentlyAdded(any())(any(), any())).thenReturn(ResultT.fromValue(0))
      when(mockAccountService.hasOrganisationContactDetails(any())).thenReturn(ResultT.fromError(InternalServerError))
      when(mockAccountService.getOrganisationName(any())).thenReturn(ResultT.fromValue(Some("Timmy Ltd")))

      when(mockUploadInformationService.hasUserUploadedFilesInLast28Days(any())).thenReturn(ResultT.fromValue(true))

      running(application) {
        val request = FakeRequest(GET, routes.HomePageController.onPageLoad().url)
        val result  = route(application, request).value

        status(result)              mustEqual SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
        verify(mockAccountService).getNumberOfRcaspsCurrentlyAdded(any())(any(), any())
        verify(mockAccountService).hasOrganisationContactDetails(any())
        verify(mockAccountService, times(0)).getOrganisationName(any())
        verify(mockUploadInformationService, times(0)).hasUserUploadedFilesInLast28Days(any())
      }
    }

    "must redirect to journey recovery when the call to getOrganisationName fails" in new Setup(
      requestUtr = Some(testUtr.uniqueTaxPayerReference)
    ) {
      when(mockAppConfig.aeoiEmailAddress) thenReturn "aeoi.enquiries@hmrc.gov.uk"
      when(mockAppConfig.changeContactDetailsIndexUrl) thenReturn "bbb"
      when(mockAppConfig.feedbackUrl(any())) thenReturn "ccc"

      when(mockAccountService.getNumberOfRcaspsCurrentlyAdded(any())(any(), any())).thenReturn(ResultT.fromValue(0))
      when(mockAccountService.hasOrganisationContactDetails(any())).thenReturn(ResultT.fromValue(true))
      when(mockAccountService.getOrganisationName(any())).thenReturn(ResultT.fromError(InternalServerError))

      when(mockUploadInformationService.hasUserUploadedFilesInLast28Days(any())).thenReturn(ResultT.fromValue(true))

      running(application) {
        val request = FakeRequest(GET, routes.HomePageController.onPageLoad().url)
        val result  = route(application, request).value

        status(result)              mustEqual SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
        verify(mockAccountService).getNumberOfRcaspsCurrentlyAdded(any())(any(), any())
        verify(mockAccountService).hasOrganisationContactDetails(any())
        verify(mockAccountService).getOrganisationName(any())
        verify(mockUploadInformationService, times(0)).hasUserUploadedFilesInLast28Days(any())
      }
    }

    "must redirect to journey recovery when the call to hasUserUploadedFilesInLast28Days fails" in new Setup(
      requestUtr = Some(testUtr.uniqueTaxPayerReference)
    ) {
      when(mockAppConfig.aeoiEmailAddress) thenReturn "aeoi.enquiries@hmrc.gov.uk"
      when(mockAppConfig.changeContactDetailsIndexUrl) thenReturn "bbb"
      when(mockAppConfig.feedbackUrl(any())) thenReturn "ccc"

      when(mockAccountService.getNumberOfRcaspsCurrentlyAdded(any())(any(), any())).thenReturn(ResultT.fromValue(0))
      when(mockAccountService.hasOrganisationContactDetails(any())).thenReturn(ResultT.fromValue(true))
      when(mockAccountService.getOrganisationName(any())).thenReturn(ResultT.fromValue(Some("Timmy Ltd")))

      when(mockUploadInformationService.hasUserUploadedFilesInLast28Days(any()))
        .thenReturn(ResultT.fromError(InternalServerError))

      running(application) {
        val request = FakeRequest(GET, routes.HomePageController.onPageLoad().url)
        val result  = route(application, request).value

        status(result)              mustEqual SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
        verify(mockAccountService).getNumberOfRcaspsCurrentlyAdded(any())(any(), any())
        verify(mockAccountService).hasOrganisationContactDetails(any())
        verify(mockAccountService).getOrganisationName(any())
        verify(mockUploadInformationService).hasUserUploadedFilesInLast28Days(any())
      }
    }
  }

  class Setup(
      requestUtr: Option[String] = None,
      userAnswers: Option[UserAnswers] = None
  ) {
    val mockAccountService: AccountService                     = mock[AccountService]
    val mockUploadInformationService: UploadInformationService = mock[UploadInformationService]
    val mockAppConfig: FrontendAppConfig                       = mock[FrontendAppConfig]
    val mockAddressLookupService: AddressLookupService         = mock[AddressLookupService]

    val application: Application =
      applicationBuilder(requestUtr = requestUtr, userAnswers = userAnswers)
        .overrides(
          bind[AccountService].toInstance(mockAccountService),
          bind[UploadInformationService].toInstance(mockUploadInformationService),
          bind[FrontendAppConfig].toInstance(mockAppConfig),
          bind[AddressLookupService].toInstance(mockAddressLookupService),
          bind[Clock].toInstance(clock)
        )
        .build()
  }
}
