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
import cats.data.EitherT
import forms.ChooseAddressFormProvider
import models.OrganisationOrIndividual.{Individual, Organisation}
import models.countries.CountryUk
import models.errors.ApiError.InternalServerError
import models.{format, AddressAndUPRN, AddressUk, FindAddress, NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.{any, argThat}
import org.mockito.Mockito.{times, verify, when}
import pages.*
import pages.combined.OrganisationOrIndividualPage
import pages.individual.IndividualNamePage
import pages.organisation.{OverwritableOrganisationName, ReportForRegisteredBusinessPage}
import play.api.data.Form
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.{redirectLocation, *}
import services.AccountService
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem
import views.html.ChooseAddressView

import scala.concurrent.Future

class ChooseAddressControllerSpec extends SpecBase {

  def onwardRoute = Call("GET", "/foo")

  private lazy val chooseAddressRoute =
    controllers.routes.ChooseAddressController.onPageLoad(NormalMode).url

  private lazy val isRcaspUserRoute =
    routes.PlaceholderController.onPageLoad("Should nav to /registered-business/check-answers (CARF-294)").url

  val formProvider       = new ChooseAddressFormProvider()
  val form: Form[String] = formProvider()

  val mockAccountService: AccountService = mock[AccountService]

  val address = AddressUk(
    "1 Test Street",
    Some("Line 2"),
    None,
    "Testtown",
    "BB00 0BB",
    CountryUk("GB", "United Kingdom")
  )

  "ChooseAddress Controller" - {

    "must return OK and the correct view for a GET when OverwriteableOrganisationName is present" in {

      val userAnswers =
        emptyUserAnswers
          .withPage(AddressLookupResult, Seq(AddressAndUPRN(address, testUPRN)))
          .withPage(FindAddressPage, FindAddress(address.postCode, None))
          .withPage(FindAddressAdditionalCallUa, false)
          .withPage(OverwritableOrganisationName, testOrgName)
          .withPage(OrganisationOrIndividualPage, Organisation)

      val application =
        applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, chooseAddressRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ChooseAddressView]

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, createAddressRadios(Seq(address)), None, testOrgName)(
          request,
          messages(application)
        ).toString
      }
    }

    "must return OK and the correct view for a GET when IndividualNamePage is present" in {

      val userAnswers =
        emptyUserAnswers
          .withPage(AddressLookupResult, Seq(AddressAndUPRN(address, testUPRN)))
          .withPage(FindAddressPage, FindAddress(address.postCode, None))
          .withPage(FindAddressAdditionalCallUa, false)
          .withPage(IndividualNamePage, testIndividualName)
          .withPage(OrganisationOrIndividualPage, Individual)
      val application =
        applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, chooseAddressRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ChooseAddressView]

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(
          form,
          NormalMode,
          createAddressRadios(Seq(address)),
          None,
          testIndividualName.fullName
        )(
          request,
          messages(application)
        ).toString
      }
    }

    "must return OK and the correct view for a GET when OverwriteableOrganisationName is present but OrganisationOrIndividualPage is not" in {
      val userAnswers =
        emptyUserAnswers
          .withPage(AddressLookupResult, Seq(AddressAndUPRN(address, testUPRN)))
          .withPage(FindAddressPage, FindAddress(address.postCode, None))
          .withPage(FindAddressAdditionalCallUa, false)
          .withPage(OverwritableOrganisationName, testOrgName)

      val application =
        applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, chooseAddressRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ChooseAddressView]

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, createAddressRadios(Seq(address)), None, testOrgName)(
          request,
          messages(application)
        ).toString
      }
    }

    "must return OK and the correct view with dynamic html element for a GET when additional call is true" in {

      val additionalHtml = generateHtml("property 1", address.postCode)

      val userAnswers =
        emptyUserAnswers
          .withPage(AddressLookupResult, Seq(AddressAndUPRN(address, testUPRN)))
          .withPage(FindAddressPage, FindAddress(address.postCode, Some("property 1")))
          .withPage(FindAddressAdditionalCallUa, true)
          .withPage(OverwritableOrganisationName, testOrgName)

      val application =
        applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, chooseAddressRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ChooseAddressView]

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(
          form,
          NormalMode,
          createAddressRadios(Seq(address)),
          Some(additionalHtml),
          testOrgName
        )(
          request,
          messages(application)
        ).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers =
        emptyUserAnswers
          .withPage(ChooseAddressPage, address.format)
          .withPage(AddressLookupResult, Seq(AddressAndUPRN(address, testUPRN)))
          .withPage(FindAddressPage, FindAddress(address.postCode, None))
          .withPage(FindAddressAdditionalCallUa, false)
          .withPage(OverwritableOrganisationName, testOrgName)

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, chooseAddressRoute)

        val view = application.injector.instanceOf[ChooseAddressView]

        val result = route(application, request).value

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(
          form.fill(address.format),
          NormalMode,
          createAddressRadios(Seq(address)),
          None,
          testOrgName
        )(
          request,
          messages(application)
        ).toString
      }
    }

    "must return Redirect to journey recovery when FindAddressPage or FindAddressAdditionalCallUa is missing in ua for GET " in {

      val userAnswers = emptyUserAnswers.withPage(AddressLookupResult, Seq(AddressAndUPRN(address, testUPRN)))

      val application =
        applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, chooseAddressRoute)

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController
          .onPageLoad()
          .url
      }
    }

    "must redirect to journey recovery when AddressLookupResult not present for a GET" in {

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, chooseAddressRoute)

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController
          .onPageLoad()
          .url
      }
    }

    "must redirect to address page when no address is found but AddressLookup is present for a GET" in {

      val userAnswers = emptyUserAnswers.withPage(AddressLookupResult, Seq.empty)

      val application =
        applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, chooseAddressRoute)

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.PlaceholderController
          .onPageLoad("Should redirect to /address - (CARF-203)")
          .url
      }
    }

    "must redirect to the next page when valid data is submitted and is not rcasp user" in {
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockAccountService.getNumberOfRcaspsCurrentlyAdded(any()))
        .thenReturn(EitherT.rightT[Future, InternalServerError.type](1))

      val userAnswers =
        emptyUserAnswers
          .withPage(AddressLookupResult, Seq(AddressAndUPRN(address, testUPRN)))
          .withPage(FindAddressPage, FindAddress(address.postCode, None))
          .withPage(FindAddressAdditionalCallUa, false)
          .withPage(OverwritableOrganisationName, testOrgName)

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[AccountService].toInstance(mockAccountService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, chooseAddressRoute)
            .withFormUrlEncodedBody(("value", address.format))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
        verify(mockSessionRepository, times(1)).set(
          argThat(_.get(AddressUPRNUserAnswers).get == testUPRN)
        )
      }
    }

    "must redirect to the next page when none of these is submitted and does not store an address" in {
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockAccountService.getNumberOfRcaspsCurrentlyAdded(any()))
        .thenReturn(EitherT.rightT[Future, InternalServerError.type](1))

      val userAnswers =
        emptyUserAnswers
          .withPage(AddressLookupResult, Seq(AddressAndUPRN(address, testUPRN)))
          .withPage(FindAddressPage, FindAddress(address.postCode, None))
          .withPage(FindAddressAdditionalCallUa, false)
          .withPage(OverwritableOrganisationName, testOrgName)

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[AccountService].toInstance(mockAccountService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, chooseAddressRoute)
            .withFormUrlEncodedBody(("value", "none"))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url

        verify(mockSessionRepository).set(argThat(_.get(SelectedChooseAddressPage).isEmpty))
      }
    }

    "must redirect to the next page when valid data is submitted and is rcasp user" in {
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockAccountService.getNumberOfRcaspsCurrentlyAdded(any()))
        .thenReturn(EitherT.rightT[Future, InternalServerError.type](0))

      val userAnswers =
        emptyUserAnswers
          .withPage(AddressLookupResult, Seq(AddressAndUPRN(address, testUPRN)))
          .withPage(FindAddressPage, FindAddress(address.postCode, None))
          .withPage(FindAddressAdditionalCallUa, false)
          .withPage(OverwritableOrganisationName, testOrgName)
          .withPage(ReportForRegisteredBusinessPage, true)

      val application =
        applicationBuilder(userAnswers = Some(userAnswers), requestUtr = Some(testUtr.uniqueTaxPayerReference))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[AccountService].toInstance(mockAccountService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, chooseAddressRoute)
            .withFormUrlEncodedBody(("value", address.format))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual isRcaspUserRoute
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val userAnswers =
        emptyUserAnswers
          .withPage(AddressLookupResult, Seq(AddressAndUPRN(address, testUPRN)))
          .withPage(FindAddressPage, FindAddress(address.postCode, None))
          .withPage(FindAddressAdditionalCallUa, false)
          .withPage(OverwritableOrganisationName, testOrgName)

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, chooseAddressRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[ChooseAddressView]

        val result = route(application, request).value

        status(result)          mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(
          boundForm,
          NormalMode,
          createAddressRadios(Seq(address)),
          None,
          testOrgName
        )(
          request,
          messages(application)
        ).toString
      }
    }

    "must return a Bad Request and errors when invalid data is submitted when additional call flag is true" in {

      val additionalHtml = generateHtml("property 1", address.postCode)

      val userAnswers =
        emptyUserAnswers
          .withPage(AddressLookupResult, Seq(AddressAndUPRN(address, testUPRN)))
          .withPage(FindAddressPage, FindAddress(address.postCode, Some("property 1")))
          .withPage(FindAddressAdditionalCallUa, true)
          .withPage(OverwritableOrganisationName, testOrgName)

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, chooseAddressRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[ChooseAddressView]

        val result = route(application, request).value

        status(result)          mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(
          boundForm,
          NormalMode,
          createAddressRadios(Seq(address)),
          Some(additionalHtml),
          testOrgName
        )(
          request,
          messages(application)
        ).toString
      }
    }

    "redirect to Journey Recovery for a POST if no existing user answers data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, chooseAddressRoute)
            .withFormUrlEncodedBody(("value", "None"))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must fail when address selected cannot be found for a POST" in {

      val userAnswers = emptyUserAnswers.withPage(AddressLookupResult, Seq(AddressAndUPRN(address, testUPRN)))

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[AccountService].toInstance(mockAccountService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, chooseAddressRoute)
            .withFormUrlEncodedBody(("value", "Test road 15 not found street"))

        val result = route(application, request).value

        result.failed.futureValue.getMessage mustEqual "Failed to find address"

      }
    }

    def createAddressRadios(addresses: => Seq[AddressUk]): Seq[RadioItem] =
      addresses.map { address =>
        val addressFormatted = {
          val addressLines = Seq(
            Some(address.addressLine1),
            address.addressLine2,
            address.addressLine3,
            Some(address.townOrCity),
            Some(address.postCode)
          ).flatten ++ {
            if (address.countryUk.code == "GB") {
              Seq.empty
            } else {
              Seq(address.countryUk.name)
            }
          }

          addressLines.mkString(", ")
        }
        RadioItem(content = Text(s"$addressFormatted"), value = Some(s"$addressFormatted"))
      }

    def generateHtml(property: String, postcode: String) =
      s"""We could not find a match for ‘$property’ — showing all results for $postcode instead."""

  }
}
