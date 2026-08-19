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
import forms.GenericYesNoPageFormProvider
import models.errors.ApiError.InternalServerError
import models.responses.AddressRegistrationResponse
import models.*
import models.individual.*
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.{any, argThat, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import pages.changeDetails.ChangeRcaspCachedDetails
import pages.organisation.*
import pages.combined.OrganisationOrIndividualPage
import pages.individual.*
import play.api.data.Form
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.{AccountService, RegistrationService}
import types.ResultT
import utils.CountryListFactory
import views.html.organisation.ReportForRegisteredBusinessView

import scala.concurrent.Future

class ReportForRegisteredBusinessControllerSpec extends SpecBase {

  def onwardRoute = Call("GET", "/foo")

  val formProvider                     = new GenericYesNoPageFormProvider()
  val form: Form[Boolean]              = formProvider("reportForRegisteredBusiness.error.required")
  val changeDetailsForm: Form[Boolean] = formProvider("reportForRegisteredBusiness.changeDetails.error.required")

  val mockRegistrationService: RegistrationService = mock[RegistrationService]
  val mockCountryListFactory: CountryListFactory   = mock[CountryListFactory]

  lazy val routeOnPageLoad: String = controllers.organisation.routes.ReportForRegisteredBusinessController
    .onPageLoad(NormalMode)
    .url

  lazy val routeOnPageLoadChangeMode: String = controllers.organisation.routes.ReportForRegisteredBusinessController
    .onPageLoad(ChangeMode)
    .url

  lazy val onSubmitRoute: String = controllers.organisation.routes.ReportForRegisteredBusinessController
    .onSubmit(NormalMode)
    .url

  lazy val onSubmitRouteChangeMode: String = controllers.organisation.routes.ReportForRegisteredBusinessController
    .onSubmit(ChangeMode)
    .url

  val businessDetailsFromService =
    BusinessDetails(
      name = "Timmy Ltd",
      address = AddressRegistrationResponse(
        addressLine1 = "1 Test Street",
        addressLine2 = Some("Testville"),
        addressLine3 = None,
        addressLine4 = None,
        postalCode = Some("TE1 1ST"),
        countryCode = "GB"
      )
    )

  "ReportForRegisteredBusiness Controller" - {

    "Add journey" - {
      "onPageLoad" - {
        "must return OK and the correct view for a GET when a UTR is present" in {
          when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

          when(mockRegistrationService.getBusinessWithCtUtr(eqTo(testUtr.uniqueTaxPayerReference))(any()))
            .thenReturn(ResultT.fromValue(businessDetailsFromService))

          when(mockCountryListFactory.getDescriptionFromCode(eqTo("GB")))
            .thenReturn(Some("United Kingdom"))

          val application =
            applicationBuilder(
              userAnswers = Some(emptyUserAnswers),
              requestUtr = Some(testUtr.uniqueTaxPayerReference)
            ).overrides(
              bind[RegistrationService].toInstance(mockRegistrationService),
              bind[CountryListFactory].toInstance(mockCountryListFactory)
            ).build()

          running(application) {
            val request = FakeRequest(GET, routeOnPageLoad)
            val result  = route(application, request).value
            val view    = application.injector.instanceOf[ReportForRegisteredBusinessView]

            status(result) mustEqual OK

            contentAsString(result) mustEqual view(
              form,
              NormalMode,
              Some(cachedBusinessDetails.name),
              false
            )(request, messages(application)).toString
          }
        }

        "must populate the view correctly on a GET when the question has previously been answered" in {
          when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

          when(mockRegistrationService.getBusinessWithCtUtr(any())(any()))
            .thenReturn(ResultT.fromValue(businessDetailsFromService))

          when(mockCountryListFactory.getDescriptionFromCode(any()))
            .thenReturn(Some("United Kingdom"))

          val userAnswers =
            emptyUserAnswers
              .withPage(ReportForRegisteredBusinessPage, true)

          val application =
            applicationBuilder(
              userAnswers = Some(userAnswers),
              requestUtr = Some(testUtr.uniqueTaxPayerReference)
            ).overrides(
              bind[RegistrationService].toInstance(mockRegistrationService),
              bind[CountryListFactory].toInstance(mockCountryListFactory)
            ).build()

          running(application) {
            val request = FakeRequest(GET, routeOnPageLoad)
            val result  = route(application, request).value
            val view    = application.injector.instanceOf[ReportForRegisteredBusinessView]

            status(result) mustEqual OK

            contentAsString(result) mustEqual view(
              form.fill(true),
              NormalMode,
              Some(cachedBusinessDetails.name),
              false
            )(request, messages(application)).toString
          }
        }

        "must redirect to Journey Recovery on GET when registration service returns an error" in {
          when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

          when(mockRegistrationService.getBusinessWithCtUtr(any())(any()))
            .thenReturn(ResultT.fromError(InternalServerError))

          val application =
            applicationBuilder(
              userAnswers = Some(emptyUserAnswers),
              requestUtr = Some(testUtr.uniqueTaxPayerReference)
            ).overrides(
              bind[RegistrationService].toInstance(mockRegistrationService),
              bind[CountryListFactory].toInstance(mockCountryListFactory)
            ).build()

          running(application) {
            val request = FakeRequest(GET, routeOnPageLoad)
            val result  = route(application, request).value

            status(result)                 mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
          }
        }

        "must redirect to Journey Recovery on GET when country code is not found in country list" in {
          when(mockRegistrationService.getBusinessWithCtUtr(any())(any()))
            .thenReturn(ResultT.fromValue(businessDetailsFromService))

          when(mockCountryListFactory.getDescriptionFromCode(any()))
            .thenReturn(None)

          val application =
            applicationBuilder(
              userAnswers = Some(emptyUserAnswers),
              requestUtr = Some(testUtr.uniqueTaxPayerReference)
            ).overrides(
              bind[RegistrationService].toInstance(mockRegistrationService),
              bind[CountryListFactory].toInstance(mockCountryListFactory)
            ).build()

          running(application) {
            val request = FakeRequest(GET, routeOnPageLoad)
            val result  = route(application, request).value

            status(result) mustEqual SEE_OTHER
          }
        }

        "must redirect to Journey Recovery on GET when CT UTR is not present" in {
          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), requestUtr = None).build()

          running(application) {
            val request = FakeRequest(GET, routeOnPageLoad)
            val result  = route(application, request).value

            status(result)                 mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
          }
        }
      }

      "onSubmit" - {
        "when the answer has changed" - {
          "in normal mode" - {
            "when the user answers Yes to the question, has a CT UTR and has zero RCASPs added" - {
              "must redirect to the next page and set rcaspIsRegisteredBusiness to true" in {
                val mockAccountService: AccountService = mock[AccountService]

                val userAnswers = emptyUserAnswers.withPage(CachedBusinessDetailsPage, cachedBusinessDetails)

                when(mockAccountService.getNumberOfRcaspsCurrentlyAdded(any())(any(), any()))
                  .thenReturn(ResultT.fromValue(0))
                when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

                val application =
                  applicationBuilder(
                    userAnswers = Some(userAnswers),
                    requestUtr = Some(testUtr.uniqueTaxPayerReference)
                  ).overrides(
                    bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
                    bind[AccountService].toInstance(mockAccountService)
                  ).build()

                running(application) {
                  val request = FakeRequest(POST, onSubmitRoute).withFormUrlEncodedBody(("value", "true"))

                  val result = route(application, request).value

                  status(result)                 mustEqual SEE_OTHER
                  redirectLocation(result).value mustEqual onwardRoute.url
                  verify(mockSessionRepository).set(argThat(_.rcaspIsRegisteredBusiness))
                }
              }
            }

            "when the user answers No to the question, has a CT UTR and has zero RCASPs added" - {
              "must redirect to the next page and keep rcaspIsRegisteredBusiness as false" in {
                val mockAccountService: AccountService = mock[AccountService]

                val userAnswers = emptyUserAnswers.withPage(CachedBusinessDetailsPage, cachedBusinessDetails)

                when(mockAccountService.getNumberOfRcaspsCurrentlyAdded(any())(any(), any()))
                  .thenReturn(ResultT.fromValue(0))
                when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

                val application =
                  applicationBuilder(
                    userAnswers = Some(userAnswers),
                    requestUtr = Some(testUtr.uniqueTaxPayerReference)
                  ).overrides(
                    bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
                    bind[AccountService].toInstance(mockAccountService)
                  ).build()

                running(application) {
                  val request = FakeRequest(POST, onSubmitRoute).withFormUrlEncodedBody(("value", "false"))

                  val result = route(application, request).value

                  status(result)                 mustEqual SEE_OTHER
                  redirectLocation(result).value mustEqual onwardRoute.url
                  verify(mockSessionRepository).set(argThat(!_.rcaspIsRegisteredBusiness))
                }
              }
            }

            "when the user answers Yes to the question, has a CT UTR and has more than zero RCASPs added" - {
              "must redirect to the next page and keep rcaspIsRegisteredBusiness as false" in {
                val mockAccountService: AccountService = mock[AccountService]

                val userAnswers = emptyUserAnswers.withPage(CachedBusinessDetailsPage, cachedBusinessDetails)

                when(mockAccountService.getNumberOfRcaspsCurrentlyAdded(any())(any(), any()))
                  .thenReturn(ResultT.fromValue(1))
                when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

                val application =
                  applicationBuilder(
                    userAnswers = Some(userAnswers),
                    requestUtr = Some(testUtr.uniqueTaxPayerReference)
                  ).overrides(
                    bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
                    bind[AccountService].toInstance(mockAccountService)
                  ).build()

                running(application) {
                  val request = FakeRequest(POST, onSubmitRoute).withFormUrlEncodedBody(("value", "true"))

                  val result = route(application, request).value

                  status(result)                 mustEqual SEE_OTHER
                  redirectLocation(result).value mustEqual onwardRoute.url
                  verify(mockSessionRepository).set(argThat(!_.rcaspIsRegisteredBusiness))
                }
              }
            }

            "when the user answers Yes to the question, does NOT have a CT UTR and has zero RCASPs added" - {
              "must redirect to the next page and keep rcaspIsRegisteredBusiness as false" in {
                val mockAccountService: AccountService = mock[AccountService]

                val userAnswers = emptyUserAnswers.withPage(CachedBusinessDetailsPage, cachedBusinessDetails)

                when(mockAccountService.getNumberOfRcaspsCurrentlyAdded(any())(any(), any()))
                  .thenReturn(ResultT.fromValue(0))
                when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

                val application =
                  applicationBuilder(
                    userAnswers = Some(userAnswers),
                    requestUtr = None
                  ).overrides(
                    bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
                    bind[AccountService].toInstance(mockAccountService)
                  ).build()

                running(application) {
                  val request = FakeRequest(POST, onSubmitRoute).withFormUrlEncodedBody(("value", "true"))

                  val result = route(application, request).value

                  status(result)                 mustEqual SEE_OTHER
                  redirectLocation(result).value mustEqual onwardRoute.url
                  verify(mockSessionRepository).set(argThat(!_.rcaspIsRegisteredBusiness))
                }
              }
            }

            "when the user answers has rcaspIsRegisteredBusiness as true, but fails the conditions for being a registered business" - {
              "must redirect to the next page and change rcaspIsRegisteredBusiness to false" in {
                val mockAccountService: AccountService = mock[AccountService]

                val userAnswers = emptyUserAnswers
                  .copy(rcaspIsRegisteredBusiness = true)
                  .withPage(CachedBusinessDetailsPage, cachedBusinessDetails)

                when(mockAccountService.getNumberOfRcaspsCurrentlyAdded(any())(any(), any()))
                  .thenReturn(ResultT.fromValue(0))
                when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

                val application =
                  applicationBuilder(
                    userAnswers = Some(userAnswers),
                    requestUtr = Some(testUtr.uniqueTaxPayerReference)
                  ).overrides(
                    bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
                    bind[AccountService].toInstance(mockAccountService)
                  ).build()

                running(application) {
                  val request = FakeRequest(POST, onSubmitRoute).withFormUrlEncodedBody(("value", "false"))

                  val result = route(application, request).value

                  status(result)                 mustEqual SEE_OTHER
                  redirectLocation(result).value mustEqual onwardRoute.url
                  verify(mockSessionRepository).set(argThat(!_.rcaspIsRegisteredBusiness))
                }
              }
            }

            "must redirect to Journey Recovery when AccountService returns an error" in {
              val mockAccountService: AccountService = mock[AccountService]

              val userAnswers = emptyUserAnswers.withPage(CachedBusinessDetailsPage, cachedBusinessDetails)

              when(mockAccountService.getNumberOfRcaspsCurrentlyAdded(any())(any(), any()))
                .thenReturn(ResultT.fromError(InternalServerError))

              val application =
                applicationBuilder(
                  userAnswers = Some(userAnswers),
                  requestUtr = Some(testUtr.uniqueTaxPayerReference)
                ).overrides(
                  bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
                  bind[AccountService].toInstance(mockAccountService)
                ).build()

              running(application) {
                val request = FakeRequest(POST, onSubmitRoute).withFormUrlEncodedBody(("value", "true"))
                val result  = route(application, request).value

                status(result)                 mustEqual SEE_OTHER
                redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
              }
            }
          }

          "in change mode" - {
            "when the answer is changed from false -> true" - {
              "must redirect to the next page, set rcaspIsRegisteredBusiness and clear pages" in {
                val mockAccountService: AccountService = mock[AccountService]

                val userAnswers = emptyUserAnswers
                  .withPage(ReportForRegisteredBusinessPage, false)
                  .withPage(OrganisationOrIndividualPage, OrganisationOrIndividual.Organisation)
                  .withPage(UtrPage, testUtr.uniqueTaxPayerReference)
                  .withPage(OrganisationFirstContactNamePage, testOrgName)
                  .withPage(OrganisationFirstContactEmailPage, testEmail)
                  .withPage(OrganisationFirstContactHavePhonePage, true)
                  .withPage(OrganisationFirstContactPhoneNumberPage, testPhone)
                  .withPage(OrganisationHaveSecondContactPage, true)
                  .withPage(OrganisationSecondContactNamePage, "Second Contact Name")
                  .withPage(OrganisationSecondContactEmailPage, testEmail)
                  .withPage(OrganisationSecondContactHavePhonePage, true)
                  .withPage(OrganisationSecondContactPhoneNumberPage, testPhone)
                  .withPage(IndividualNamePage, testIndividualName)
                  .withPage(NiNumberPage, testNiNumber)
                  .withPage(IndividualEmailPage, testEmail)
                  .withPage(IndividualHavePhonePage, true)
                  .withPage(IndividualPhonePage, testPhone)

                when(mockAccountService.getNumberOfRcaspsCurrentlyAdded(any())(any(), any()))
                  .thenReturn(ResultT.fromValue(0))
                when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

                val application = applicationBuilder(
                  userAnswers = Some(userAnswers),
                  requestUtr = Some(testUtr.uniqueTaxPayerReference)
                ).overrides(
                  bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
                  bind[AccountService].toInstance(mockAccountService)
                ).build()

                running(application) {
                  val request = FakeRequest(POST, onSubmitRouteChangeMode).withFormUrlEncodedBody(("value", "true"))
                  val result  = route(application, request).value

                  status(result)                 mustEqual SEE_OTHER
                  redirectLocation(result).value mustEqual onwardRoute.url

                  verify(mockSessionRepository).set(argThat { ua =>
                    ua.get(ReportForRegisteredBusinessPage).contains(true) &&
                    ua.rcaspIsRegisteredBusiness &&
                    ua.get(IndividualNamePage).isEmpty &&
                    ua.get(NiNumberPage).isEmpty &&
                    ua.get(IndividualEmailPage).isEmpty &&
                    ua.get(IndividualHavePhonePage).isEmpty &&
                    ua.get(IndividualPhonePage).isEmpty &&
                    ua.get(OrganisationFirstContactNamePage).isEmpty &&
                    ua.get(OrganisationFirstContactEmailPage).isEmpty &&
                    ua.get(OrganisationFirstContactHavePhonePage).isEmpty &&
                    ua.get(OrganisationFirstContactPhoneNumberPage).isEmpty &&
                    ua.get(OrganisationHaveSecondContactPage).isEmpty &&
                    ua.get(OrganisationSecondContactNamePage).isEmpty &&
                    ua.get(OrganisationSecondContactEmailPage).isEmpty &&
                    ua.get(OrganisationSecondContactHavePhonePage).isEmpty &&
                    ua.get(OrganisationSecondContactPhoneNumberPage).isEmpty &&
                    ua.get(OrganisationOrIndividualPage).isEmpty &&
                    ua.get(UtrPage).isEmpty
                  })
                }
              }
            }

            "when the answer is changed from true -> false" - {
              "must redirect to the next page, set rcaspIsRegisteredBusiness and clear pages" in {
                val mockAccountService: AccountService = mock[AccountService]

                val userAnswers = emptyUserAnswers
                  .withPage(ReportForRegisteredBusinessPage, true)
                  .withPage(RegisteredBusinessIsThisYourBusinessNamePage, true)
                  .withPage(RegisteredBusinessIsTheAddressCorrectPage, true)

                when(mockAccountService.getNumberOfRcaspsCurrentlyAdded(any())(any(), any()))
                  .thenReturn(ResultT.fromValue(0))
                when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

                val application = applicationBuilder(
                  userAnswers = Some(userAnswers),
                  requestUtr = Some(testUtr.uniqueTaxPayerReference)
                ).overrides(
                  bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
                  bind[AccountService].toInstance(mockAccountService)
                ).build()

                running(application) {
                  val request = FakeRequest(POST, onSubmitRouteChangeMode).withFormUrlEncodedBody(("value", "false"))
                  val result  = route(application, request).value

                  status(result)                 mustEqual SEE_OTHER
                  redirectLocation(result).value mustEqual onwardRoute.url

                  verify(mockSessionRepository).set(argThat { ua =>
                    ua.get(ReportForRegisteredBusinessPage).contains(false) &&
                    !ua.rcaspIsRegisteredBusiness &&
                    ua.get(RegisteredBusinessIsThisYourBusinessNamePage).isEmpty &&
                    ua.get(RegisteredBusinessIsTheAddressCorrectPage).isEmpty
                  })
                }
              }
            }
          }
        }

        "when the answer has not changed" - {
          "in normal mode" - {
            "when the answer remains as true" - {
              "must redirect to the next page without updating SessionRepository" in {
                val userAnswers = emptyUserAnswers.withPage(ReportForRegisteredBusinessPage, true)

                val application = applicationBuilder(userAnswers = Some(userAnswers))
                  .overrides(bind[Navigator].toInstance(new FakeNavigator(onwardRoute)))
                  .build()

                running(application) {
                  val request = FakeRequest(POST, onSubmitRoute).withFormUrlEncodedBody(("value", "true"))
                  val result  = route(application, request).value

                  status(result)                 mustEqual SEE_OTHER
                  redirectLocation(result).value mustEqual onwardRoute.url
                }
              }
            }

            "when the answer remains as false" - {
              "must redirect to the next page without updating SessionRepository" in {
                val userAnswers = emptyUserAnswers.withPage(ReportForRegisteredBusinessPage, false)

                val application = applicationBuilder(userAnswers = Some(userAnswers))
                  .overrides(bind[Navigator].toInstance(new FakeNavigator(onwardRoute)))
                  .build()

                running(application) {
                  val request = FakeRequest(POST, onSubmitRoute).withFormUrlEncodedBody(("value", "false"))
                  val result  = route(application, request).value

                  status(result)                 mustEqual SEE_OTHER
                  redirectLocation(result).value mustEqual onwardRoute.url
                }
              }
            }
          }

          "in change mode" - {
            "when the answer remains as true" - {
              "must redirect to EndofJourneyRoutingController without updating SessionRepository" in {
                val userAnswers = emptyUserAnswers.withPage(ReportForRegisteredBusinessPage, true)

                val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

                running(application) {
                  val request = FakeRequest(POST, onSubmitRouteChangeMode).withFormUrlEncodedBody(("value", "true"))
                  val result  = route(application, request).value

                  status(result)                 mustEqual SEE_OTHER
                  redirectLocation(result).value mustEqual
                    controllers.routes.EndOfJourneyRoutingController.onPageLoad().url
                }
              }
            }

            "when the answer remains as false" - {
              "must redirect to EndOfJourneyRoutingController without updating SessionRepository" in {
                val userAnswers = emptyUserAnswers.withPage(ReportForRegisteredBusinessPage, false)

                val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

                running(application) {
                  val request = FakeRequest(POST, onSubmitRouteChangeMode).withFormUrlEncodedBody(("value", "false"))
                  val result  = route(application, request).value

                  status(result)                 mustEqual SEE_OTHER
                  redirectLocation(result).value mustEqual
                    controllers.routes.EndOfJourneyRoutingController.onPageLoad().url
                }
              }
            }
          }
        }

        "must return a Bad Request and errors when invalid data is submitted" in {
          val userAnswers =
            emptyUserAnswers
              .withPage(CachedBusinessDetailsPage, cachedBusinessDetails)

          val application =
            applicationBuilder(userAnswers = Some(userAnswers))
              .build()

          running(application) {
            val request =
              FakeRequest(POST, routeOnPageLoad)
                .withFormUrlEncodedBody(("value", ""))

            val result = route(application, request).value

            status(result) mustEqual BAD_REQUEST
          }
        }
      }
    }

    "Change journey" - {
      "normal mode" - {
        "onPageLoad" - {
          "must return OK and the correct view for a GET when the question has previously been answered" in {
            val userAnswers = emptyUserAnswers
              .withPage(ReportForRegisteredBusinessPage, true)
              .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate.copy(IsRCASPUser = true))

            val application = applicationBuilder(
              userAnswers = Some(userAnswers),
              requestUtr = Some(testUtr.uniqueTaxPayerReference)
            ).build()

            running(application) {
              val request = FakeRequest(GET, routeOnPageLoad)
              val result  = route(application, request).value
              val view    = application.injector.instanceOf[ReportForRegisteredBusinessView]

              status(result)          mustEqual OK
              contentAsString(result) mustEqual view(changeDetailsForm.fill(true), NormalMode, None, true)(
                request,
                messages(application)
              ).toString
            }
          }

          "must redirect to Journey Recovery on GET when ReportForRegisteredBusinessPage is empty" in {
            val userAnswers = emptyUserAnswers
              .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate.copy(IsRCASPUser = true))

            val application = applicationBuilder(
              userAnswers = Some(userAnswers),
              requestUtr = Some(testUtr.uniqueTaxPayerReference)
            ).build()

            running(application) {
              val request = FakeRequest(GET, routeOnPageLoad)
              val result  = route(application, request).value

              status(result)                 mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
            }
          }

          "must redirect to Journey Recovery if isRCASPUser is false" in {
            val userAnswers = emptyUserAnswers
              .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate)

            val application = applicationBuilder(
              userAnswers = Some(userAnswers),
              requestUtr = Some(testUtr.uniqueTaxPayerReference)
            ).build()

            running(application) {
              val request = FakeRequest(GET, routeOnPageLoad)
              val result  = route(application, request).value

              status(result)                 mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
            }
          }

          "must redirect to Journey Recovery on GET when CT UTR is not present" in {
            val userAnswers = emptyUserAnswers
              .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate)

            val application = applicationBuilder(
              userAnswers = Some(userAnswers),
              requestUtr = None
            ).build()

            running(application) {
              val request = FakeRequest(GET, routeOnPageLoad)
              val result  = route(application, request).value

              status(result)                 mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
            }
          }
        }

        "onSubmit" - {
          "when the answer is changed from false -> true and ChangeRcaspCachedDetails contains RCASP = true" - {
            "must redirect to RegisteredBusinessIsThisYourBusinessNameController and set rcaspIsRegisteredBusiness to true and clear pages" in {
              val userAnswers = emptyUserAnswers
                .withPage(ReportForRegisteredBusinessPage, false)
                .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate.copy(IsRCASPUser = true))
                .withPage(OrganisationOrIndividualPage, OrganisationOrIndividual.Organisation)
                .withPage(UtrPage, testUtr.uniqueTaxPayerReference)
                .withPage(OrganisationFirstContactNamePage, testOrgName)
                .withPage(OrganisationFirstContactEmailPage, testEmail)
                .withPage(OrganisationFirstContactHavePhonePage, true)
                .withPage(OrganisationFirstContactPhoneNumberPage, testPhone)
                .withPage(OrganisationHaveSecondContactPage, true)
                .withPage(OrganisationSecondContactNamePage, "Second Contact Name")
                .withPage(OrganisationSecondContactEmailPage, testEmail)
                .withPage(OrganisationSecondContactHavePhonePage, true)
                .withPage(OrganisationSecondContactPhoneNumberPage, testPhone)
                .withPage(IndividualNamePage, testIndividualName)
                .withPage(NiNumberPage, testNiNumber)
                .withPage(IndividualEmailPage, testEmail)
                .withPage(IndividualHavePhonePage, true)
                .withPage(IndividualPhonePage, testPhone)

              when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

              val application =
                applicationBuilder(
                  userAnswers = Some(userAnswers),
                  requestUtr = Some(testUtr.uniqueTaxPayerReference)
                ).build()

              running(application) {
                val request = FakeRequest(POST, onSubmitRoute).withFormUrlEncodedBody(("value", "true"))
                val result  = route(application, request).value

                status(result) mustEqual SEE_OTHER
                redirectLocation(
                  result
                ).value        mustEqual controllers.organisation.routes.RegisteredBusinessIsThisYourBusinessNameController
                  .onPageLoad(NormalMode)
                  .url

                verify(mockSessionRepository).set(argThat { ua =>
                  ua.get(ReportForRegisteredBusinessPage).contains(true) &&
                  ua.rcaspIsRegisteredBusiness &&
                  ua.get(IndividualNamePage).isEmpty &&
                  ua.get(NiNumberPage).isEmpty &&
                  ua.get(IndividualEmailPage).isEmpty &&
                  ua.get(IndividualHavePhonePage).isEmpty &&
                  ua.get(IndividualPhonePage).isEmpty &&
                  ua.get(OrganisationFirstContactNamePage).isEmpty &&
                  ua.get(OrganisationFirstContactEmailPage).isEmpty &&
                  ua.get(OrganisationFirstContactHavePhonePage).isEmpty &&
                  ua.get(OrganisationFirstContactPhoneNumberPage).isEmpty &&
                  ua.get(OrganisationHaveSecondContactPage).isEmpty &&
                  ua.get(OrganisationSecondContactNamePage).isEmpty &&
                  ua.get(OrganisationSecondContactEmailPage).isEmpty &&
                  ua.get(OrganisationSecondContactHavePhonePage).isEmpty &&
                  ua.get(OrganisationSecondContactPhoneNumberPage).isEmpty &&
                  ua.get(OrganisationOrIndividualPage).isEmpty &&
                  ua.get(UtrPage).isEmpty
                })
              }
            }
          }

          "when the answer is changed from false -> true and ChangeRcaspCachedDetails contains RCASP = false" - {
            "must redirect to Journey recovery" in {
              val userAnswers = emptyUserAnswers
                .withPage(ReportForRegisteredBusinessPage, false)
                .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate.copy(IsRCASPUser = false))

              when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

              val application =
                applicationBuilder(
                  userAnswers = Some(userAnswers),
                  requestUtr = Some(testUtr.uniqueTaxPayerReference)
                ).build()

              running(application) {
                val request = FakeRequest(POST, onSubmitRoute).withFormUrlEncodedBody(("value", "true"))
                val result  = route(application, request).value

                status(result) mustEqual SEE_OTHER
                redirectLocation(
                  result
                ).value        mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
              }
            }
          }

          "when the answer is changed from true -> false" - {
            "must redirect to OrganisationOrIndividualController and set rcaspIsRegisteredBusiness to false and clear pages" in {
              val userAnswers = emptyUserAnswers
                .withPage(ReportForRegisteredBusinessPage, true)
                .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate)
                .withPage(RegisteredBusinessIsThisYourBusinessNamePage, true)
                .withPage(RegisteredBusinessIsTheAddressCorrectPage, true)

              when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

              val application =
                applicationBuilder(userAnswers = Some(userAnswers))
                  .build()

              running(application) {
                val request = FakeRequest(POST, onSubmitRoute).withFormUrlEncodedBody(("value", "false"))
                val result  = route(application, request).value

                status(result)                 mustEqual SEE_OTHER
                redirectLocation(result).value mustEqual controllers.combined.routes.OrganisationOrIndividualController
                  .onPageLoad(NormalMode)
                  .url

                verify(mockSessionRepository).set(argThat { ua =>
                  ua.get(ReportForRegisteredBusinessPage).contains(false) && !ua.rcaspIsRegisteredBusiness &&
                  ua.get(RegisteredBusinessIsThisYourBusinessNamePage).isEmpty &&
                  ua.get(RegisteredBusinessIsTheAddressCorrectPage).isEmpty
                })
              }
            }
          }

          "when the answer remains unchanged as true" - {
            "must redirect to the next page without updating SessionRepository" in {
              val userAnswers = emptyUserAnswers
                .withPage(ReportForRegisteredBusinessPage, true)
                .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate)

              val application = applicationBuilder(userAnswers = Some(userAnswers))
                .overrides(bind[Navigator].toInstance(new FakeNavigator(onwardRoute)))
                .build()

              running(application) {
                val request = FakeRequest(POST, onSubmitRoute).withFormUrlEncodedBody(("value", "true"))
                val result  = route(application, request).value

                status(result)                 mustEqual SEE_OTHER
                redirectLocation(result).value mustEqual onwardRoute.url
              }
            }
          }

          "when the answer remains unchanged as false" - {
            "must redirect to OrganisationOrIndividualController without updating SessionRepository" in {
              val userAnswers = emptyUserAnswers
                .withPage(ReportForRegisteredBusinessPage, false)
                .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate)

              val application = applicationBuilder(userAnswers = Some(userAnswers))
                .overrides(bind[Navigator].toInstance(new FakeNavigator(onwardRoute)))
                .build()

              running(application) {
                val request = FakeRequest(POST, onSubmitRoute).withFormUrlEncodedBody(("value", "false"))
                val result  = route(application, request).value

                status(result)                 mustEqual SEE_OTHER
                redirectLocation(result).value mustEqual onwardRoute.url
              }
            }
          }

          "must return a Bad Request and errors when invalid data is submitted" in {
            val userAnswers =
              emptyUserAnswers
                .withPage(ReportForRegisteredBusinessPage, true)
                .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate)

            val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

            running(application) {
              val request = FakeRequest(POST, onSubmitRoute).withFormUrlEncodedBody(("value", ""))
              val result  = route(application, request).value

              status(result) mustEqual BAD_REQUEST
            }
          }
        }
      }

      "change mode" - {
        "onPageLoad" - {
          "must return OK and the correct view for a GET when the question has previously been answered" in {
            val userAnswers = emptyUserAnswers
              .withPage(ReportForRegisteredBusinessPage, true)
              .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate.copy(IsRCASPUser = true))

            val application = applicationBuilder(
              userAnswers = Some(userAnswers),
              requestUtr = Some(testUtr.uniqueTaxPayerReference)
            ).build()

            running(application) {
              val request = FakeRequest(GET, routeOnPageLoadChangeMode)
              val result  = route(application, request).value
              val view    = application.injector.instanceOf[ReportForRegisteredBusinessView]

              status(result)          mustEqual OK
              contentAsString(result) mustEqual view(changeDetailsForm.fill(true), ChangeMode, None, true)(
                request,
                messages(application)
              ).toString
            }
          }

          "must redirect to Journey Recovery on GET when ReportForRegisteredBusinessPage is empty" in {
            val application = applicationBuilder(userAnswers =
              Some(
                emptyUserAnswers
                  .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate.copy(IsRCASPUser = true))
              )
            ).build()

            running(application) {
              val request = FakeRequest(GET, routeOnPageLoadChangeMode)
              val result  = route(application, request).value

              status(result)                 mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
            }
          }
        }

        "onSubmit" - {
          "when the answer is changed from false -> true and ChangeRcaspCachedDetails contains RCASP = true" - {
            "must redirect to RegisteredBusinessIsThisYourBusinessNameController and set rcaspIsRegisteredBusiness to true and clear pages" in {
              val userAnswers = emptyUserAnswers
                .withPage(ReportForRegisteredBusinessPage, false)
                .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate.copy(IsRCASPUser = true))
                .withPage(OrganisationOrIndividualPage, OrganisationOrIndividual.Organisation)
                .withPage(UtrPage, testUtr.uniqueTaxPayerReference)
                .withPage(OrganisationFirstContactNamePage, testOrgName)
                .withPage(OrganisationFirstContactEmailPage, testEmail)
                .withPage(OrganisationFirstContactHavePhonePage, true)
                .withPage(OrganisationFirstContactPhoneNumberPage, testPhone)
                .withPage(OrganisationHaveSecondContactPage, true)
                .withPage(OrganisationSecondContactNamePage, "Second Contact Name")
                .withPage(OrganisationSecondContactEmailPage, testEmail)
                .withPage(OrganisationSecondContactHavePhonePage, true)
                .withPage(OrganisationSecondContactPhoneNumberPage, testPhone)
                .withPage(IndividualNamePage, testIndividualName)
                .withPage(NiNumberPage, testNiNumber)
                .withPage(IndividualEmailPage, testEmail)
                .withPage(IndividualHavePhonePage, true)
                .withPage(IndividualPhonePage, testPhone)

              when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

              val application =
                applicationBuilder(
                  userAnswers = Some(userAnswers),
                  requestUtr = Some(testUtr.uniqueTaxPayerReference)
                ).build()

              running(application) {
                val request = FakeRequest(POST, onSubmitRouteChangeMode).withFormUrlEncodedBody(("value", "true"))
                val result  = route(application, request).value

                status(result) mustEqual SEE_OTHER
                redirectLocation(
                  result
                ).value        mustEqual controllers.organisation.routes.RegisteredBusinessIsThisYourBusinessNameController
                  .onPageLoad(NormalMode)
                  .url

                verify(mockSessionRepository).set(argThat { ua =>
                  ua.get(ReportForRegisteredBusinessPage).contains(true) &&
                  ua.rcaspIsRegisteredBusiness &&
                  ua.get(IndividualNamePage).isEmpty &&
                  ua.get(NiNumberPage).isEmpty &&
                  ua.get(IndividualEmailPage).isEmpty &&
                  ua.get(IndividualHavePhonePage).isEmpty &&
                  ua.get(IndividualPhonePage).isEmpty &&
                  ua.get(OrganisationFirstContactNamePage).isEmpty &&
                  ua.get(OrganisationFirstContactEmailPage).isEmpty &&
                  ua.get(OrganisationFirstContactHavePhonePage).isEmpty &&
                  ua.get(OrganisationFirstContactPhoneNumberPage).isEmpty &&
                  ua.get(OrganisationHaveSecondContactPage).isEmpty &&
                  ua.get(OrganisationSecondContactNamePage).isEmpty &&
                  ua.get(OrganisationSecondContactEmailPage).isEmpty &&
                  ua.get(OrganisationSecondContactHavePhonePage).isEmpty &&
                  ua.get(OrganisationSecondContactPhoneNumberPage).isEmpty &&
                  ua.get(OrganisationOrIndividualPage).isEmpty &&
                  ua.get(UtrPage).isEmpty
                })
              }
            }
          }

          "when the answer is changed from false -> true and ChangeRcaspCachedDetails contains RCASP = false" - {
            "must redirect to Journey recovery" in {
              val userAnswers = emptyUserAnswers
                .withPage(ReportForRegisteredBusinessPage, false)
                .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate)

              when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

              val application =
                applicationBuilder(
                  userAnswers = Some(userAnswers),
                  requestUtr = Some(testUtr.uniqueTaxPayerReference)
                ).build()

              running(application) {
                val request = FakeRequest(POST, onSubmitRouteChangeMode).withFormUrlEncodedBody(("value", "true"))
                val result  = route(application, request).value

                status(result)                 mustEqual SEE_OTHER
                redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
              }
            }
          }

          "when the answer is changed from true -> false" - {
            "must redirect to OrganisationOrIndividualController and set rcaspIsRegisteredBusiness to false and clear pages" in {
              val userAnswers = emptyUserAnswers
                .withPage(ReportForRegisteredBusinessPage, true)
                .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate)
                .withPage(RegisteredBusinessIsThisYourBusinessNamePage, true)
                .withPage(RegisteredBusinessIsTheAddressCorrectPage, true)

              when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

              val application =
                applicationBuilder(userAnswers = Some(userAnswers)).build()

              running(application) {
                val request = FakeRequest(POST, onSubmitRouteChangeMode).withFormUrlEncodedBody(("value", "false"))
                val result  = route(application, request).value

                status(result)                 mustEqual SEE_OTHER
                redirectLocation(result).value mustEqual controllers.combined.routes.OrganisationOrIndividualController
                  .onPageLoad(NormalMode)
                  .url

                verify(mockSessionRepository).set(argThat { ua =>
                  ua.get(ReportForRegisteredBusinessPage).contains(false) && !ua.rcaspIsRegisteredBusiness &&
                  ua.get(RegisteredBusinessIsThisYourBusinessNamePage).isEmpty &&
                  ua.get(RegisteredBusinessIsTheAddressCorrectPage).isEmpty
                })
              }
            }
          }

          "when the answer remains unchanged as true" - {
            "must redirect to EndofJourneyRoutingController without updating SessionRepository" in {
              val userAnswers = emptyUserAnswers
                .withPage(ReportForRegisteredBusinessPage, true)
                .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate)

              val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

              running(application) {
                val request = FakeRequest(POST, onSubmitRouteChangeMode).withFormUrlEncodedBody(("value", "true"))
                val result  = route(application, request).value

                status(result)                 mustEqual SEE_OTHER
                redirectLocation(result).value mustEqual
                  controllers.routes.EndOfJourneyRoutingController.onPageLoad().url
              }
            }
          }

          "when the answer remains unchanged as false" - {
            "must redirect to EndOfJourneyRoutingController without updating SessionRepository" in {
              val userAnswers = emptyUserAnswers
                .withPage(ReportForRegisteredBusinessPage, false)
                .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate)

              val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

              running(application) {
                val request = FakeRequest(POST, onSubmitRouteChangeMode).withFormUrlEncodedBody(("value", "false"))
                val result  = route(application, request).value

                status(result)                 mustEqual SEE_OTHER
                redirectLocation(result).value mustEqual
                  controllers.routes.EndOfJourneyRoutingController.onPageLoad().url
              }
            }
          }

          "must return a Bad Request and errors when invalid data is submitted" in {
            val userAnswers =
              emptyUserAnswers
                .withPage(ReportForRegisteredBusinessPage, true)
                .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate)

            val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

            running(application) {
              val request = FakeRequest(POST, onSubmitRouteChangeMode).withFormUrlEncodedBody(("value", ""))
              val result  = route(application, request).value

              status(result) mustEqual BAD_REQUEST
            }
          }
        }
      }
    }
  }
}
