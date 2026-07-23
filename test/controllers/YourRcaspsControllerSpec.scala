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
import connectors.RcaspConnector
import forms.GenericYesNoPageFormProvider
import models.NormalMode
import models.errors.ApiError.JsonValidationError
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, times, verify, when}
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import types.ResultT
import viewmodels.YourRcaspsListWithActionsHelper
import views.html.YourRcaspsView

class YourRcaspsControllerSpec extends SpecBase {

  val formProvider        = new GenericYesNoPageFormProvider()
  val form: Form[Boolean] = formProvider("yourRcasps.error.required")

  val mockRcaspConnector: RcaspConnector = mock[RcaspConnector]

  lazy val yourRcaspsRoute: String = controllers.routes.YourRcaspsController.onPageLoad().url

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockRcaspConnector)
  }

  "YourRcasps Controller" - {

    "must return OK and the correct view for a GET if the Rcasp connector is called successfully" in {
      when(mockRcaspConnector.viewRcasp(any())(any(), any()))
        .thenReturn(ResultT.fromValue(List(organisationRcaspDetailsResponse)))

      val application = applicationBuilder(userAnswers = None)
        .overrides(bind[RcaspConnector].toInstance(mockRcaspConnector))
        .build()

      running(application) {
        val request = FakeRequest(GET, yourRcaspsRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[YourRcaspsView]

        val listWithItems =
          YourRcaspsListWithActionsHelper.getYourRcaspsRows(List(organisationRcaspDetailsResponse))(
            messages(application)
          )

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(form, listWithItems)(request, messages(application)).toString

        verify(mockRcaspConnector, times(1)).viewRcasp(any())(any(), any())
      }
    }

    "must redirect to Journey Recovery for a GET if the Rcasp connector returns an error" in {
      when(mockRcaspConnector.viewRcasp(any())(any(), any())).thenReturn(ResultT.fromError(JsonValidationError))

      val application = applicationBuilder(userAnswers = None)
        .overrides(bind[RcaspConnector].toInstance(mockRcaspConnector))
        .build()

      running(application) {
        val request = FakeRequest(GET, yourRcaspsRoute)

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

        verify(mockRcaspConnector, times(1)).viewRcasp(any())(any(), any())
      }
    }

    "must redirect to the next page when valid data is submitted" - {
      "when the answer is Yes" in {
        val application = applicationBuilder(userAnswers = None)
          .overrides(bind[RcaspConnector].toInstance(mockRcaspConnector))
          .build()

        running(application) {
          val request =
            FakeRequest(POST, yourRcaspsRoute)
              .withFormUrlEncodedBody(("value", "true"))

          val result = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.RoutingController.onPageLoad(NormalMode).url

          verify(mockRcaspConnector, times(0)).viewRcasp(any())(any(), any())
        }
      }

      "when the answer is No" in {
        val application = applicationBuilder(userAnswers = None)
          .overrides(bind[RcaspConnector].toInstance(mockRcaspConnector))
          .build()

        running(application) {
          val request =
            FakeRequest(POST, yourRcaspsRoute)
              .withFormUrlEncodedBody(("value", "false"))

          val result = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.home.routes.HomePageController.onPageLoad().url

          verify(mockRcaspConnector, times(0)).viewRcasp(any())(any(), any())
        }
      }
    }

    "must return a Bad Request and errors when invalid data is submitted and the Rcasp connector is called successfully" in {
      when(mockRcaspConnector.viewRcasp(any())(any(), any()))
        .thenReturn(ResultT.fromValue(List(organisationRcaspDetailsResponse)))

      val application = applicationBuilder(userAnswers = None)
        .overrides(bind[RcaspConnector].toInstance(mockRcaspConnector))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, yourRcaspsRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[YourRcaspsView]

        val result = route(application, request).value

        val listWithItems =
          YourRcaspsListWithActionsHelper.getYourRcaspsRows(List(organisationRcaspDetailsResponse))(
            messages(application)
          )

        status(result)          mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, listWithItems)(request, messages(application)).toString

        verify(mockRcaspConnector, times(1)).viewRcasp(any())(any(), any())
      }
    }

    "must redirect to Journey Recovery when invalid data is submitted and the Rcasp connector returns an error" in {
      when(mockRcaspConnector.viewRcasp(any())(any(), any())).thenReturn(ResultT.fromError(JsonValidationError))

      val application = applicationBuilder(userAnswers = None)
        .overrides(bind[RcaspConnector].toInstance(mockRcaspConnector))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, yourRcaspsRoute)
            .withFormUrlEncodedBody(("value", ""))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

        verify(mockRcaspConnector, times(1)).viewRcasp(any())(any(), any())
      }
    }
  }
}
