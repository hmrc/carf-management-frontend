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
import models.OrganisationOrIndividual.Individual
import pages.RcaspIdPage
import pages.combined.OrganisationOrIndividualPage
import pages.individual.IndividualNamePage
import pages.organisation.OverwritableOrganisationName
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.RcaspAddedConfirmationView

class RcaspAddedConfirmationControllerSpec extends SpecBase {

  lazy val rcaspAddedRoute: String = routes.RcaspAddedConfirmationController.onPageLoad().url

  "RcaspAddedConfirmation Controller" - {

    "must return OK and the correct view for a GET when an organisation name and rcaspId are present in user answers" in {

      val userAnswers = emptyUserAnswers
        .withPage(OverwritableOrganisationName, testOrgName)
        .withPage(RcaspIdPage, rcaspId)

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, rcaspAddedRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[RcaspAddedConfirmationView]

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(rcaspId, testOrgName)(
          request,
          messages(application)
        ).toString
      }
    }

    "must return OK and the correct view for a GET when an individual name and rcaspId are present in user answers" in {

      val userAnswers = emptyUserAnswers
        .withPage(OrganisationOrIndividualPage, Individual)
        .withPage(IndividualNamePage, testIndividualName)
        .withPage(RcaspIdPage, rcaspId)

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, rcaspAddedRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[RcaspAddedConfirmationView]

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(rcaspId, testIndividualName.fullName)(
          request,
          messages(application)
        ).toString
      }
    }

    "must include a link to the your-rcasps page" in {

      val userAnswers = emptyUserAnswers
        .withPage(OverwritableOrganisationName, testOrgName)
        .withPage(RcaspIdPage, rcaspId)

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, rcaspAddedRoute)
        val result  = route(application, request).value

        status(result)     mustEqual OK
        contentAsString(result) must include(routes.YourRcaspsController.onPageLoad().url)
        contentAsString(result) must include("""id="manage-your-rcasps-link"""")
      }
    }

    "must include a link to the home page" in {

      val userAnswers = emptyUserAnswers
        .withPage(OverwritableOrganisationName, testOrgName)
        .withPage(RcaspIdPage, rcaspId)

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, rcaspAddedRoute)
        val result  = route(application, request).value

        status(result)     mustEqual OK
        contentAsString(result) must include(controllers.home.routes.HomePageController.onPageLoad().url)
        contentAsString(result) must include("""id="manage-your-cryptoassets-report-link"""")
      }
    }

    "must redirect to Journey Recovery for a GET when a name is present but rcaspId is missing" in {

      val userAnswers = emptyUserAnswers.withPage(OverwritableOrganisationName, testOrgName)

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, rcaspAddedRoute)

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a GET when an rcaspId is present but no name is in user answers" in {

      val userAnswers = emptyUserAnswers.withPage(RcaspIdPage, rcaspId)

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, rcaspAddedRoute)

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, rcaspAddedRoute)

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
