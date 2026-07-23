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
import models.errors.ApiError.InternalServerError
import models.responses.AddressRegistrationResponse
import models.{BusinessDetails, CachedBusinessDetails, NormalMode}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.{any, argThat, eq as eqTo}
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalacheck.Gen
import pages.changeDetails.ChangeRcaspCachedDetails
import pages.organisation.{CachedBusinessDetailsPage, ReportForRegisteredBusinessPage}
import play.api.data.Form
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.{AccountService, RegistrationService}
import types.ResultT
import utils.CountryListFactory
import views.html.organisation.ReportForRegisteredBusinessView

import scala.concurrent.Future

class ReportForRegisteredBusinessControllerSpec extends SpecBase {

  def onwardRoute = Call("GET", "/foo")

  val formProvider        = new GenericYesNoPageFormProvider()
  val form: Form[Boolean] = formProvider("reportForRegisteredBusiness.error.required")

  val mockRegistrationService: RegistrationService = mock[RegistrationService]
  val mockCountryListFactory: CountryListFactory   = mock[CountryListFactory]

  lazy val routeUnderTest: String =
    controllers.organisation.routes.ReportForRegisteredBusinessController.onPageLoad(NormalMode).url

  val businessDetailsFromService =
    BusinessDetails(
      name = "Timmy Ltd",
      address = AddressRegistrationResponse(
        addressLine1 = "1 Test Street",
        addressLine2 = Some("Testville"),
        addressLine3 = None,
        addressLine4 = None,
        postalCode = Some("TE1 1ST"),
        countryCode = "GB"
      )
    )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockRegistrationService, mockCountryListFactory)
  }

  "ReportForRegisteredBusiness Controller" - {

    "must return OK and the correct view for a GET when a UTR is present and CachedBusinessDetails are present" in {
      val application =
        applicationBuilder(
          userAnswers = Some(emptyUserAnswers.withPage(CachedBusinessDetailsPage, cachedBusinessDetails)),
          requestUtr = Some(testUtr.uniqueTaxPayerReference)
        ).overrides(
          bind[RegistrationService].toInstance(mockRegistrationService),
          bind[CountryListFactory].toInstance(mockCountryListFactory)
        ).build()

      running(application) {
        val request = FakeRequest(GET, routeUnderTest)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[ReportForRegisteredBusinessView]

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(
          form,
          NormalMode,
          cachedBusinessDetails.name
        )(request, messages(application)).toString

        verify(mockRegistrationService, times(0)).getBusinessWithCtUtr(any())(any())
        verify(mockCountryListFactory, times(0)).getDescriptionFromCode(any())
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered and CachedBusinessDetails are present" in {
      val userAnswers = emptyUserAnswers
        .withPage(CachedBusinessDetailsPage, cachedBusinessDetails)
        .withPage(ReportForRegisteredBusinessPage, true)

      val application =
        applicationBuilder(
          userAnswers = Some(userAnswers),
          requestUtr = Some(testUtr.uniqueTaxPayerReference)
        ).overrides(
          bind[RegistrationService].toInstance(mockRegistrationService),
          bind[CountryListFactory].toInstance(mockCountryListFactory)
        ).build()

      running(application) {
        val request = FakeRequest(GET, routeUnderTest)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[ReportForRegisteredBusinessView]

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(
          form.fill(true),
          NormalMode,
          cachedBusinessDetails.name
        )(request, messages(application)).toString

        verify(mockRegistrationService, times(0)).getBusinessWithCtUtr(any())(any())
        verify(mockCountryListFactory, times(0)).getDescriptionFromCode(any())
      }
    }

    "must return OK and the correct view for a GET when a UTR is present and CachedBusinessDetails are not present" in {
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      when(mockRegistrationService.getBusinessWithCtUtr(any())(any()))
        .thenReturn(ResultT.fromValue(businessDetailsFromService))

      when(mockCountryListFactory.getDescriptionFromCode(any())).thenReturn(Some("United Kingdom"))

      val application =
        applicationBuilder(
          userAnswers = Some(emptyUserAnswers),
          requestUtr = Some(testUtr.uniqueTaxPayerReference)
        ).overrides(
          bind[RegistrationService].toInstance(mockRegistrationService),
          bind[CountryListFactory].toInstance(mockCountryListFactory)
        ).build()

      running(application) {
        val request = FakeRequest(GET, routeUnderTest)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[ReportForRegisteredBusinessView]

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(
          form,
          NormalMode,
          cachedBusinessDetails.name
        )(request, messages(application)).toString

        verify(mockRegistrationService, times(1)).getBusinessWithCtUtr(eqTo(testUtr.uniqueTaxPayerReference))(any())
        verify(mockCountryListFactory, times(1)).getDescriptionFromCode(eqTo("GB"))
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered and CachedBusinessDetails are not present" in {
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      when(mockRegistrationService.getBusinessWithCtUtr(any())(any()))
        .thenReturn(ResultT.fromValue(businessDetailsFromService))

      when(mockCountryListFactory.getDescriptionFromCode(any())).thenReturn(Some("United Kingdom"))

      val userAnswers = emptyUserAnswers.withPage(ReportForRegisteredBusinessPage, true)

      val application =
        applicationBuilder(
          userAnswers = Some(userAnswers),
          requestUtr = Some(testUtr.uniqueTaxPayerReference)
        ).overrides(
          bind[RegistrationService].toInstance(mockRegistrationService),
          bind[CountryListFactory].toInstance(mockCountryListFactory)
        ).build()

      running(application) {
        val request = FakeRequest(GET, routeUnderTest)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[ReportForRegisteredBusinessView]

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(
          form.fill(true),
          NormalMode,
          cachedBusinessDetails.name
        )(request, messages(application)).toString

        verify(mockRegistrationService, times(1)).getBusinessWithCtUtr(eqTo(testUtr.uniqueTaxPayerReference))(any())
        verify(mockCountryListFactory, times(1)).getDescriptionFromCode(eqTo("GB"))
      }
    }

    "must redirect to Journey Recovery on GET when registration service returns an error" in {
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      when(mockRegistrationService.getBusinessWithCtUtr(any())(any()))
        .thenReturn(ResultT.fromError(InternalServerError))

      val application =
        applicationBuilder(
          userAnswers = Some(emptyUserAnswers),
          requestUtr = Some(testUtr.uniqueTaxPayerReference)
        ).overrides(
          bind[RegistrationService].toInstance(mockRegistrationService),
          bind[CountryListFactory].toInstance(mockCountryListFactory)
        ).build()

      running(application) {
        val request = FakeRequest(GET, routeUnderTest)
        val result  = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

        verify(mockRegistrationService, times(1)).getBusinessWithCtUtr(eqTo(testUtr.uniqueTaxPayerReference))(any())
        verify(mockCountryListFactory, times(0)).getDescriptionFromCode(any())
      }
    }

    "must redirect to Journey Recovery on GET when country code is not found in country list" in {
      when(mockRegistrationService.getBusinessWithCtUtr(any())(any()))
        .thenReturn(ResultT.fromValue(businessDetailsFromService))

      when(mockCountryListFactory.getDescriptionFromCode(any())).thenReturn(None)

      val application =
        applicationBuilder(
          userAnswers = Some(emptyUserAnswers),
          requestUtr = Some(testUtr.uniqueTaxPayerReference)
        ).overrides(
          bind[RegistrationService].toInstance(mockRegistrationService),
          bind[CountryListFactory].toInstance(mockCountryListFactory)
        ).build()

      running(application) {
        val request = FakeRequest(GET, routeUnderTest)
        val result  = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

        verify(mockRegistrationService, times(1)).getBusinessWithCtUtr(eqTo(testUtr.uniqueTaxPayerReference))(any())
        verify(mockCountryListFactory, times(1)).getDescriptionFromCode(eqTo("GB"))
      }
    }

    "must redirect to Journey Recovery on GET when CT UTR is not found in the request" in {
      when(mockRegistrationService.getBusinessWithCtUtr(any())(any()))
        .thenReturn(ResultT.fromValue(businessDetailsFromService))

      when(mockCountryListFactory.getDescriptionFromCode(any())).thenReturn(None)

      val application =
        applicationBuilder(
          userAnswers = Some(emptyUserAnswers),
          requestUtr = None
        ).overrides(
          bind[RegistrationService].toInstance(mockRegistrationService),
          bind[CountryListFactory].toInstance(mockCountryListFactory)
        ).build()

      running(application) {
        val request = FakeRequest(GET, routeUnderTest)
        val result  = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

        verify(mockRegistrationService, times(0)).getBusinessWithCtUtr(any())(any())
        verify(mockCountryListFactory, times(0)).getDescriptionFromCode(any())
      }
    }

    "when the user is in the add journey (ChangeRcaspCachedDetails is absent)" - {
      "when the user answers Yes to the question, has a CT UTR and has zero RCASPs added" - {
        "must redirect to the next page and set rcaspIsRegisteredBusiness to true" in {
          val mockAccountService: AccountService = mock[AccountService]

          val userAnswers = emptyUserAnswers.withPage(CachedBusinessDetailsPage, cachedBusinessDetails)

          when(mockAccountService.getNumberOfRcaspsCurrentlyAdded(any())(any(), any())).thenReturn(ResultT.fromValue(0))
          when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

          val application =
            applicationBuilder(
              userAnswers = Some(userAnswers),
              requestUtr = Some(testUtr.uniqueTaxPayerReference)
            ).overrides(
              bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
              bind[AccountService].toInstance(mockAccountService)
            ).build()

          running(application) {
            val request = FakeRequest(POST, routeUnderTest).withFormUrlEncodedBody(("value", "true"))
            val result  = route(application, request).value

            status(result)                 mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual onwardRoute.url

            verify(mockSessionRepository).set(argThat(_.rcaspIsRegisteredBusiness))
            verify(mockAccountService).getNumberOfRcaspsCurrentlyAdded(any())(any(), any())
          }
        }
      }

      "when the user answers No to the question, has a CT UTR and has zero RCASPs added" - {
        "must redirect to the next page and keep rcaspIsRegisteredBusiness as false" in {
          val mockAccountService: AccountService = mock[AccountService]

          val userAnswers = emptyUserAnswers.withPage(CachedBusinessDetailsPage, cachedBusinessDetails)

          when(mockAccountService.getNumberOfRcaspsCurrentlyAdded(any())(any(), any())).thenReturn(ResultT.fromValue(0))
          when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

          val application =
            applicationBuilder(
              userAnswers = Some(userAnswers),
              requestUtr = Some(testUtr.uniqueTaxPayerReference)
            ).overrides(
              bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
              bind[AccountService].toInstance(mockAccountService)
            ).build()

          running(application) {
            val request = FakeRequest(POST, routeUnderTest).withFormUrlEncodedBody(("value", "false"))
            val result  = route(application, request).value

            status(result)                 mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual onwardRoute.url

            verify(mockSessionRepository).set(argThat(!_.rcaspIsRegisteredBusiness))
            verify(mockAccountService).getNumberOfRcaspsCurrentlyAdded(any())(any(), any())
          }
        }
      }

      "when the user answers Yes to the question, has a CT UTR and has more than zero RCASPs added" - {
        "must redirect to the next page and keep rcaspIsRegisteredBusiness as false" in {
          val mockAccountService: AccountService = mock[AccountService]

          val userAnswers = emptyUserAnswers.withPage(CachedBusinessDetailsPage, cachedBusinessDetails)

          when(mockAccountService.getNumberOfRcaspsCurrentlyAdded(any())(any(), any())).thenReturn(ResultT.fromValue(1))
          when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

          val application =
            applicationBuilder(
              userAnswers = Some(userAnswers),
              requestUtr = Some(testUtr.uniqueTaxPayerReference)
            ).overrides(
              bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
              bind[AccountService].toInstance(mockAccountService)
            ).build()

          running(application) {
            val request = FakeRequest(POST, routeUnderTest).withFormUrlEncodedBody(("value", "true"))
            val result  = route(application, request).value

            status(result)                 mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual onwardRoute.url

            verify(mockSessionRepository).set(argThat(!_.rcaspIsRegisteredBusiness))
            verify(mockAccountService).getNumberOfRcaspsCurrentlyAdded(any())(any(), any())
          }
        }
      }

      "when the user answers Yes to the question, does NOT have a CT UTR and has zero RCASPs added" - {
        "must redirect to the next page and keep rcaspIsRegisteredBusiness as false" in {
          val mockAccountService: AccountService = mock[AccountService]

          val userAnswers = emptyUserAnswers.withPage(CachedBusinessDetailsPage, cachedBusinessDetails)

          when(mockAccountService.getNumberOfRcaspsCurrentlyAdded(any())(any(), any())).thenReturn(ResultT.fromValue(0))
          when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

          val application =
            applicationBuilder(
              userAnswers = Some(userAnswers),
              requestUtr = None
            ).overrides(
              bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
              bind[AccountService].toInstance(mockAccountService)
            ).build()

          running(application) {
            val request = FakeRequest(POST, routeUnderTest).withFormUrlEncodedBody(("value", "true"))
            val result  = route(application, request).value

            status(result)                 mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual onwardRoute.url

            verify(mockSessionRepository).set(argThat(!_.rcaspIsRegisteredBusiness))
            verify(mockAccountService).getNumberOfRcaspsCurrentlyAdded(any())(any(), any())
          }
        }
      }

      "when the user answers has rcaspIsRegisteredBusiness as true, but fails the conditions for being a registered business" - {
        "must redirect to the next page and change rcaspIsRegisteredBusiness to false" in {
          val mockAccountService: AccountService = mock[AccountService]

          val userAnswers = emptyUserAnswers
            .copy(rcaspIsRegisteredBusiness = true)
            .withPage(CachedBusinessDetailsPage, cachedBusinessDetails)

          when(mockAccountService.getNumberOfRcaspsCurrentlyAdded(any())(any(), any())).thenReturn(ResultT.fromValue(0))
          when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

          val application =
            applicationBuilder(
              userAnswers = Some(userAnswers),
              requestUtr = Some(testUtr.uniqueTaxPayerReference)
            ).overrides(
              bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
              bind[AccountService].toInstance(mockAccountService)
            ).build()

          running(application) {
            val request = FakeRequest(POST, routeUnderTest).withFormUrlEncodedBody(("value", "false"))
            val result  = route(application, request).value

            status(result)                 mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual onwardRoute.url

            verify(mockSessionRepository).set(argThat(!_.rcaspIsRegisteredBusiness))
            verify(mockAccountService).getNumberOfRcaspsCurrentlyAdded(any())(any(), any())
          }
        }
      }
    }

    "when the user is in the change journey (ChangeRcaspCachedDetails is present)" - {
      "when the cached details are for the registered business" - {
        "must redirect to the next page and keep rcaspIsRegisteredBusiness as true when the answer is Yes" in {
          val mockAccountService: AccountService = mock[AccountService]

          val userAnswers = emptyUserAnswers
            .copy(rcaspIsRegisteredBusiness = true)
            .withPage(CachedBusinessDetailsPage, cachedBusinessDetails)
            .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate.copy(IsRCASPUser = true))

          when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

          val application =
            applicationBuilder(
              userAnswers = Some(userAnswers),
              requestUtr = Some(testUtr.uniqueTaxPayerReference)
            ).overrides(
              bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
              bind[AccountService].toInstance(mockAccountService)
            ).build()

          running(application) {
            val request = FakeRequest(POST, routeUnderTest).withFormUrlEncodedBody(("value", "true"))
            val result  = route(application, request).value

            status(result)                 mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual onwardRoute.url

            verify(mockSessionRepository).set(argThat(_.rcaspIsRegisteredBusiness))
            verify(mockAccountService, times(0)).getNumberOfRcaspsCurrentlyAdded(any())(any(), any())
          }
        }

        "must redirect to the next page and set rcaspIsRegisteredBusiness to false when the answer is No" in {
          val mockAccountService: AccountService = mock[AccountService]

          val userAnswers = emptyUserAnswers
            .copy(rcaspIsRegisteredBusiness = true)
            .withPage(CachedBusinessDetailsPage, cachedBusinessDetails)
            .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate.copy(IsRCASPUser = true))

          when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

          val application =
            applicationBuilder(
              userAnswers = Some(userAnswers),
              requestUtr = Some(testUtr.uniqueTaxPayerReference)
            ).overrides(
              bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
              bind[AccountService].toInstance(mockAccountService)
            ).build()

          running(application) {
            val request = FakeRequest(POST, routeUnderTest).withFormUrlEncodedBody(("value", "false"))
            val result  = route(application, request).value

            status(result)                 mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual onwardRoute.url

            verify(mockSessionRepository).set(argThat(!_.rcaspIsRegisteredBusiness))
            verify(mockAccountService, times(0)).getNumberOfRcaspsCurrentlyAdded(any())(any(), any())
          }
        }
      }

      "when the cached details are not for the registered business" - {
        "must redirect to the next page and keep rcaspIsRegisteredBusiness as false" in {
          val mockAccountService: AccountService = mock[AccountService]

          val userAnswers = emptyUserAnswers
            .copy(rcaspIsRegisteredBusiness = false)
            .withPage(CachedBusinessDetailsPage, cachedBusinessDetails)
            .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate)

          when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

          val application =
            applicationBuilder(
              userAnswers = Some(userAnswers),
              requestUtr = Some(testUtr.uniqueTaxPayerReference)
            ).overrides(
              bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
              bind[AccountService].toInstance(mockAccountService)
            ).build()

          running(application) {
            val pageAnswer = Gen.oneOf("true", "false").sample.get
            val request    = FakeRequest(POST, routeUnderTest).withFormUrlEncodedBody(("value", pageAnswer))
            val result     = route(application, request).value

            status(result)                 mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual onwardRoute.url

            verify(mockSessionRepository).set(argThat(!_.rcaspIsRegisteredBusiness))
            verify(mockAccountService, times(0)).getNumberOfRcaspsCurrentlyAdded(any())(any(), any())
          }
        }
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      val userAnswers =
        emptyUserAnswers
          .withPage(CachedBusinessDetailsPage, cachedBusinessDetails)

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .build()

      running(application) {
        val request = FakeRequest(POST, routeUnderTest).withFormUrlEncodedBody(("value", ""))
        val result  = route(application, request).value

        val boundForm = form.bind(Map("value" -> ""))
        val view      = application.injector.instanceOf[ReportForRegisteredBusinessView]

        status(result)          mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(
          boundForm,
          NormalMode,
          cachedBusinessDetails.name
        )(request, messages(application)).toString
      }
    }
  }
}
