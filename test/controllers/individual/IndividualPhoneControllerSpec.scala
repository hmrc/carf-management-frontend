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

package controllers.individual

import base.SpecBase
import forms.GenericPhoneFormProvider
import models.NormalMode
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import pages.individual.{IndividualNamePage, IndividualPhonePage}
import play.api.data.Form
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.individual.IndividualPhoneView

import scala.concurrent.Future

class IndividualPhoneControllerSpec extends SpecBase {

  def onwardRoute = Call("GET", "/foo")

  val formProvider       = new GenericPhoneFormProvider()
  val form: Form[String] = formProvider("individualPhone")

  lazy val individualPhoneRoute: String =
    controllers.individual.routes.IndividualPhoneController.onPageLoad(NormalMode).url

  "IndividualPhone Controller" - {

    "must return OK and the correct view for a GET when an individual name is present in user answers" in {

      val ua = emptyUserAnswers.withPage(IndividualNamePage, testIndividualName)

      val application = applicationBuilder(userAnswers = Some(ua)).build()

      running(application) {
        val request = FakeRequest(GET, individualPhoneRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[IndividualPhoneView]

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, testIndividualName.fullName)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Some Information is Missing GET when an individual name is NOT present in user answers" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, individualPhoneRoute)

        val result = route(application, request).value

        status(result)               mustEqual SEE_OTHER
        redirectLocation(result).get mustEqual controllers.routes.InformationMissingController.onPageLoad().url
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = emptyUserAnswers
        .withPage(IndividualPhonePage, "answer")
        .withPage(IndividualNamePage, testIndividualName)

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, individualPhoneRoute)

        val view = application.injector.instanceOf[IndividualPhoneView]

        val result = route(application, request).value

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(form.fill("answer"), NormalMode, testIndividualName.fullName)(
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
        val request = FakeRequest(POST, individualPhoneRoute).withFormUrlEncodedBody(("value", "07123456789"))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted and an individual name is present in user answers" in {

      val ua = emptyUserAnswers.withPage(IndividualNamePage, testIndividualName)

      val application = applicationBuilder(userAnswers = Some(ua)).build()

      running(application) {
        val request = FakeRequest(POST, individualPhoneRoute).withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[IndividualPhoneView]

        val result = route(application, request).value

        status(result)          mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, testIndividualName.fullName)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Some Information is Missing when invalid data is submitted and an org name is NOT present in user answers" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, individualPhoneRoute).withFormUrlEncodedBody(("value", ""))

        val result = route(application, request).value

        status(result)               mustEqual SEE_OTHER
        redirectLocation(result).get mustEqual controllers.routes.InformationMissingController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, individualPhoneRoute)

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(POST, individualPhoneRoute).withFormUrlEncodedBody(("value", "answer"))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
