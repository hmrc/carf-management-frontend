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
import models.NormalMode
import models.OrganisationOrIndividual.{Individual, Organisation}
import models.errors.ApiError.InternalServerError
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when}
import pages.*
import pages.combined.OrganisationOrIndividualPage
import pages.individual.IndividualNamePage
import pages.organisation.{OverwritableOrganisationName, ReportForRegisteredBusinessPage}
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.AccountService
import views.html.ReviewAddressView

import scala.concurrent.Future

class ReviewAddressControllerSpec extends SpecBase {

  def onwardRoute = Call("GET", "/foo")

  lazy val reviewAddressRoute: String =
    controllers.routes.ReviewAddressController.onPageLoad(NormalMode).url

  lazy val reviewAddressOnSubmitRoute: String =
    controllers.routes.ReviewAddressController.onSubmit(NormalMode).url

  val mockAccountService: AccountService = mock[AccountService]

  "ReviewAddress Controller" - {

    "must return OK and the correct view for a GET when OverwritableOrganisationName is present" in {
      val userAnswers =
        emptyUserAnswers
          .withPage(AddressPagePrePop, testAddressUk)
          .withPage(OverwritableOrganisationName, testOrgName)
          .withPage(OrganisationOrIndividualPage, Organisation)

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(
          GET,
          reviewAddressRoute
        )

        val result = route(application, request).value

        val view            = application.injector.instanceOf[ReviewAddressView]
        val editAddressLink =
          routes.AddressController.onPageLoad(NormalMode).url

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(testAddressUk, NormalMode, editAddressLink, testOrgName)(
          request,
          messages(application)
        ).toString
      }
    }

    "must return OK and the correct view for a GET when IndividualName is present" in {
      val userAnswers =
        emptyUserAnswers
          .withPage(AddressPagePrePop, testAddressUk)
          .withPage(IndividualNamePage, testIndividualName)
          .withPage(OrganisationOrIndividualPage, Individual)

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(
          GET,
          reviewAddressRoute
        )

        val result = route(application, request).value

        val view            = application.injector.instanceOf[ReviewAddressView]
        val editAddressLink =
          routes.AddressController.onPageLoad(NormalMode).url

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(testAddressUk, NormalMode, editAddressLink, testIndividualName.fullName)(
          request,
          messages(application)
        ).toString
      }
    }

    "must return OK and the correct view for a GET when OverwritableOrganisationName is present but OrganisationOrIndividual is not" in {
      val userAnswers =
        emptyUserAnswers
          .withPage(AddressPagePrePop, testAddressUk)
          .withPage(OverwritableOrganisationName, testOrgName)

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(
          GET,
          reviewAddressRoute
        )

        val result = route(application, request).value

        val view            = application.injector.instanceOf[ReviewAddressView]
        val editAddressLink =
          routes.AddressController.onPageLoad(NormalMode).url

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(testAddressUk, NormalMode, editAddressLink, testOrgName)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Journey Recovery when an address is not found in userAnswers on page load" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(
          GET,
          reviewAddressRoute
        )

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery when no userAnswers exist on page load" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(
          GET,
          reviewAddressRoute
        )

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to the next page when user clicks the Continue button and is not rcasp user" in {
      val userAnswers =
        emptyUserAnswers.withPage(AddressPagePrePop, testAddressUk)

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockAccountService.getNumberOfRcaspsCurrentlyAdded(any())(any(), any()))
        .thenReturn(EitherT.rightT[Future, InternalServerError.type](1))

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[AccountService].toInstance(mockAccountService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(GET, reviewAddressOnSubmitRoute)

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
        verify(mockSessionRepository, times(1)).set(any())
      }
    }

    "must redirect to the next page when user clicks the Continue button and is rcasp user" in {
      val userAnswers =
        emptyUserAnswers.withPage(AddressPagePrePop, testAddressUk).withPage(ReportForRegisteredBusinessPage, true)

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockAccountService.getNumberOfRcaspsCurrentlyAdded(any())(any(), any()))
        .thenReturn(EitherT.rightT[Future, InternalServerError.type](0))

      val application =
        applicationBuilder(userAnswers = Some(userAnswers), requestUtr = Some(testUtr.uniqueTaxPayerReference))
          .overrides(
            bind[AccountService].toInstance(mockAccountService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(GET, reviewAddressOnSubmitRoute)

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          routes.PlaceholderController
            .onPageLoad(
              "Should nav to /registered-business/check-answers (CARF-294)"
            )
            .url
        verify(mockSessionRepository, times(1)).set(any())
      }
    }

    "must redirect to Journey Recovery on submit when address is not found" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(GET, reviewAddressOnSubmitRoute)

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery on submit when no userAnswers exist" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(GET, reviewAddressOnSubmitRoute)
        val result  = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

  }
}
