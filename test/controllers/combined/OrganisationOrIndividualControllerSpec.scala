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
import forms.combined.OrganisationOrIndividualFormProvider
import models.individual.IndividualName
import models.{ChangeMode, NormalMode, OrganisationOrIndividual}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.{any, argThat}
import org.mockito.Mockito.{verify, when}
import pages.changeDetails.ChangeRcaspCachedDetails
import pages.combined.OrganisationOrIndividualPage
import pages.individual.*
import pages.organisation.*
import play.api.data.Form
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.combined.OrganisationOrIndividualView

import scala.concurrent.Future

class OrganisationOrIndividualControllerSpec extends SpecBase {

  def onwardRoute = Call("GET", "/foo")

  val formProvider                         = new OrganisationOrIndividualFormProvider()
  val form: Form[OrganisationOrIndividual] = formProvider("organisationOrIndividual.error.required")

  lazy val routeUnderTest: String           =
    controllers.combined.routes.OrganisationOrIndividualController.onPageLoad(NormalMode).url
  lazy val routeUnderChangeModeTest: String =
    controllers.combined.routes.OrganisationOrIndividualController.onPageLoad(ChangeMode).url

  "OrganisationOrIndividual Controller" - {
    "normal mode" - {
      "must return OK and the correct view for a GET" in {
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, routeUnderTest)
          val result  = route(application, request).value
          val view    = application.injector.instanceOf[OrganisationOrIndividualView]

          status(result)          mustEqual OK
          contentAsString(result) mustEqual view(form, NormalMode)(request, messages(application)).toString
        }
      }

      "must populate the view correctly on a GET when the question has previously been answered" in {
        val userAnswers =
          emptyUserAnswers
            .withPage(OrganisationOrIndividualPage, OrganisationOrIndividual.Organisation)

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, routeUnderTest)
          val view    = application.injector.instanceOf[OrganisationOrIndividualView]

          val result = route(application, request).value

