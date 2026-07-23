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

package controllers.changeDetails

import base.SpecBase
import models.OrganisationOrIndividual.{Individual, Organisation}
import pages.SubmissionSucceededPage
import pages.combined.OrganisationOrIndividualPage
import pages.individual.IndividualNamePage
import pages.organisation.OverwritableOrganisationName
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.changeDetails.RcaspUpdatedConfirmationView

class RcaspUpdatedConfirmationControllerSpec extends SpecBase {

  lazy val rcaspUpdatedRoute: String = routes.RcaspUpdatedConfirmationController.onPageLoad().url

  "RcaspUpdatedConfirmation Controller" - {

    "must return OK and the correct view for a GET when an organisation name is present and SubmissionSucceededPage is true" in {
      val userAnswers = emptyUserAnswers
        .withPage(OrganisationOrIndividualPage, Organisation)
        .withPage(OverwritableOrganisationName, testOrgName)
        .withPage(SubmissionSucceededPage, true)

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, rcaspUpdatedRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[RcaspUpdatedConfirmationView]

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(testOrgName)(request, messages(application)).toString

        contentAsString(result) must include(controllers.routes.YourRcaspsController.onPageLoad().url)
        contentAsString(result) must include("""id="manage-your-rcasps-link"""")
        contentAsString(result) must include(controllers.home.routes.HomePageController.onPageLoad().url)
        contentAsString(result) must include("""id="manage-your-cryptoassets-report-link"""")
      }
    }

    "must return OK and the correct view for a GET when an individual name is present and SubmissionSucceededPage is true" in {
      val userAnswers = emptyUserAnswers
        .withPage(OrganisationOrIndividualPage, Individual)
        .withPage(IndividualNamePage, testIndividualName)
        .withPage(SubmissionSucceededPage, true)

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, rcaspUpdatedRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[RcaspUpdatedConfirmationView]

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(testIndividualName.fullName)(request, messages(application)).toString

        contentAsString(result) must include(controllers.routes.YourRcaspsController.onPageLoad().url)
        contentAsString(result) must include("""id="manage-your-rcasps-link"""")
        contentAsString(result) must include(controllers.home.routes.HomePageController.onPageLoad().url)
        contentAsString(result) must include("""id="manage-your-cryptoassets-report-link"""")
      }
    }

    "must redirect to Journey Recovery for a GET when a name is present but SubmissionSucceededPage is false" in {
      val userAnswers = emptyUserAnswers
        .withPage(OrganisationOrIndividualPage, Organisation)
        .withPage(OverwritableOrganisationName, testOrgName)
        .withPage(SubmissionSucceededPage, false)

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, rcaspUpdatedRoute)

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a GET when a name is present but SubmissionSucceededPage is missing" in {
      val userAnswers = emptyUserAnswers
        .withPage(OrganisationOrIndividualPage, Organisation)
        .withPage(OverwritableOrganisationName, testOrgName)

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, rcaspUpdatedRoute)

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a GET when SubmissionSucceededPage is true but no name is in user answers" in {
      val userAnswers = emptyUserAnswers.withPage(SubmissionSucceededPage, true)

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, rcaspUpdatedRoute)

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, rcaspUpdatedRoute)
        val result  = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
