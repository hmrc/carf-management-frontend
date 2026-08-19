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
import pages.changeDetails.ChangeRcaspCachedDetails
import play.api.test.FakeRequest
import play.api.test.Helpers.*

class EndOfJourneyRoutingControllerSpec extends SpecBase {

  lazy val endOfJourneyRoutingRoute: String = routes.EndOfJourneyRoutingController.onPageLoad().url

  "EndOfJourneyRouting Controller" - {

    "must redirect to CheckDetailsController when ChangeRcaspCachedDetails is absent and rcaspIsRegisteredBusiness = false" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, endOfJourneyRoutingRoute)
        val result  = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.CheckDetailsController.onPageLoad.url
      }
    }

    "must redirect to RegisteredBusinessCheckDetailsController when ChangeRcaspCachedDetails is absent and rcaspIsRegisteredBusiness = true" in {
      val userAnswers = emptyUserAnswers.copy(rcaspIsRegisteredBusiness = true)

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, endOfJourneyRoutingRoute)
        val result  = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          controllers.organisation.routes.RegisteredBusinessCheckDetailsController.onPageLoad.url
      }
    }

    "must redirect to ChangeDetailsRoutingController when ChangeRcaspCachedDetails is present" in {
      val userAnswers = emptyUserAnswers.withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate)

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, endOfJourneyRoutingRoute)
        val result  = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          controllers.changeDetails.routes.ChangeDetailsRoutingController.onPageLoad(rcaspId).url
      }
    }

    "must redirect to Journey Recovery for a GET when user answers is missing" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, endOfJourneyRoutingRoute)

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }

}
