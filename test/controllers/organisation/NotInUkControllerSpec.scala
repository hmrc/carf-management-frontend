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
import models.BusinessDetails
import models.responses.AddressRegistrationResponse
import pages.organisation.CachedBusinessDetailsPage
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.organisation.NotInUkView

class NotInUkControllerSpec extends SpecBase {

  lazy val notInUkRoute: String =
    controllers.organisation.routes.NotInUkController.onPageLoad().url

  val businessDetails: BusinessDetails =
    BusinessDetails(
      name = "Test Business Ltd",
      address = AddressRegistrationResponse(
        addressLine1 = "1 Test Street",
        addressLine2 = Some("Testville"),
        addressLine3 = None,
        addressLine4 = None,
        postalCode = Some("TE1 1ST"),
        countryCode = "US"
      ),
      countryName = "United States"
    )

  "NotInUk Controller" - {

    "must return OK and the correct view when cached business details are present and have a non-GB country code" in {

      val ua = emptyUserAnswers.withPage(CachedBusinessDetailsPage, businessDetails)

      val application = applicationBuilder(userAnswers = Some(ua)).build()

      running(application) {
        val request = FakeRequest(GET, notInUkRoute)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[NotInUkView]

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(businessDetails.name)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery when no cached business details are found" in {

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
