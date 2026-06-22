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
import forms.FindAddressFormProvider
import generators.Generators
import models.errors.ApiError.BadRequestError
import models.individual.IndividualName
import models.responses.{AddressRecord, AddressResponse, CountryRecord}
import models.{AddressAndUPRN, FindAddress, NormalMode, UserAnswers}
import org.mockito.ArgumentMatchers.{any, argThat, eq as eqTo}
import org.mockito.Mockito.*
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.individual.IndividualNamePage
import pages.organisation.OverwritableOrganisationName
import pages.{AddressUPRNUserAnswers, FindAddressAdditionalCallUa, FindAddressPage}
import play.api.data.Form
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.AddressLookupService
import views.html.FindAddressView

import scala.concurrent.Future

class FindAddressControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach with Generators {

  val formProvider: FindAddressFormProvider          = new FindAddressFormProvider()
  val form: Form[FindAddress]                        = formProvider()
  val mockAddressLookupService: AddressLookupService = mock[AddressLookupService]

  lazy val findAddressRoute: String =
    controllers.routes.FindAddressController.onPageLoad(NormalMode).url

  override def beforeEach(): Unit = {
    reset(mockAddressLookupService)
    reset(mockSessionRepository)
    super.beforeEach()
  }

  val searchByPostcodeValidResponse: Seq[AddressResponse] = Seq(
    AddressResponse(
      id = "Test-Id",
      uprn = 123456,
      address = AddressRecord(
        lines = List("Address-Line1", "Address-Line2"),
        town = "Bristol",
        postcode = validGBOnlyNonCDPostcodes.sample.value,
        country = CountryRecord(code = "UK", name = "United Kingdom")
      )
    )
  )

  val userAnswers = UserAnswers(
    id = userAnswersId,
    data = Json.obj(
      FindAddressPage.toString -> Json.obj(
        "postcode"             -> "AA1 1AA",
        "propertyNameOrNumber" -> "value 2"
      )
    )
  )

  val testName = "james"

  private def expectedManualUrl: String =
    controllers.routes.PlaceholderController.onPageLoad("Should nav to /address (CARF-203)").url

  "FindAddress Controller" - {

    "must return OK and the correct view for a GET when IndividualNamePage is present" in {

      val userAnswersWithName = emptyUserAnswers.withPage(IndividualNamePage, testIndividualName)

      val application = applicationBuilder(userAnswers = Some(userAnswersWithName)).build()

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      running(application) {
        val request = FakeRequest(GET, findAddressRoute)

        val view = application.injector.instanceOf[FindAddressView]

        val result = route(application, request).value

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, testIndividualName.fullName, expectedManualUrl)(
          request,
          messages(application)
        ).toString
      }
    }

