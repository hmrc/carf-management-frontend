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
import forms.GenericYesNoPageFormProvider
import models.responses.AddressRegistrationResponse
import models.{BusinessDetails, NormalMode}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import pages.organisation.{CachedBusinessDetailsPage, ReportForRegisteredBusinessPage}
import play.api.data.Form
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.RegistrationService
import views.html.organisation.ReportForRegisteredBusinessView

import scala.concurrent.Future

class ReportForRegisteredBusinessControllerSpec extends SpecBase {

  def onwardRoute = Call("GET", "/foo")

  val formProvider        = new GenericYesNoPageFormProvider()
  val form: Form[Boolean] = formProvider("reportForRegisteredBusiness.error.required")

  val mockRegistrationService: RegistrationService = mock[RegistrationService]

  lazy val routeUnderTest: String =
    controllers.organisation.routes.ReportForRegisteredBusinessController.onPageLoad(NormalMode).url

  val businessDetails: BusinessDetails =
    BusinessDetails(
      name = "Test Business Ltd",
      address = AddressRegistrationResponse(
        addressLine1 = "1 Test Street",
        addressLine2 = Some("Testville"),
        addressLine3 = None,
        addressLine4 = None,
        postalCode = Some("TE1 1ST"),
        countryCode = "GB"
      )
    )

  "ReportForRegisteredBusiness Controller" - {

    "must return OK and the correct view for a GET when a UTR is present" in {
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val userAnswers = emptyUserAnswers

      when(mockRegistrationService.getBusinessWithUtr(eqTo(testUtr.uniqueTaxPayerReference)))
        .thenReturn(Future.successful(businessDetails))

      val application =
        applicationBuilder(
          userAnswers = Some(userAnswers),
          requestUtr = Some(testUtr.uniqueTaxPayerReference)
        )
          .overrides(bind[RegistrationService].toInstance(mockRegistrationService))
          .build()

      running(application) {
        val request = FakeRequest(GET, routeUnderTest)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[ReportForRegisteredBusinessView]

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, businessDetails.name)(
          request,
          messages(application)
        ).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      val userAnswers =
        emptyUserAnswers
          .withPage(ReportForRegisteredBusinessPage, true)

      when(mockRegistrationService.getBusinessWithUtr(eqTo(testUtr.uniqueTaxPayerReference)))
        .thenReturn(Future.successful(businessDetails))

      val application =
        applicationBuilder(
          userAnswers = Some(userAnswers),
          requestUtr = Some(testUtr.uniqueTaxPayerReference)
        )
          .overrides(bind[RegistrationService].toInstance(mockRegistrationService))
          .build()

      running(application) {
        val request = FakeRequest(GET, routeUnderTest)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[ReportForRegisteredBusinessView]

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(form.fill(true), NormalMode, businessDetails.name)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val userAnswers =
        emptyUserAnswers
          .withPage(CachedBusinessDetailsPage, businessDetails)

      val application =
        applicationBuilder(
          userAnswers = Some(userAnswers),
          requestUtr = Some(testUtr.uniqueTaxPayerReference)
        )
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[RegistrationService].toInstance(mockRegistrationService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, routeUnderTest)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      val userAnswers =
        emptyUserAnswers
          .withPage(CachedBusinessDetailsPage, businessDetails)

      val application =
        applicationBuilder(
          userAnswers = Some(userAnswers),
          requestUtr = Some(testUtr.uniqueTaxPayerReference)
        )
          .overrides(bind[RegistrationService].toInstance(mockRegistrationService))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, routeUnderTest)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))
        val view      = application.injector.instanceOf[ReportForRegisteredBusinessView]

        val result = route(application, request).value

        status(result)          mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, businessDetails.name)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Journey Recovery when UTR is not present on GET" in {
      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routeUnderTest)

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery when UTR is not present on POST" in {
      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, routeUnderTest)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery on POST when no cached business details found" in {
      val userAnswers = emptyUserAnswers

      val application =
        applicationBuilder(
          userAnswers = Some(userAnswers),
          requestUtr = Some(testUtr.uniqueTaxPayerReference)
        )
          .overrides(bind[RegistrationService].toInstance(mockRegistrationService))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, routeUnderTest)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, routeUnderTest)

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, routeUnderTest)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
