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
import models.NormalMode
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.{any, argThat}
import org.mockito.Mockito.{verify, when}
import pages.organisation.*
import play.api.data.Form
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.organisation.HaveTradingNameView

import scala.concurrent.Future

class HaveTradingNameControllerSpec extends SpecBase {

  val formProvider        = new GenericYesNoPageFormProvider()
  val form: Form[Boolean] = formProvider("haveTradingName.error.required")

  lazy val haveTradingNameRoute: String =
    controllers.organisation.routes.HaveTradingNameController.onPageLoad(NormalMode).url

  def onwardRoute = Call("GET", "/foo")

  "HaveTradingName Controller" - {

    "must return OK and the correct view for a GET when an org name is present in user answers" in {

      val ua = emptyUserAnswers.withPage(OverwritableOrganisationName, testOrgName)

      val application = applicationBuilder(userAnswers = Some(ua)).build()

      running(application) {
        val request = FakeRequest(GET, haveTradingNameRoute)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[HaveTradingNameView]

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, testOrgName)(request, messages(application)).toString
      }
    }

    "must redirect to Some Information is Missing GET when an org name is NOT present in user answers" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, haveTradingNameRoute)
        val result  = route(application, request).value

        status(result)               mustEqual SEE_OTHER
        redirectLocation(result).get mustEqual controllers.routes.InformationMissingController.onPageLoad().url
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = emptyUserAnswers
        .withPage(HaveTradingNamePage, true)
        .withPage(OverwritableOrganisationName, testOrgName)

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, haveTradingNameRoute)
        val view    = application.injector.instanceOf[HaveTradingNameView]
        val result  = route(application, request).value

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(form.fill(true), NormalMode, testOrgName)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect via Navigator and NOT clear data when answer is true" in {
      val userAnswers = emptyUserAnswers
        .withPage(TradingNamePage, testTradingName)

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute))
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, haveTradingNameRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url

        verify(mockSessionRepository).set(argThat { ua =>
          ua.get(HaveTradingNamePage).contains(true) &&
          ua.get(TradingNamePage).isDefined
        })
      }
    }

    "must redirect via Navigator and clear data when answer is false" in {
      val userAnswers = emptyUserAnswers
        .withPage(TradingNamePage, testTradingName)

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute))
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, haveTradingNameRoute)
            .withFormUrlEncodedBody(("value", "false"))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url

        verify(mockSessionRepository).set(argThat { ua =>
          ua.get(HaveTradingNamePage).contains(false) &&
          ua.get(TradingNamePage).isEmpty
        })
      }
    }

    "must return a Bad Request and errors when invalid data is submitted and an org name is present in user answers" in {

      val ua = emptyUserAnswers.withPage(OverwritableOrganisationName, testOrgName)

      val application = applicationBuilder(userAnswers = Some(ua)).build()

      running(application) {
        val request =
          FakeRequest(POST, haveTradingNameRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))
        val view      = application.injector.instanceOf[HaveTradingNameView]
        val result    = route(application, request).value

        status(result)          mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, testOrgName)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Some Information is Missing when invalid data is submitted and an org name is NOT present in user answers" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, haveTradingNameRoute)
            .withFormUrlEncodedBody(("value", ""))

        val result = route(application, request).value

        status(result)               mustEqual SEE_OTHER
        redirectLocation(result).get mustEqual controllers.routes.InformationMissingController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, haveTradingNameRoute)
        val result  = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, haveTradingNameRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
