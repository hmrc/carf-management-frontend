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
import forms.organisation.UtrFormProvider
import models.NormalMode
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import pages.organisation.{OverwritableOrganisationName, UtrPage}
import play.api.data.Form
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.organisation.UtrView

import scala.concurrent.Future

class UtrControllerSpec extends SpecBase {

  def onwardRoute: Call = Call("GET", "/foo")

  val formProvider       = new UtrFormProvider()
  val form: Form[String] = formProvider()

  lazy val utrRoute: String =
    controllers.organisation.routes.UtrController.onPageLoad(NormalMode).url

  "Utr Controller" - {

    "must return OK and the correct view for a GET when organisation name is present in user answers" in {

      val ua = emptyUserAnswers.withPage(OverwritableOrganisationName, testOrgName)

      val application = applicationBuilder(userAnswers = Some(ua)).build()

      running(application) {
        val request = FakeRequest(GET, utrRoute)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[UtrView]

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, testOrgName)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val ua = emptyUserAnswers
        .withPage(UtrPage, "1234567890")
        .withPage(OverwritableOrganisationName, testOrgName)

      val application = applicationBuilder(userAnswers = Some(ua)).build()

      running(application) {
        val request = FakeRequest(GET, utrRoute)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[UtrView]

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(form.fill("1234567890"), NormalMode, testOrgName)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Journey Recovery for a GET when organisation name is not found" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, utrRoute)
        val result  = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to the next page when valid data is submitted" in {
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[Navigator].toInstance(new FakeNavigator(onwardRoute))
        )
        .build()

      running(application) {
        val request =
          FakeRequest(POST, utrRoute)
            .withFormUrlEncodedBody(("value", "1234567890"))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val ua = emptyUserAnswers.withPage(OverwritableOrganisationName, testOrgName)

      val application = applicationBuilder(userAnswers = Some(ua)).build()

      running(application) {
        val request =
          FakeRequest(POST, utrRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))
        val view      = application.injector.instanceOf[UtrView]
        val result    = route(application, request).value

        status(result)          mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, testOrgName)(
          request,
          messages(application)
        ).toString
      }
    }

    "must return a Bad Request when UTR contains non-numeric characters" in {

      val ua = emptyUserAnswers.withPage(OverwritableOrganisationName, testOrgName)

      val application = applicationBuilder(userAnswers = Some(ua)).build()

      running(application) {
        val request =
          FakeRequest(POST, utrRoute)
            .withFormUrlEncodedBody(("value", "123456789A"))

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
      }
    }

    "must return a Bad Request when UTR is wrong length" in {

      val ua = emptyUserAnswers.withPage(OverwritableOrganisationName, testOrgName)

      val application = applicationBuilder(userAnswers = Some(ua)).build()

      running(application) {
        val request =
          FakeRequest(POST, utrRoute)
            .withFormUrlEncodedBody(("value", "123456789"))

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
      }
    }

    "must redirect to Journey Recovery for a POST when invalid data is submitted and organisation name is not found" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, utrRoute)
            .withFormUrlEncodedBody(("value", ""))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, utrRoute)
        val result  = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, utrRoute)
            .withFormUrlEncodedBody(("value", "1234567890"))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