          status(result)          mustEqual OK
          contentAsString(result) mustEqual view(form.fill(OrganisationOrIndividual.Organisation), NormalMode)(
            request,
            messages(application)
          ).toString
        }
      }

      "must redirect to the next page when valid data is submitted" in {
        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        val application =
          applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              bind[Navigator].toInstance(new FakeNavigator(onwardRoute))
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, routeUnderTest)
              .withFormUrlEncodedBody(("value", "Organisation"))

          val result = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url
        }
      }

      "must return a Bad Request and errors when invalid data is submitted" in {
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request =
            FakeRequest(POST, routeUnderTest)
              .withFormUrlEncodedBody(("value", ""))

          val boundForm = form.bind(Map("value" -> ""))

          val view = application.injector.instanceOf[OrganisationOrIndividualView]

          val result = route(application, request).value

          status(result)          mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm, NormalMode)(request, messages(application)).toString
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
              .withFormUrlEncodedBody(("value", "Organisation"))

          val result = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    "change mode" - {
      "must redirect to the OrganisationNameController and also clear Individual pages when answer is changed from [Individual -> Organisation]" in {
        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        val userAnswers = emptyUserAnswers
          .withPage(OrganisationOrIndividualPage, OrganisationOrIndividual.Individual)
          .withPage(IndividualNamePage, IndividualName(testName, "lastName"))
          .withPage(NiNumberPage, testNiNumber)
          .withPage(IndividualEmailPage, testEmail)
          .withPage(IndividualHavePhonePage, true)
          .withPage(IndividualPhonePage, testPhone)

        val application =
          applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(
              bind[Navigator].toInstance(new FakeNavigator(onwardRoute))
            )
            .build()

        running(application) {

          val request = FakeRequest(POST, routeUnderChangeModeTest)
            .withFormUrlEncodedBody(("value", OrganisationOrIndividual.Organisation.toString))

          val result = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.organisation.routes.OrganisationNameController
            .onPageLoad(NormalMode)
            .url

          verify(mockSessionRepository).set(argThat { ua =>
            ua.get(OrganisationOrIndividualPage).contains(OrganisationOrIndividual.Organisation) &&
            ua.get(IndividualNamePage).isEmpty &&
            ua.get(NiNumberPage).isEmpty &&
            ua.get(IndividualEmailPage).isEmpty &&
            ua.get(IndividualHavePhonePage).isEmpty &&
            ua.get(IndividualPhonePage).isEmpty
          })
        }
      }

      "must redirect to the IndividualNameController and also clear Organisation pages when answer is changed from [Organisation -> Individual]" in {
        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        val userAnswers = emptyUserAnswers
          .withPage(OrganisationOrIndividualPage, OrganisationOrIndividual.Organisation)
          .withPage(OrganisationNamePage, "Test Organisation Name")
          .withPage(OverwritableOrganisationName, "Test Overwritable Name")
          .withPage(HaveTradingNamePage, true)
          .withPage(TradingNamePage, testTradingName)
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

        val application =
          applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(
              bind[Navigator].toInstance(new FakeNavigator(onwardRoute))
            )
            .build()

        running(application) {

          val request = FakeRequest(POST, routeUnderChangeModeTest)
            .withFormUrlEncodedBody(("value", OrganisationOrIndividual.Individual.toString))

          val result = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.individual.routes.IndividualNameController
            .onPageLoad(NormalMode)
            .url

          verify(mockSessionRepository).set(argThat { ua =>
            ua.get(OrganisationOrIndividualPage).contains(OrganisationOrIndividual.Individual) &&
            ua.get(OrganisationNamePage).isEmpty &&
            ua.get(OverwritableOrganisationName).isEmpty &&
            ua.get(HaveTradingNamePage).isEmpty &&
            ua.get(TradingNamePage).isEmpty &&
            ua.get(UtrPage).isEmpty &&
            ua.get(OrganisationFirstContactNamePage).isEmpty &&
            ua.get(OrganisationFirstContactEmailPage).isEmpty &&
            ua.get(OrganisationFirstContactHavePhonePage).isEmpty &&
            ua.get(OrganisationFirstContactPhoneNumberPage).isEmpty &&
            ua.get(OrganisationHaveSecondContactPage).isEmpty &&
            ua.get(OrganisationSecondContactNamePage).isEmpty &&
            ua.get(OrganisationSecondContactEmailPage).isEmpty &&
            ua.get(OrganisationSecondContactHavePhonePage).isEmpty &&
            ua.get(OrganisationSecondContactPhoneNumberPage).isEmpty
          })
        }
      }

      "must redirect to ChangeDetailsRoutingController and NOT clear Organisation pages when answer remains unchanged [Organisation -> Organisation]" in {
        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
        val userAnswers = emptyUserAnswers
          .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsResponse)
          .withPage(OrganisationOrIndividualPage, OrganisationOrIndividual.Organisation)
          .withPage(OrganisationNamePage, "Test Organisation Name")
          .withPage(OverwritableOrganisationName, "Test Overwritable Name")
          .withPage(HaveTradingNamePage, true)
          .withPage(TradingNamePage, testTradingName)
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

        val application =
          applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(
              bind[Navigator].toInstance(new FakeNavigator(onwardRoute))
            )
            .build()

        running(application) {

          val request = FakeRequest(POST, routeUnderChangeModeTest)
            .withFormUrlEncodedBody(("value", OrganisationOrIndividual.Organisation.toString))

          val result = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.changeDetails.routes.ChangeDetailsRoutingController
            .onPageLoad(rcaspId)
            .url

          verify(mockSessionRepository).set(argThat { ua =>
            ua.get(OrganisationOrIndividualPage).contains(OrganisationOrIndividual.Organisation) &&
            ua.get(OrganisationNamePage).isDefined &&
            ua.get(OverwritableOrganisationName).isDefined &&
            ua.get(HaveTradingNamePage).isDefined &&
            ua.get(TradingNamePage).isDefined &&
            ua.get(UtrPage).isDefined &&
            ua.get(OrganisationFirstContactNamePage).isDefined &&
            ua.get(OrganisationFirstContactEmailPage).isDefined &&
            ua.get(OrganisationFirstContactHavePhonePage).isDefined &&
            ua.get(OrganisationFirstContactPhoneNumberPage).isDefined &&
            ua.get(OrganisationHaveSecondContactPage).isDefined &&
            ua.get(OrganisationSecondContactNamePage).isDefined &&
            ua.get(OrganisationSecondContactEmailPage).isDefined &&
            ua.get(OrganisationSecondContactHavePhonePage).isDefined &&
            ua.get(OrganisationSecondContactPhoneNumberPage).isDefined
          })
        }
      }

      "must redirect to ChangeDetailsRoutingController and NOT clear Individual pages when answer remains unchanged [Individual -> Individual]" in {
        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
        val userAnswers = emptyUserAnswers
          .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsResponse)
          .withPage(OrganisationOrIndividualPage, OrganisationOrIndividual.Individual)
          .withPage(IndividualNamePage, IndividualName(testName, "lastName"))
          .withPage(NiNumberPage, testNiNumber)
          .withPage(IndividualEmailPage, testEmail)
          .withPage(IndividualHavePhonePage, true)
          .withPage(IndividualPhonePage, testPhone)

        val application =
          applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(
              bind[Navigator].toInstance(new FakeNavigator(onwardRoute))
            )
            .build()

        running(application) {

          val request = FakeRequest(POST, routeUnderChangeModeTest)
            .withFormUrlEncodedBody(("value", OrganisationOrIndividual.Individual.toString))

          val result = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.changeDetails.routes.ChangeDetailsRoutingController
            .onPageLoad(rcaspId)
            .url

          verify(mockSessionRepository).set(argThat { ua =>
            ua.get(OrganisationOrIndividualPage).contains(OrganisationOrIndividual.Individual) &&
            ua.get(IndividualNamePage).isDefined &&
            ua.get(NiNumberPage).isDefined &&
            ua.get(IndividualEmailPage).isDefined &&
            ua.get(IndividualHavePhonePage).isDefined &&
            ua.get(IndividualPhonePage).isDefined
          })
        }
      }

      "must redirect to Journey Recover when ChangeRcaspCachedDetails is not available when answer remains unchanged [Individual -> Individual]" in {
        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
        val userAnswers = emptyUserAnswers
          .withPage(OrganisationOrIndividualPage, OrganisationOrIndividual.Individual)
          .withPage(IndividualNamePage, IndividualName(testName, "lastName"))
          .withPage(NiNumberPage, testNiNumber)
          .withPage(IndividualEmailPage, testEmail)
          .withPage(IndividualHavePhonePage, true)
          .withPage(IndividualPhonePage, testPhone)

        val application =
          applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(
              bind[Navigator].toInstance(new FakeNavigator(onwardRoute))
            )
            .build()

        running(application) {

          val request = FakeRequest(POST, routeUnderChangeModeTest)
            .withFormUrlEncodedBody(("value", OrganisationOrIndividual.Individual.toString))

          val result = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

          verify(mockSessionRepository).set(argThat { ua =>
            ua.get(OrganisationOrIndividualPage).contains(OrganisationOrIndividual.Individual) &&
            ua.get(IndividualNamePage).isDefined &&
            ua.get(NiNumberPage).isDefined &&
            ua.get(IndividualEmailPage).isDefined &&
            ua.get(IndividualHavePhonePage).isDefined &&
            ua.get(IndividualPhonePage).isDefined
          })
        }
      }
    }
  }
}
