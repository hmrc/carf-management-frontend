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

package controllers.combined

import base.SpecBase
import forms.GenericYesNoPageFormProvider
import models.individual.IndividualName
import models.{NormalMode, OrganisationOrIndividual}
import org.mockito.ArgumentMatchers.{any, argThat}
import org.mockito.Mockito.{verify, when}
import pages.changeDetails.ChangeRcaspCachedDetails
import pages.combined.OrganisationOrIndividualPage
import pages.individual.*
import pages.organisation.*
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.AccountService
import types.ResultT
import views.html.combined.ChangeReportForRegisteredBusinessView

import scala.concurrent.Future

class ChangeReportForRegisteredBusinessControllerSpec extends SpecBase {

  val formProvider        = new GenericYesNoPageFormProvider()
  val form: Form[Boolean] = formProvider("reportForRegisteredBusiness.changeDetails.error.required")

  val mockAccountService: AccountService = mock[AccountService]

  lazy val onPageLoadRoute: String =
    controllers.combined.routes.ChangeReportForRegisteredBusinessController.onPageLoad.url
  lazy val onSubmitRoute: String   = controllers.combined.routes.ChangeReportForRegisteredBusinessController.onSubmit.url

  "ChangeReportForRegisteredBusiness Controller" - {

    "onPageLoad" - {

      "must return OK and the correct view for a GET when the question has previously been answered" in {
        val userAnswers = emptyUserAnswers.withPage(ReportForRegisteredBusinessPage, true)

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, onPageLoadRoute)
          val result  = route(application, request).value
          val view    = application.injector.instanceOf[ChangeReportForRegisteredBusinessView]

          status(result)          mustEqual OK
          contentAsString(result) mustEqual view(form.fill(true))(request, messages(application)).toString
        }
      }

      "must redirect to Journey Recovery on GET when ReportForRegisteredBusinessPage is empty" in {
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, onPageLoadRoute)
          val result  = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    "onSubmit" - {

      "when the answer is changed from false -> true, has a CT UTR, and has 1 RCASP added" - {
        "must redirect to RegisteredBusinessIsThisYourBusinessNameController and set rcaspIsRegisteredBusiness to true and clear pages" in {
          val userAnswers = emptyUserAnswers
            .withPage(ReportForRegisteredBusinessPage, false)
            .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate)
            .withPage(OrganisationOrIndividualPage, OrganisationOrIndividual.Organisation)
            .withPage(UtrPage, testUtr.uniqueTaxPayerReference)
            .withPage(OrganisationFirstContactNamePage, testOrgName)
            .withPage(OrganisationFirstContactEmailPage, testEmail)
            .withPage(OrganisationFirstContactHavePhonePage, true)
            .withPage(OrganisationFirstContactPhoneNumberPage, testPhone)
            .withPage(OrganisationHaveSecondContactPage, true)
            .withPage(OrganisationSecondContactNamePage, "Second Contact Name")
            .withPage(OrganisationSecondContactEmailPage, testEmail)
            .withPage(OrganisationSecondContactHavePhonePage, true)
            .withPage(OrganisationSecondContactPhoneNumberPage, testPhone)
            .withPage(IndividualNamePage, IndividualName(testName, "lastName"))
            .withPage(NiNumberPage, testNiNumber)
            .withPage(IndividualEmailPage, testEmail)
            .withPage(IndividualHavePhonePage, true)
            .withPage(IndividualPhonePage, testPhone)

          when(mockAccountService.getNumberOfRcaspsCurrentlyAdded(any())(any(), any())).thenReturn(ResultT.fromValue(1))
          when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

          val application =
            applicationBuilder(
              userAnswers = Some(userAnswers),
              requestUtr = Some(testUtr.uniqueTaxPayerReference)
            ).overrides(
              bind[AccountService].toInstance(mockAccountService)
            ).build()

          running(application) {
            val request = FakeRequest(POST, onSubmitRoute).withFormUrlEncodedBody(("value", "true"))
            val result  = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(
              result
            ).value        mustEqual controllers.organisation.routes.RegisteredBusinessIsThisYourBusinessNameController
              .onPageLoad(NormalMode)
              .url

            verify(mockSessionRepository).set(argThat { ua =>
              ua.get(ReportForRegisteredBusinessPage).contains(true) &&
              ua.rcaspIsRegisteredBusiness &&
              ua.get(IndividualNamePage).isEmpty &&
              ua.get(NiNumberPage).isEmpty &&
              ua.get(IndividualEmailPage).isEmpty &&
              ua.get(IndividualHavePhonePage).isEmpty &&
              ua.get(IndividualPhonePage).isEmpty &&
              ua.get(OrganisationFirstContactNamePage).isEmpty &&
              ua.get(OrganisationFirstContactEmailPage).isEmpty &&
              ua.get(OrganisationFirstContactHavePhonePage).isEmpty &&
              ua.get(OrganisationFirstContactPhoneNumberPage).isEmpty &&
              ua.get(OrganisationSecondContactNamePage).isEmpty &&
              ua.get(OrganisationSecondContactEmailPage).isEmpty &&
              ua.get(OrganisationSecondContactHavePhonePage).isEmpty &&
              ua.get(OrganisationSecondContactPhoneNumberPage).isEmpty &&
              ua.get(OrganisationOrIndividualPage).isEmpty &&
              ua.get(UtrPage).isEmpty
            })
          }
        }
      }

