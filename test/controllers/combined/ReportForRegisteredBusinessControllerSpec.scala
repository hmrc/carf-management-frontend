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
import pages.combined.ReportForRegisteredBusinessPage
import pages.organisation.OverwritableOrganisationName
import play.api.data.Form
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.combined.ReportForRegisteredBusinessView

import scala.concurrent.Future

class ReportForRegisteredBusinessControllerSpec extends SpecBase {

  def onwardRoute = Call("GET", "/foo")

  val formProvider        = new GenericYesNoPageFormProvider()
  val form: Form[Boolean] = formProvider("reportForRegisteredBusiness.error.required")

  lazy val routeUnderTest: String =
    controllers.combined.routes.ReportForRegisteredBusinessController.onPageLoad(NormalMode).url

  "ReportForRegisteredBusiness Controller" - {

    "must return OK and the correct view for a GET when ct auto matched is true and org name is present" in {
      val ua =
        emptyUserAnswers
          .copy(isCtAutoMatched = true)
          .withPage(OverwritableOrganisationName, testOrgName)

      val application = applicationBuilder(userAnswers = Some(ua)).build()

      running(application) {
        val request = FakeRequest(GET, routeUnderTest)

        val result = route(application, request).value
        val view   = application.injector.instanceOf[ReportForRegisteredBusinessView]

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, testOrgName)(request, messages(application)).toString
      }
    }

    "must redirect to OrganisationOrIndividual page when user is not ct auto matched" in {
      val ua =
        emptyUserAnswers
          .copy(isCtAutoMatched = false)
          .withPage(OverwritableOrganisationName, testOrgName)

      val application =
        applicationBuilder(userAnswers = Some(ua), affinityGroup = uk.gov.hmrc.auth.core.AffinityGroup.Organisation)
          .build()

      running(application) {
        val request = FakeRequest(GET, routeUnderTest)

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.combined.routes.OrganisationOrIndividualController
          .onPageLoad(NormalMode)
          .url
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {
      val userAnswers =
        emptyUserAnswers
          .copy(isCtAutoMatched = true)
          .withPage(OverwritableOrganisationName, testOrgName)
          .withPage(ReportForRegisteredBusinessPage, true)

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routeUnderTest)
        val view    = application.injector.instanceOf[ReportForRegisteredBusinessView]

        val result = route(application, request).value

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(form.fill(true), NormalMode, testOrgName)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(
          userAnswers = Some(
            emptyUserAnswers
              .copy(isCtAutoMatched = true)
              .withPage(OverwritableOrganisationName, testOrgName)
          )
        ).overrides(
          bind[Navigator].toInstance(new FakeNavigator(onwardRoute))
        ).build()

      running(application) {
        val request =
          FakeRequest(POST, routeUnderTest)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted and org name is present" in {
      val ua =
        emptyUserAnswers
          .copy(isCtAutoMatched = true)
          .withPage(OverwritableOrganisationName, testOrgName)

      val application = applicationBuilder(userAnswers = Some(ua)).build()

      running(application) {
        val request =
          FakeRequest(POST, routeUnderTest)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[ReportForRegisteredBusinessView]

        val result = route(application, request).value

        status(result)          mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, testOrgName)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Some Information is Missing when invalid data is submitted and org name is not present" in {
      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers.copy(isCtAutoMatched = true))).build()

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

    "must redirect to OrganisationOrIndividual page for a GET when user is not ct auto matched" in {
      val ua =
        emptyUserAnswers
          .copy(isCtAutoMatched = false)
          .withPage(OverwritableOrganisationName, testOrgName)

      val application =
        applicationBuilder(userAnswers = Some(ua), affinityGroup = uk.gov.hmrc.auth.core.AffinityGroup.Organisation)
          .build()

      running(application) {
        val request = FakeRequest(GET, routeUnderTest)

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.combined.routes.OrganisationOrIndividualController
          .onPageLoad(NormalMode)
          .url
      }
    }

    "must redirect to OrganisationOrIndividual page for a POST when user is not ct auto matched" in {
      val ua =
        emptyUserAnswers
          .copy(isCtAutoMatched = false)
          .withPage(OverwritableOrganisationName, testOrgName)

      val application =
        applicationBuilder(userAnswers = Some(ua), affinityGroup = uk.gov.hmrc.auth.core.AffinityGroup.Organisation)
          .build()

      running(application) {
        val request =
          FakeRequest(POST, routeUnderTest)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.combined.routes.OrganisationOrIndividualController
          .onPageLoad(NormalMode)
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

    "must redirect to Some Information is Missing when org name is not present on GET" in {
      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers.copy(isCtAutoMatched = true))).build()

      running(application) {
        val request = FakeRequest(GET, routeUnderTest)

        val result = route(application, request).value

        status(result)               mustEqual SEE_OTHER
        redirectLocation(result).get mustEqual controllers.routes.PlaceholderController
          .onPageLoad("Should redirect to Some Information is Missing Page (CARF-293)")
          .url
      }
    }
  }
}
