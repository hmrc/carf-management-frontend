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
import controllers.routes
import forms.GenericYesNoPageFormProvider
import models.{NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.{argThat, eq as eqTo}
import org.mockito.Mockito.{times, verify, when}
import pages.organisation.*
import play.api.data.Form
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.organisation.OrganisationFirstContactHavePhoneView

import scala.concurrent.Future

class OrganisationFirstContactHavePhoneControllerSpec extends SpecBase {

  def onwardRoute = Call("GET", "/foo")

  val formProvider        = new GenericYesNoPageFormProvider()
  val form: Form[Boolean] = formProvider("organisationFirstContactHavePhone.error.required")

  lazy val organisationFirstContactHavePhoneRoute: String =
    controllers.organisation.routes.OrganisationFirstContactHavePhoneController.onPageLoad(NormalMode).url

  val firstNameTest: String        = "First Contact Name"
  val organisationNameTest: String = "Organisation Name"

  val userAnswersWithFirstNameAndRcaspNameTest: UserAnswers =
    emptyUserAnswers
      .withPage(OrganisationFirstContactNamePage, firstNameTest)
      .withPage(OverwritableOrganisationName, organisationNameTest)

  "OrganisationFirstContactHavePhone Controller" - {
    "must return OK and the correct view for a GET" in {
      val application = applicationBuilder(userAnswers = Some(userAnswersWithFirstNameAndRcaspNameTest)).build()
      running(application) {
        val request = FakeRequest(GET, organisationFirstContactHavePhoneRoute)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[OrganisationFirstContactHavePhoneView]
        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, firstNameTest, organisationNameTest)(
          request,
          messages(application)
        ).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {
      val userAnswers =
        userAnswersWithFirstNameAndRcaspNameTest.set(OrganisationFirstContactHavePhonePage, true).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()
      running(application) {
        val request = FakeRequest(GET, organisationFirstContactHavePhoneRoute)
        val view    = application.injector.instanceOf[OrganisationFirstContactHavePhoneView]
        val result  = route(application, request).value
        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(form.fill(true), NormalMode, firstNameTest, organisationNameTest)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {
      val expectedUserAnswers =
        userAnswersWithFirstNameAndRcaspNameTest.withPage(OrganisationFirstContactHavePhonePage, true)

      when(mockSessionRepository.set(eqTo(expectedUserAnswers))) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(expectedUserAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute))
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, organisationFirstContactHavePhoneRoute).withFormUrlEncodedBody(("value", "true"))
        val result  = route(application, request).value
        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
        verify(mockSessionRepository, times(1)).set(argThat(_.get(OrganisationFirstContactHavePhonePage).get))
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      val application = applicationBuilder(userAnswers = Some(userAnswersWithFirstNameAndRcaspNameTest)).build()
      running(application) {
        val request =
          FakeRequest(POST, organisationFirstContactHavePhoneRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))
        val view      = application.injector.instanceOf[OrganisationFirstContactHavePhoneView]
        val result    = route(application, request).value
        status(result)          mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, firstNameTest, organisationNameTest)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()
      running(application) {
        val request = FakeRequest(GET, organisationFirstContactHavePhoneRoute)
        val result  = route(application, request).value
        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Some Information Is Missing for a GET if OverwritableOrganisationName is present but OrganisationFirstContactNamePage is None" in {
      val ua = emptyUserAnswers.withPage(OverwritableOrganisationName, organisationNameTest)

      val application = applicationBuilder(userAnswers = Some(ua)).build()
      running(application) {
        val request = FakeRequest(GET, organisationFirstContactHavePhoneRoute)
        val result  = route(application, request).value
        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.InformationMissingController.onPageLoad().url
      }
    }

    "must redirect to Some Information Is Missing for a GET if OrganisationFirstContactNamePage is present but OverwritableOrganisationName is None" in {
      val ua = emptyUserAnswers.withPage(OrganisationFirstContactNamePage, firstNameTest)

      val application = applicationBuilder(userAnswers = Some(ua)).build()
      running(application) {
        val request = FakeRequest(GET, organisationFirstContactHavePhoneRoute)
        val result  = route(application, request).value
        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.InformationMissingController.onPageLoad().url
      }
    }

    "redirect to Journey Recovery for a POST if no existing userAnswers data is found" in {
      val application = applicationBuilder(userAnswers = None).build()
      running(application) {
        val request =
          FakeRequest(POST, organisationFirstContactHavePhoneRoute)
            .withFormUrlEncodedBody(("value", "true"))
        val result  = route(application, request).value
        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
    "redirect to Some Information Is Missing for a POST if the form for OrganisationFirstContactNamePage has errors" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
      running(application) {
        val request =
          FakeRequest(POST, organisationFirstContactHavePhoneRoute)
            .withFormUrlEncodedBody(("value", "invalid  Boolean"))
        val result  = route(application, request).value
        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.InformationMissingController.onPageLoad().url
      }
    }
  }
}
