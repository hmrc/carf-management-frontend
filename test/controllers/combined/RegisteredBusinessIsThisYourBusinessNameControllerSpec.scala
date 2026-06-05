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
import models.NormalMode
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import pages.combined.RegisteredBusinessIsThisYourBusinessNamePage
import pages.organisation.OverwritableOrganisationName
import play.api.data.Form
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.combined.RegisteredBusinessIsThisYourBusinessNameView

import scala.concurrent.Future

class RegisteredBusinessIsThisYourBusinessNameControllerSpec extends SpecBase {

  def onwardRoute = Call("GET", "/foo")

  val formProvider        = new GenericYesNoPageFormProvider()
  val form: Form[Boolean] = formProvider("registeredBusinessIsThisYourBusinessName.error.required")

  lazy val routeUnderTest: String =
    controllers.combined.routes.RegisteredBusinessIsThisYourBusinessNameController.onPageLoad(NormalMode).url

  "RegisteredBusinessIsThisYourBusinessName Controller" - {

    "must return OK and the correct view for a GET when an org name is present" in {
      val ua = emptyUserAnswers.withPage(OverwritableOrganisationName, testOrgName)

      val application = applicationBuilder(userAnswers = Some(ua)).build()

      running(application) {
        val request = FakeRequest(GET, routeUnderTest)

        val result = route(application, request).value

        val view = application.injector.instanceOf[RegisteredBusinessIsThisYourBusinessNameView]

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, testOrgName)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {
      val userAnswers = emptyUserAnswers
        .withPage(OverwritableOrganisationName, testOrgName)
        .withPage(RegisteredBusinessIsThisYourBusinessNamePage, true)

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routeUnderTest)

        val view = application.injector.instanceOf[RegisteredBusinessIsThisYourBusinessNameView]

        val result = route(application, request).value

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(form.fill(true), NormalMode, testOrgName)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Some Information is Missing when org name is not present" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routeUnderTest)

        val result = route(application, request).value

        status(result)               mustEqual SEE_OTHER
        redirectLocation(result).get mustEqual controllers.routes.PlaceholderController
          .onPageLoad("Should redirect to Some Information is Missing Page (CARF-293)")
          .url
      }
    }

    "must redirect to the next page when valid data is submitted" in {
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers.withPage(OverwritableOrganisationName, testOrgName)))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute))
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

    "must return a Bad Request and errors when invalid data is submitted and an org name is present" in {
      val ua = emptyUserAnswers.withPage(OverwritableOrganisationName, testOrgName)

      val application = applicationBuilder(userAnswers = Some(ua)).build()

      running(application) {
        val request =
          FakeRequest(POST, routeUnderTest)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[RegisteredBusinessIsThisYourBusinessNameView]

        val result = route(application, request).value

        status(result)          mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, testOrgName)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Some Information is Missing when invalid data is submitted and org name is not present" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, routeUnderTest)
            .withFormUrlEncodedBody(("value", ""))

        val result = route(application, request).value

        status(result)               mustEqual SEE_OTHER
        redirectLocation(result).get mustEqual controllers.routes.PlaceholderController
          .onPageLoad("Should redirect to Some Information is Missing Page (CARF-293)")
          .url
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
