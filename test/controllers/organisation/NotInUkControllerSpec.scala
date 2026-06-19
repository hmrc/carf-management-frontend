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
import pages.organisation.{OverwritableOrganisationName, TradingNamePage}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.organisation.NotInUkView

class NotInUkControllerSpec extends SpecBase {

  lazy val notInUkRoute: String =
    controllers.organisation.routes.NotInUkController.onPageLoad().url

  "NotInUk Controller" - {

    "must return OK and the correct view when trading name is present in user answers" in {

      val ua = emptyUserAnswers.withPage(TradingNamePage, "Test Trading Name")

      val application = applicationBuilder(userAnswers = Some(ua)).build()

      running(application) {
        val request = FakeRequest(GET, notInUkRoute)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[NotInUkView]

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view("Test Trading Name")(request, messages(application)).toString
      }
    }

    "must return OK and the correct view when only organisation name is present in user answers" in {

      val ua = emptyUserAnswers.withPage(OverwritableOrganisationName, testOrgName)

      val application = applicationBuilder(userAnswers = Some(ua)).build()

      running(application) {
        val request = FakeRequest(GET, notInUkRoute)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[NotInUkView]

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(testOrgName)(request, messages(application)).toString
      }
    }

    "must prefer trading name over organisation name when both are present" in {

      val ua = emptyUserAnswers
        .withPage(TradingNamePage, "Test Trading Name")
        .withPage(OverwritableOrganisationName, testOrgName)

      val application = applicationBuilder(userAnswers = Some(ua)).build()

      running(application) {
        val request = FakeRequest(GET, notInUkRoute)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[NotInUkView]

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view("Test Trading Name")(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery when no RCASP name is found in user answers" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, notInUkRoute)
        val result  = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, notInUkRoute)
        val result  = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