      "when the answer is changed from false -> true, has a CT UTR, and has 2 RCASP added" - {
        "must redirect to RegisteredBusinessIsThisYourBusinessNameController and set rcaspIsRegisteredBusiness to false and clear pages" in {
          val userAnswers = emptyUserAnswers
            .withPage(ReportForRegisteredBusinessPage, false)
            .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate)
            .withPage(OrganisationOrIndividualPage, OrganisationOrIndividual.Organisation)
            .withPage(UtrPage, testUtr.uniqueTaxPayerReference)
            .withPage(OrganisationFirstContactNamePage, testOrgName)
            .withPage(OrganisationFirstContactEmailPage, testEmail)
            .withPage(OrganisationFirstContactHavePhonePage, true)
            .withPage(OrganisationFirstContactPhoneNumberPage, testPhone)
            .withPage(OrganisationHaveSecondContactPage, true)
            .withPage(OrganisationSecondContactNamePage, "Second Contact Name")
            .withPage(OrganisationSecondContactEmailPage, testEmail)
            .withPage(OrganisationSecondContactHavePhonePage, true)
            .withPage(OrganisationSecondContactPhoneNumberPage, testPhone)
            .withPage(IndividualNamePage, IndividualName(testName, "lastName"))
            .withPage(NiNumberPage, testNiNumber)
            .withPage(IndividualEmailPage, testEmail)
            .withPage(IndividualHavePhonePage, true)
            .withPage(IndividualPhonePage, testPhone)

          when(mockAccountService.getNumberOfRcaspsCurrentlyAdded(any())(any(), any())).thenReturn(ResultT.fromValue(2))
          when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

          val application =
            applicationBuilder(
              userAnswers = Some(userAnswers),
              requestUtr = Some(testUtr.uniqueTaxPayerReference)
            ).overrides(
              bind[AccountService].toInstance(mockAccountService)
            ).build()

          running(application) {
            val request = FakeRequest(POST, onSubmitRoute).withFormUrlEncodedBody(("value", "true"))
            val result  = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(
              result
            ).value        mustEqual controllers.organisation.routes.RegisteredBusinessIsThisYourBusinessNameController
              .onPageLoad(NormalMode)
              .url

            verify(mockSessionRepository).set(argThat { ua =>
              ua.get(ReportForRegisteredBusinessPage).contains(true) &&
              !ua.rcaspIsRegisteredBusiness &&
              ua.get(IndividualNamePage).isEmpty &&
              ua.get(NiNumberPage).isEmpty &&
              ua.get(IndividualEmailPage).isEmpty &&
              ua.get(IndividualHavePhonePage).isEmpty &&
              ua.get(IndividualPhonePage).isEmpty &&
              ua.get(OrganisationFirstContactNamePage).isEmpty &&
              ua.get(OrganisationFirstContactEmailPage).isEmpty &&
              ua.get(OrganisationFirstContactHavePhonePage).isEmpty &&
              ua.get(OrganisationFirstContactPhoneNumberPage).isEmpty &&
              ua.get(OrganisationSecondContactNamePage).isEmpty &&
              ua.get(OrganisationSecondContactEmailPage).isEmpty &&
              ua.get(OrganisationSecondContactHavePhonePage).isEmpty &&
              ua.get(OrganisationSecondContactPhoneNumberPage).isEmpty &&
              ua.get(OrganisationOrIndividualPage).isEmpty &&
              ua.get(UtrPage).isEmpty
            })
          }
        }
      }

      "when the answer is changed from true -> false" - {
        "must redirect to OrganisationOrIndividualController and set rcaspIsRegisteredBusiness to false and clear pages" in {
          val userAnswers = emptyUserAnswers
            .withPage(ReportForRegisteredBusinessPage, true)
            .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate)
            .withPage(RegisteredBusinessIsThisYourBusinessNamePage, true)
            .withPage(RegisteredBusinessIsTheAddressCorrectPage, true)

          when(mockAccountService.getNumberOfRcaspsCurrentlyAdded(any())(any(), any())).thenReturn(ResultT.fromValue(1))
          when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

          val application =
            applicationBuilder(userAnswers = Some(userAnswers))
              .overrides(bind[AccountService].toInstance(mockAccountService))
              .build()

          running(application) {
            val request = FakeRequest(POST, onSubmitRoute).withFormUrlEncodedBody(("value", "false"))
            val result  = route(application, request).value

            status(result)                 mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.combined.routes.OrganisationOrIndividualController
              .onPageLoad(NormalMode)
              .url

            verify(mockSessionRepository).set(argThat { ua =>
              ua.get(ReportForRegisteredBusinessPage).contains(false) && !ua.rcaspIsRegisteredBusiness &&
              ua.get(RegisteredBusinessIsThisYourBusinessNamePage).isEmpty &&
              ua.get(RegisteredBusinessIsTheAddressCorrectPage).isEmpty
            })
          }
        }

        "must not clear pages if ChangeRcaspCachedDetails is not defined" in {
          val userAnswers = emptyUserAnswers
            .withPage(ReportForRegisteredBusinessPage, true)
            .withPage(RegisteredBusinessIsThisYourBusinessNamePage, true)
            .withPage(RegisteredBusinessIsTheAddressCorrectPage, true)

          when(mockAccountService.getNumberOfRcaspsCurrentlyAdded(any())(any(), any())).thenReturn(ResultT.fromValue(1))
          when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

          val application =
            applicationBuilder(userAnswers = Some(userAnswers))
              .overrides(bind[AccountService].toInstance(mockAccountService))
              .build()

          running(application) {
            val request = FakeRequest(POST, onSubmitRoute).withFormUrlEncodedBody(("value", "false"))
            val result  = route(application, request).value

            status(result) mustEqual SEE_OTHER

            verify(mockSessionRepository).set(argThat { ua =>
              ua.get(ReportForRegisteredBusinessPage).contains(false) && !ua.rcaspIsRegisteredBusiness &&
              ua.get(RegisteredBusinessIsThisYourBusinessNamePage).isDefined &&
              ua.get(RegisteredBusinessIsTheAddressCorrectPage).isDefined
            })
          }
        }
      }
      "when the answer remains unchanged as true" - {
        "must redirect to RegisteredBusinessChangeDetailsController without updating SessionRepository" in {
          val userAnswers = emptyUserAnswers
            .withPage(ReportForRegisteredBusinessPage, true)
            .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate)

          val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

          running(application) {
            val request = FakeRequest(POST, onSubmitRoute).withFormUrlEncodedBody(("value", "true"))
            val result  = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(
              result
            ).value        mustEqual controllers.changeDetails.routes.RegisteredBusinessChangeDetailsController
              .onPageLoad(rcaspId)
              .url
          }
        }
      }

      "when the answer remains unchanged as false" - {
        "must redirect to ChangeDetailsController without updating SessionRepository" in {
          val userAnswers = emptyUserAnswers
            .withPage(ReportForRegisteredBusinessPage, false)
            .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate)

          val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

          running(application) {
            val request = FakeRequest(POST, onSubmitRoute).withFormUrlEncodedBody(("value", "false"))
            val result  = route(application, request).value

            status(result)                 mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.changeDetails.routes.ChangeDetailsController
              .onPageLoad(organisationRcaspDetailsViewUpdate.RCASPID)
              .url
          }
        }
      }

      "must redirect to Journey Recovery if ChangeRcaspCachedDetails is missing" in {
        val userAnswers = emptyUserAnswers.withPage(ReportForRegisteredBusinessPage, false)

        when(mockAccountService.getNumberOfRcaspsCurrentlyAdded(any())(any(), any())).thenReturn(ResultT.fromValue(1))
        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        val application =
          applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(bind[AccountService].toInstance(mockAccountService))
            .build()

        running(application) {
          val request = FakeRequest(POST, onSubmitRoute).withFormUrlEncodedBody(("value", "true"))
          val result  = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must return a Bad Request and errors when invalid data is submitted" in {
        val userAnswers = emptyUserAnswers.withPage(ReportForRegisteredBusinessPage, true)

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(POST, onSubmitRoute).withFormUrlEncodedBody(("value", ""))
          val result  = route(application, request).value

          status(result) mustEqual BAD_REQUEST
        }
      }
    }
  }
}