    "must return OK and the correct view for a GET when OverwritableOrganisationName is present" in {

      val userAnswersWithName = emptyUserAnswers.withPage(OverwritableOrganisationName, testName)

      val application = applicationBuilder(userAnswers = Some(userAnswersWithName)).build()

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      running(application) {
        val request = FakeRequest(GET, findAddressRoute)

        val view = application.injector.instanceOf[FindAddressView]

        val result = route(application, request).value

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, testName, expectedManualUrl)(
          request,
          messages(application)
        ).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val application =
        applicationBuilder(userAnswers = Some(userAnswers.withPage(OverwritableOrganisationName, testName))).build()

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      running(application) {
        val request = FakeRequest(GET, findAddressRoute)

        val view = application.injector.instanceOf[FindAddressView]

        val result = route(application, request).value

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(
          form.fill(FindAddress("AA1 1AA", Some("value 2"))),
          NormalMode,
          testName,
          expectedManualUrl
        )(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to the next page when postcode has returned one address" in {

      val onwardRouteOneAddress =
        controllers.routes.PlaceholderController.onPageLoad("Should nav to /review-address (CARF-201)")

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockAddressLookupService.postcodeSearch(eqTo("TE1 1ST"), eqTo(Some("value 2")))(any(), any()))
        .thenReturn(
          Future.successful(
            Right(Seq(AddressAndUPRN(testAddressUk, testUPRN)), false)
          )
        )

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers.withPage(OverwritableOrganisationName, testName)))
          .overrides(
            bind[AddressLookupService].toInstance(mockAddressLookupService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, findAddressRoute)
            .withFormUrlEncodedBody(("postcode", "TE1 1ST"), ("propertyNameOrNumber", "value 2"))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRouteOneAddress.url
        verify(mockAddressLookupService, times(1)).postcodeSearch(eqTo("TE1 1ST"), eqTo(Some("value 2")))(any(), any())
        verify(mockSessionRepository, times(1)).set(
          argThat(_.get(AddressUPRNUserAnswers).get == testUPRN)
        )
      }
    }

    "must redirect to the next page when postcode has returned more than one address" in {

      val onwardRouteMultipleAddresses =
        controllers.routes.PlaceholderController.onPageLoad("Should nav to /choose-address (CARF-201)")

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockAddressLookupService.postcodeSearch(eqTo("TE1 1ST"), eqTo(None))(any(), any()))
        .thenReturn(
          Future.successful(
            Right(
              Seq(
                AddressAndUPRN(testAddressUk, testUPRN),
                AddressAndUPRN(testAddressUk, testUPRN),
                AddressAndUPRN(testAddressUk, testUPRN)
              ),
              false
            )
          )
        )

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers.withPage(OverwritableOrganisationName, testName)))
          .overrides(
            bind[AddressLookupService].toInstance(mockAddressLookupService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, findAddressRoute)
            .withFormUrlEncodedBody(("postcode", "TE1 1ST"))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRouteMultipleAddresses.url
        verify(mockAddressLookupService, times(1)).postcodeSearch(eqTo("TE1 1ST"), eqTo(None))(any(), any())
      }
    }

    "must redirect to the next page when postcode has returned more than one address and retry has happened" in {

      val onwardRouteMultipleAddresses =
        controllers.routes.PlaceholderController.onPageLoad("Should nav to /choose-address (CARF-201)")

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockAddressLookupService.postcodeSearch(eqTo("TE1 1ST"), eqTo(None))(any(), any()))
        .thenReturn(
          Future.successful(
            Right(
              Seq(
                AddressAndUPRN(testAddressUk, testUPRN),
                AddressAndUPRN(testAddressUk, testUPRN),
                AddressAndUPRN(testAddressUk, testUPRN)
              ),
              true
            )
          )
        )

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers.withPage(OverwritableOrganisationName, testName)))
          .overrides(
            bind[AddressLookupService].toInstance(mockAddressLookupService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, findAddressRoute)
            .withFormUrlEncodedBody(("postcode", "TE1 1ST"))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRouteMultipleAddresses.url
        verify(mockAddressLookupService, times(1)).postcodeSearch(eqTo("TE1 1ST"), eqTo(None))(any(), any())
        verify(mockSessionRepository, times(1)).set(any())
        verify(mockSessionRepository).set(argThat(_.get(FindAddressAdditionalCallUa).isDefined))
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val userAnswersWithName = emptyUserAnswers.withPage(OverwritableOrganisationName, testName)

      val application = applicationBuilder(userAnswers = Some(userAnswersWithName)).build()

      running(application) {
        val request =
          FakeRequest(POST, findAddressRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[FindAddressView]

        val result = route(application, request).value

        status(result)          mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, testName, expectedManualUrl)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Some Information Is Missing for a GET if no available name is found" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, findAddressRoute)

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.InformationMissingController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, findAddressRoute)

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Some Information Is Missing for a POST if no available name is found" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, findAddressRoute)
            .withFormUrlEncodedBody(("postcode", "value 1"), ("propertyNameOrNumber", "value 2"))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.InformationMissingController.onPageLoad().url
      }
    }

    "must redirect to Some Information Is Missing for a POST if no available name is found and address lookup found no addresses" in {

      when(mockAddressLookupService.postcodeSearch(eqTo("TE1 1ST"), eqTo(None))(any(), any()))
        .thenReturn(
          Future.successful(
            Right((Nil, false))
          )
        )

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[AddressLookupService].toInstance(mockAddressLookupService)
        )
        .build()

      running(application) {
        val request =
          FakeRequest(POST, findAddressRoute)
            .withFormUrlEncodedBody(("postcode", "TE1 1ST"))

        val view = application.injector.instanceOf[FindAddressView]

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.InformationMissingController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, findAddressRoute)
            .withFormUrlEncodedBody(("postcode", "value 1"), ("propertyNameOrNumber", "value 2"))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must return Bad Request with error when postcode search returns no addresses" in {

      when(mockAddressLookupService.postcodeSearch(eqTo("TE1 1ST"), eqTo(None))(any(), any()))
        .thenReturn(
          Future.successful(
            Right((Nil, false))
          )
        )

      val userAnswersWithName = emptyUserAnswers.withPage(OverwritableOrganisationName, testName)

      val application = applicationBuilder(userAnswers = Some(userAnswersWithName))
        .overrides(
          bind[AddressLookupService].toInstance(mockAddressLookupService)
        )
        .build()

      running(application) {
        val request =
          FakeRequest(POST, findAddressRoute)
            .withFormUrlEncodedBody(("postcode", "TE1 1ST"))

        val view = application.injector.instanceOf[FindAddressView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST

        val boundForm     = form.bind(Map("postcode" -> "TE1 1ST"))
        val formWithError = boundForm.withError("postcode", "findAddress.postcode.error.notFound")

        contentAsString(result) mustEqual view(formWithError, NormalMode, testName, expectedManualUrl)(
          request,
          messages(application)
        ).toString

        verify(mockAddressLookupService, times(1)).postcodeSearch(eqTo("TE1 1ST"), eqTo(None))(any(), any())
      }
    }

    "must redirect to Journey Recovery when address lookup service returns an error" in {

      when(mockAddressLookupService.postcodeSearch(eqTo("TE1 1ST"), eqTo(None))(any(), any()))
        .thenReturn(Future.successful(Left(BadRequestError)))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[AddressLookupService].toInstance(mockAddressLookupService)
        )
        .build()

      running(application) {
        val request =
          FakeRequest(POST, findAddressRoute)
            .withFormUrlEncodedBody(("postcode", "TE1 1ST"))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        verify(mockAddressLookupService, times(1)).postcodeSearch(eqTo("TE1 1ST"), eqTo(None))(any(), any())
      }
    }

  }
}
