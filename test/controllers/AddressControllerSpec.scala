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

package controllers

import base.SpecBase
import forms.AddressFormProvider
import models.OrganisationOrIndividual.{Individual, Organisation}
import models.{AddressUk, NormalMode}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.{any, argThat}
import org.mockito.Mockito.{times, verify, when}
import pages.combined.OrganisationOrIndividualPage
import pages.individual.IndividualNamePage
import pages.organisation.OverwritableOrganisationName
import pages.{AddressPagePrePop, AddressUPRNUserAnswers}
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.AddressView

import scala.concurrent.Future

class AddressControllerSpec extends SpecBase {

  def onwardRoute = Call("GET", "/foo")

  private val formProvider = new AddressFormProvider()
  private val form         = formProvider()

  private lazy val addressRoute =
    controllers.routes.AddressController.onPageLoad(NormalMode).url

  inline final val addressRegex     = """^[a-zA-Z0-9 \.&`\-\'\^]*$"""
  inline final val addressMaxLength = 35

  "Address Controller" - {
    "must return OK and the correct view for a GET when IndividualNamePage is present" in {

      val userAnswersWithName =
        emptyUserAnswers
          .withPage(OrganisationOrIndividualPage, Individual)
          .withPage(IndividualNamePage, testIndividualName)

      val application = applicationBuilder(userAnswers = Some(userAnswersWithName))
        .build()

      running(application) {
        val request = FakeRequest(GET, addressRoute)
        val view    = application.injector.instanceOf[AddressView]
        val result  = route(application, request).value

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, testIndividualName.fullName)(
          request,
          messages(application)
        ).toString
      }
    }

    "must return OK and the correct view for a GET when OverwritableOrganisationName is present" in {

      val userAnswersWithName =
        emptyUserAnswers
          .withPage(OrganisationOrIndividualPage, Organisation)
          .withPage(OverwritableOrganisationName, testOrgName)

      val application = applicationBuilder(userAnswers = Some(userAnswersWithName))
        .build()

      running(application) {
        val request = FakeRequest(GET, addressRoute)
        val view    = application.injector.instanceOf[AddressView]
        val result  = route(application, request).value

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, testOrgName)(
          request,
          messages(application)
        ).toString
      }
    }

    "must return Some Information Is Missing for a GET when neither OverwritableOrganisationName or IndividualNamePage is present" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .build()

      running(application) {
        val request = FakeRequest(GET, addressRoute)
        val result  = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.InformationMissingController.onPageLoad().url
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered via pre-pop" in {
      val userAnswers = emptyUserAnswers
        .withPage(AddressPagePrePop, testAddressUk)
        .withPage(OrganisationOrIndividualPage, Organisation)
        .withPage(OverwritableOrganisationName, testOrgName)

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .build()

      running(application) {
        val request = FakeRequest(GET, addressRoute)

        val view = application.injector.instanceOf[AddressView]

        val result = route(application, request).value

        status(result) mustEqual OK

        contentAsString(result) mustEqual view(form.fill(testAddressUk), NormalMode, testOrgName)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val userAnswers = emptyUserAnswers
        .withPage(AddressUPRNUserAnswers, testUPRN)
        .withPage(OrganisationOrIndividualPage, Organisation)
        .withPage(OverwritableOrganisationName, testOrgName)

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[Navigator].toInstance(new FakeNavigator(onwardRoute)))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, addressRoute)
            .withFormUrlEncodedBody(
              "addressLine1" -> "value 1",
              "addressLine2" -> "value 2",
              "county"       -> "west testshire",
              "townOrCity"   -> "test town",
              "postcode"     -> testNonCdPostcode
            )

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
        verify(mockSessionRepository, times(1)).set(
          argThat(_.get(AddressUPRNUserAnswers).isEmpty)
        )
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      val userAnswers = emptyUserAnswers
        .withPage(OrganisationOrIndividualPage, Organisation)
        .withPage(OverwritableOrganisationName, testOrgName)

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, addressRoute)
            .withFormUrlEncodedBody(("addressLine1", ""))

        val boundForm = form.bind(Map("addressLine1" -> ""))
        val view      = application.injector.instanceOf[AddressView]
        val result    = route(application, request).value

        status(result)          mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, testOrgName)(
          request,
          messages(application)
        ).toString
      }
    }

    "must return Some Information Is Missing when invalid data is submitted and neither OverwritableOrganisationName or IndividualNamePage is present" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, addressRoute)
            .withFormUrlEncodedBody(("addressLine1", ""))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.InformationMissingController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, addressRoute)

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, addressRoute)
            .withFormUrlEncodedBody(("addressLine1", "value 1"), ("addressLine2", "value 2"))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
