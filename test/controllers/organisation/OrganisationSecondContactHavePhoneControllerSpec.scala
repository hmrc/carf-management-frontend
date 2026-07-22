/*
 * Copyright 2025 HM Revenue & Customs
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
import models.{ChangeMode, NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.{any, argThat}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.changeDetails.ChangeRcaspCachedDetails
import pages.organisation.{OrganisationSecondContactHavePhonePage, OrganisationSecondContactNamePage, OrganisationSecondContactPhoneNumberPage, OverwritableOrganisationName}
import play.api.data.Form
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.organisation.OrganisationSecondContactHavePhoneView

import java.time.Clock
import scala.concurrent.Future

class OrganisationSecondContactHavePhoneControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute                                        = Call("GET", "/foo")
  lazy val secondContactHavePhoneRoute: String           =
    controllers.organisation.routes.OrganisationSecondContactHavePhoneController.onPageLoad(NormalMode).url
  lazy val secondContactHavePhoneRouteChangeMode: String =
    controllers.organisation.routes.OrganisationSecondContactHavePhoneController.onPageLoad(ChangeMode).url

  val formProvider: GenericYesNoPageFormProvider =
    new GenericYesNoPageFormProvider()
  val form: Form[Boolean]                        = formProvider("organisationSecondContactHavePhone.error.required")

  val secondNameTest: String       = "Second Contact Name"
  val organisationNameTest: String = "Organisation Name"

  val userAnswersWithSecondNameAndRcaspNameTest: UserAnswers =
    emptyUserAnswers
      .withPage(OrganisationSecondContactNamePage, secondNameTest)
      .withPage(OverwritableOrganisationName, organisationNameTest)

  "SecondContactHavePhone Controller" - {

    "normal mode" - {
      "must return OK and the correct view for a GET" in {
        val application = applicationBuilder(userAnswers = Some(userAnswersWithSecondNameAndRcaspNameTest)).build()
        running(application) {
          val request = FakeRequest(GET, secondContactHavePhoneRoute)
          val result  = route(application, request).value
          val view    = application.injector.instanceOf[OrganisationSecondContactHavePhoneView]
          status(result)          mustEqual OK
          contentAsString(result) mustEqual view(form, NormalMode, secondNameTest, organisationNameTest)(
            request,
            messages(application)
          ).toString
        }
      }

      "must populate the view correctly on a GET when the question has previously been answered" in {
        val userAnswers =
          userAnswersWithSecondNameAndRcaspNameTest.set(OrganisationSecondContactHavePhonePage, true).success.value
        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()
        running(application) {
          val request = FakeRequest(GET, secondContactHavePhoneRoute)
          val view    = application.injector.instanceOf[OrganisationSecondContactHavePhoneView]
          val result  = route(application, request).value
          status(result)          mustEqual OK
          contentAsString(result) mustEqual view(form.fill(true), NormalMode, secondNameTest, organisationNameTest)(
            request,
            messages(application)
          ).toString
        }
      }

      "must redirect to the next page and NOT clear data when valid data is submitted" in {
        val userAnswers = emptyUserAnswers
          .withPage(OrganisationSecondContactPhoneNumberPage, testPhone)

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        val application =
          applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(
              bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
              bind[Clock].toInstance(clock)
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, secondContactHavePhoneRoute)
              .withFormUrlEncodedBody(("value", "true"))

          val result = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url

          verify(mockSessionRepository).set(argThat { ua =>
            ua.get(OrganisationSecondContactHavePhonePage).contains(true) &&
            ua.get(OrganisationSecondContactPhoneNumberPage).isDefined
          })
        }
      }

      "must redirect to the next page and clear answers when answer is changed to false" in {
        val userAnswers = emptyUserAnswers
          .withPage(OrganisationSecondContactPhoneNumberPage, testPhone)

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        val application =
          applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(
              bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
              bind[Clock].toInstance(clock)
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, secondContactHavePhoneRoute)
              .withFormUrlEncodedBody(("value", "false"))

          val result = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url

          verify(mockSessionRepository).set(argThat { ua =>
            ua.get(OrganisationSecondContactHavePhonePage).contains(false) &&
            ua.get(OrganisationSecondContactPhoneNumberPage).isEmpty
          })
        }
      }

      "must return a Bad Request and errors when invalid data is submitted" in {
        val application = applicationBuilder(userAnswers = Some(userAnswersWithSecondNameAndRcaspNameTest)).build()
        running(application) {
          val request =
            FakeRequest(POST, secondContactHavePhoneRoute)
              .withFormUrlEncodedBody(("value", ""))

          val boundForm = form.bind(Map("value" -> ""))
          val view      = application.injector.instanceOf[OrganisationSecondContactHavePhoneView]
          val result    = route(application, request).value
          status(result)          mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm, NormalMode, secondNameTest, organisationNameTest)(
            request,
            messages(application)
          ).toString
        }
      }

      "must redirect to Journey Recovery for a GET if no existing data is found" in {
        val application = applicationBuilder(userAnswers = None).build()
        running(application) {
          val request = FakeRequest(GET, secondContactHavePhoneRoute)
          val result  = route(application, request).value
          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Some Information Is Missing for a GET if UserAnswers is not empty & OrganisationSecondContactNamePage is None" in {
        val ua = emptyUserAnswers.withPage(OrganisationSecondContactNamePage, secondNameTest)

        val application = applicationBuilder(userAnswers = Some(ua)).build()
        running(application) {
          val request = FakeRequest(GET, secondContactHavePhoneRoute)
          val result  = route(application, request).value
          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.InformationMissingController.onPageLoad().url
        }
      }

      "must redirect to Some Information Is Missing for a GET if UserAnswers is not empty & OverwritableOrganisationNamePage is None" in {
        val ua = emptyUserAnswers.withPage(OrganisationSecondContactNamePage, secondNameTest)

        val application = applicationBuilder(userAnswers = Some(ua)).build()
        running(application) {
          val request = FakeRequest(GET, secondContactHavePhoneRoute)
          val result  = route(application, request).value
          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.InformationMissingController.onPageLoad().url
        }
      }

      "redirect to Journey Recovery for a POST if no existing userAnswers data is found" in {
        val application = applicationBuilder(userAnswers = None).build()
        running(application) {
          val request =
            FakeRequest(POST, secondContactHavePhoneRoute)
              .withFormUrlEncodedBody(("value", "true"))
          val result  = route(application, request).value
          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "redirect to Some Information Is Missing for a POST if the form for OrganisationSecondContactNamePage has errors" in {
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
        running(application) {
          val request =
            FakeRequest(POST, secondContactHavePhoneRoute)
              .withFormUrlEncodedBody(("value", "invalid  Boolean"))
          val result  = route(application, request).value
          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.InformationMissingController.onPageLoad().url
        }
      }
    }

    "change mode" - {
      "must redirect to OrganisationSecondContactPhoneNumberController when answer is changed from false -> true" in {
        val userAnswers = emptyUserAnswers
          .withPage(OrganisationSecondContactHavePhonePage, false)

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        val application =
          applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(
              bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
              bind[Clock].toInstance(clock)
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, secondContactHavePhoneRouteChangeMode)
              .withFormUrlEncodedBody(("value", "true"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(
            result
          ).value        mustEqual controllers.organisation.routes.OrganisationSecondContactPhoneNumberController
            .onPageLoad(ChangeMode)
            .url

          verify(mockSessionRepository).set(argThat { ua =>
            ua.get(OrganisationSecondContactHavePhonePage).contains(true)
          })
        }
      }

      "must redirect ChangeDetailsRoutingController and clear data when answer is changed from true -> false" in {
        val userAnswers = emptyUserAnswers
          .withPage(OrganisationSecondContactHavePhonePage, true)
          .withPage(OrganisationSecondContactPhoneNumberPage, testPhone)
          .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate)

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        val application =
          applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(
              bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
              bind[Clock].toInstance(clock)
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, secondContactHavePhoneRouteChangeMode)
              .withFormUrlEncodedBody(("value", "false"))

          val result = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.changeDetails.routes.ChangeDetailsRoutingController
            .onPageLoad(rcaspId)
            .url

          verify(mockSessionRepository).set(argThat { ua =>
            ua.get(OrganisationSecondContactHavePhonePage).contains(false) &&
            ua.get(OrganisationSecondContactPhoneNumberPage).isEmpty
          })
        }
      }

      "must redirect ChangeDetailsRoutingController when answer is true and does not change" in {
        val userAnswers = emptyUserAnswers
          .withPage(OrganisationSecondContactHavePhonePage, true)
          .withPage(OrganisationSecondContactPhoneNumberPage, testPhone)
          .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate)

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        val application =
          applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(
              bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
              bind[Clock].toInstance(clock)
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, secondContactHavePhoneRouteChangeMode)
              .withFormUrlEncodedBody(("value", "true"))

          val result = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.changeDetails.routes.ChangeDetailsRoutingController
            .onPageLoad(rcaspId)
            .url

          verify(mockSessionRepository).set(argThat { ua =>
            ua.get(OrganisationSecondContactHavePhonePage).contains(true) &&
            ua.get(OrganisationSecondContactPhoneNumberPage).isDefined
          })
        }
      }

      "must redirect ChangeDetailsRoutingController when answer is false and does not change" in {
        val userAnswers = emptyUserAnswers
          .withPage(OrganisationSecondContactHavePhonePage, false)
          .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate)

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        val application =
          applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(
              bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
              bind[Clock].toInstance(clock)
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, secondContactHavePhoneRouteChangeMode)
              .withFormUrlEncodedBody(("value", "false"))

          val result = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.changeDetails.routes.ChangeDetailsRoutingController
            .onPageLoad(rcaspId)
            .url

          verify(mockSessionRepository).set(argThat { ua =>
            ua.get(OrganisationSecondContactHavePhonePage).contains(false)
          })
        }
      }

      "must redirect Journey recovery when ChangeRcaspCachedDetails is not in userAnswers" in {
        val userAnswers = emptyUserAnswers
          .withPage(OrganisationSecondContactHavePhonePage, false)

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        val application =
          applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(
              bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
              bind[Clock].toInstance(clock)
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, secondContactHavePhoneRouteChangeMode)
              .withFormUrlEncodedBody(("value", "false"))

          val result = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

          verify(mockSessionRepository).set(argThat { ua =>
            ua.get(OrganisationSecondContactHavePhonePage).contains(false)
          })
        }
      }
    }
  }
}
