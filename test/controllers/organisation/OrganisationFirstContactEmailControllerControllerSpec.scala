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
import forms.organisation.GenericOrganisationContactNameFormProvider
import models.NormalMode
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import pages.organisation.{OrganisationFirstContactEmailPage, OrganisationFirstContactNamePage, OverwritableOrganisationName}
import play.api.data.Form
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.organisation.OrganisationFirstContactEmailView

import scala.concurrent.Future

class OrganisationFirstContactEmailControllerControllerSpec extends SpecBase {

  def onwardRoute = Call("GET", "/foo")

  val formProvider       = new GenericOrganisationContactNameFormProvider()
  val form: Form[String] = formProvider("organisationFirstContactEmail")

  val firstContactName = "Timmy"
  val organisationName = "Timmy Ltd"

  lazy val organisationFirstContactEmailRoute: String =
    controllers.organisation.routes.OrganisationFirstContactEmailController.onPageLoad(NormalMode).url

  "OrganisationFirstContactEmail Controller" - {

    "must return OK and the correct view for a GET" in {

      val userAnswers = emptyUserAnswers
        .withPage(OrganisationFirstContactNamePage, firstContactName)
        .withPage(OverwritableOrganisationName, organisationName)

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, organisationFirstContactEmailRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[OrganisationFirstContactEmailView]

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, firstContactName, organisationName)(
          request,
          messages(application)
        ).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = emptyUserAnswers
        .withPage(OrganisationFirstContactNamePage, firstContactName)
        .withPage(OverwritableOrganisationName, organisationName)
        .withPage(OrganisationFirstContactEmailPage, "anexampleevalidemail@email.com")

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, organisationFirstContactEmailRoute)

        val view = application.injector.instanceOf[OrganisationFirstContactEmailView]

        val result = route(application, request).value

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(
          form.fill("anexampleevalidemail@email.com"),
          NormalMode,
          firstContactName,
          organisationName
        )(
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
          FakeRequest(POST, organisationFirstContactEmailRoute)
            .withFormUrlEncodedBody(("value", "answer@email.com"))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val userAnswers = emptyUserAnswers
        .withPage(OrganisationFirstContactNamePage, firstContactName)
        .withPage(OverwritableOrganisationName, organisationName)

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()
      running(application) {
        val request =
          FakeRequest(POST, organisationFirstContactEmailRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[OrganisationFirstContactEmailView]

        val result = route(application, request).value

        status(result)          mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, firstContactName, organisationName)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, organisationFirstContactEmailRoute)

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Some Information Is Missing for a GET if no first contact name is found" in {

      val userAnswers = emptyUserAnswers
        .withPage(OrganisationFirstContactEmailPage, "test@example.com")
        .withPage(OverwritableOrganisationName, organisationName)

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request =
          FakeRequest(GET, organisationFirstContactEmailRoute)

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.InformationMissingController.onPageLoad().url
      }
    }

    "must redirect to Some Information Is Missing for a GET if no organisation name is found" in {

      val userAnswers = emptyUserAnswers
        .withPage(OrganisationFirstContactEmailPage, "test@example.com")
        .withPage(OrganisationFirstContactNamePage, firstContactName)

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request =
          FakeRequest(GET, organisationFirstContactEmailRoute)

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.InformationMissingController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, organisationFirstContactEmailRoute)
            .withFormUrlEncodedBody(("value", "wrong email format@"))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Some Information Is Missing for a POST if no firstContactName is found" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, organisationFirstContactEmailRoute)
            .withFormUrlEncodedBody(("value", "email.com"))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.InformationMissingController.onPageLoad().url
      }
    }

    "must redirect to Some Information Is Missing for a POST if no organisationName is found" in {

      val userAnswers = emptyUserAnswers
        .withPage(OrganisationFirstContactEmailPage, "test@example.com")
        .withPage(OrganisationFirstContactNamePage, firstContactName)

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, organisationFirstContactEmailRoute)
            .withFormUrlEncodedBody(("value", "email.com"))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.InformationMissingController.onPageLoad().url
      }
    }
  }
}
