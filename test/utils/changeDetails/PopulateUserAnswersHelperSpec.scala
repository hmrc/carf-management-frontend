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

package utils.changeDetails

import base.SpecBase
import models.BusinessDetails
import models.OrganisationOrIndividual.{Individual, Organisation}
import models.errors.ApiError.InternalServerError
import models.responses.AddressRegistrationResponse
import models.viewAndUpdateRcasp.{IndividualRcaspDetails, OrganisationRcaspDetails}
import org.mockito.ArgumentMatchers.{any, argThat, eq as eqTo}
import org.mockito.Mockito.{reset, times, verify, when}
import pages.{AddressPagePrePop, UkAddressInUserAnswers}
import pages.changeDetails.ChangeRcaspCachedDetails
import pages.combined.OrganisationOrIndividualPage
import pages.individual.*
import pages.organisation.*
import play.api.mvc.Result
import play.api.test.Helpers.*
import services.RegistrationService
import types.ResultT
import utils.CountryListFactory

import scala.concurrent.Future

class PopulateUserAnswersHelperSpec extends SpecBase {

  val mockRegistrationService: RegistrationService = mock[RegistrationService]
  val mockCountryListFactory: CountryListFactory   = mock[CountryListFactory]

  val helper = new PopulateUserAnswersHelper(mockSessionRepository, mockRegistrationService, mockCountryListFactory)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockRegistrationService, mockCountryListFactory)
  }

  "PopulateUserAnswersHelper" - {
    ".populateUserAnswersForIndividual" - {
      "must populate user answers and redirect to ChangeDetails when all fields are present" in {
        val rcaspDetails: IndividualRcaspDetails = individualRcaspDetailsViewUpdate

        when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

        val result: Future[Result] = helper.populateUserAnswersForIndividual(userAnswersId, rcaspDetails)

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.changeDetails.routes.ChangeDetailsController
          .onPageLoad(rcaspId)
          .url

        verify(mockSessionRepository, times(1)).set(
          argThat { ua =>
            ua.id == userAnswersId &&
            !ua.rcaspIsRegisteredBusiness &&
            ua.get(ReportForRegisteredBusinessPage).contains(false) &&
            ua.get(OrganisationOrIndividualPage).contains(Individual) &&
            ua.get(IndividualNamePage).contains(testIndividualName) &&
            ua.get(NiNumberPage).contains(testNiNumber) &&
            ua.get(UkAddressInUserAnswers).contains(testAddressUk) &&
            ua.get(AddressPagePrePop).contains(testAddressUk) &&
            ua.get(IndividualEmailPage).contains(testEmail) &&
            ua.get(IndividualHavePhonePage).contains(true) &&
            ua.get(IndividualPhonePage).contains(testPhone) &&
            ua.get(ChangeRcaspCachedDetails).contains(rcaspDetails)
          }
        )
      }

      "must populate user answers and redirect to ChangeDetails when phone number is absent" in {
        val rcaspDetails: IndividualRcaspDetails =
          individualRcaspDetailsViewUpdate.copy(
            PrimaryContactDetails = Some(rcaspContactDetails.copy(PhoneNumber = None))
          )

        when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

        val result: Future[Result] = helper.populateUserAnswersForIndividual(userAnswersId, rcaspDetails)

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.changeDetails.routes.ChangeDetailsController
          .onPageLoad(rcaspId)
          .url

        verify(mockSessionRepository, times(1)).set(
          argThat { ua =>
            ua.id == userAnswersId &&
            !ua.rcaspIsRegisteredBusiness &&
            ua.get(ReportForRegisteredBusinessPage).contains(false) &&
            ua.get(OrganisationOrIndividualPage).contains(Individual) &&
            ua.get(IndividualNamePage).contains(testIndividualName) &&
            ua.get(NiNumberPage).contains(testNiNumber) &&
            ua.get(UkAddressInUserAnswers).contains(testAddressUk) &&
            ua.get(AddressPagePrePop).contains(testAddressUk) &&
            ua.get(IndividualEmailPage).contains(testEmail) &&
            ua.get(IndividualHavePhonePage).contains(false) &&
            ua.get(IndividualPhonePage).isEmpty &&
            ua.get(ChangeRcaspCachedDetails).contains(rcaspDetails)
          }
        )
      }

      "must redirect to journey recovery" - {
        "when TINDetails is missing" in {
          val rcaspDetails: IndividualRcaspDetails = individualRcaspDetailsViewUpdate.copy(TINDetails = None)

          val result: Future[Result] = helper.populateUserAnswersForIndividual(userAnswersId, rcaspDetails)

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

          verify(mockSessionRepository, times(0)).set(any())
        }

        "when TINDetails contains an empty list" in {
          val rcaspDetails: IndividualRcaspDetails =
            individualRcaspDetailsViewUpdate.copy(TINDetails = Some(List.empty))

          val result: Future[Result] = helper.populateUserAnswersForIndividual(userAnswersId, rcaspDetails)

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

          verify(mockSessionRepository, times(0)).set(any())
        }

        "when PrimaryContactDetails is missing" in {
          val rcaspDetails: IndividualRcaspDetails = individualRcaspDetailsViewUpdate.copy(PrimaryContactDetails = None)

          val result: Future[Result] = helper.populateUserAnswersForIndividual(userAnswersId, rcaspDetails)

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

          verify(mockSessionRepository, times(0)).set(any())
        }

        "when address cannot be converted to AddressUk" in {
          val rcaspDetails: IndividualRcaspDetails =
            individualRcaspDetailsViewUpdate.copy(AddressDetails = rcaspAddressEmptyOptionals)

          val result: Future[Result] = helper.populateUserAnswersForIndividual(userAnswersId, rcaspDetails)

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

          verify(mockSessionRepository, times(0)).set(any())
        }
      }
    }

    ".populateUserAnswersForOrganisation" - {
      "must populate user answers and redirect to ChangeDetails when all fields are present" in {
        val rcaspDetails: OrganisationRcaspDetails = organisationRcaspDetailsViewUpdate

        when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

        val result: Future[Result] = helper.populateUserAnswersForOrganisation(userAnswersId, rcaspDetails)

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.changeDetails.routes.ChangeDetailsController
          .onPageLoad(rcaspId)
          .url

        verify(mockSessionRepository, times(1)).set(
          argThat { ua =>
            ua.id == userAnswersId &&
            !ua.rcaspIsRegisteredBusiness &&
            ua.get(ReportForRegisteredBusinessPage).contains(false) &&
            ua.get(OrganisationOrIndividualPage).contains(Organisation) &&
            ua.get(OrganisationNamePage).contains(testOrgName) &&
            ua.get(OverwritableOrganisationName).contains(testOrgName) &&
            ua.get(HaveTradingNamePage).contains(true) &&
            ua.get(TradingNamePage).contains(testTradingName) &&
            ua.get(UtrPage).contains(testUtr.uniqueTaxPayerReference) &&
            ua.get(UkAddressInUserAnswers).contains(testAddressUk) &&
            ua.get(AddressPagePrePop).contains(testAddressUk) &&
            ua.get(OrganisationFirstContactNamePage).contains(testIndividualName.fullName) &&
            ua.get(OrganisationFirstContactEmailPage).contains(testEmail) &&
            ua.get(OrganisationFirstContactHavePhonePage).contains(true) &&
            ua.get(OrganisationFirstContactPhoneNumberPage).contains(testPhone) &&
            ua.get(OrganisationHaveSecondContactPage).contains(true) &&
            ua.get(OrganisationSecondContactNamePage).contains(testOrgContactName) &&
            ua.get(OrganisationSecondContactEmailPage).contains(testEmail) &&
            ua.get(OrganisationSecondContactHavePhonePage).contains(true) &&
            ua.get(OrganisationSecondContactPhoneNumberPage).contains(testPhone) &&
            ua.get(ChangeRcaspCachedDetails).contains(rcaspDetails)
          }
        )
      }

      "must populate user answers and redirect to ChangeDetails when separate trading name, first contact phone and second contact details are absent" in {
        val rcaspDetails: OrganisationRcaspDetails =
          organisationRcaspDetailsViewUpdate.copy(
            TradingName = testOrgName,
            PrimaryContactDetails = Some(rcaspContactDetails.copy(PhoneNumber = None)),
            SecondaryContactDetails = None
          )

        when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

        val result: Future[Result] = helper.populateUserAnswersForOrganisation(userAnswersId, rcaspDetails)

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.changeDetails.routes.ChangeDetailsController
          .onPageLoad(rcaspId)
          .url

        verify(mockSessionRepository, times(1)).set(
          argThat { ua =>
            ua.id == userAnswersId &&
            !ua.rcaspIsRegisteredBusiness &&
            ua.get(ReportForRegisteredBusinessPage).contains(false) &&
            ua.get(OrganisationOrIndividualPage).contains(Organisation) &&
            ua.get(OrganisationNamePage).contains(testOrgName) &&
            ua.get(OverwritableOrganisationName).contains(testOrgName) &&
            ua.get(HaveTradingNamePage).contains(false) &&
            ua.get(TradingNamePage).isEmpty &&
            ua.get(UtrPage).contains(testUtr.uniqueTaxPayerReference) &&
            ua.get(UkAddressInUserAnswers).contains(testAddressUk) &&
            ua.get(AddressPagePrePop).contains(testAddressUk) &&
            ua.get(OrganisationFirstContactNamePage).contains(testIndividualName.fullName) &&
            ua.get(OrganisationFirstContactEmailPage).contains(testEmail) &&
            ua.get(OrganisationFirstContactHavePhonePage).contains(false) &&
            ua.get(OrganisationFirstContactPhoneNumberPage).isEmpty &&
            ua.get(OrganisationHaveSecondContactPage).contains(false) &&
            ua.get(OrganisationSecondContactNamePage).isEmpty &&
            ua.get(OrganisationSecondContactEmailPage).isEmpty &&
            ua.get(OrganisationSecondContactHavePhonePage).isEmpty &&
            ua.get(OrganisationSecondContactPhoneNumberPage).isEmpty &&
            ua.get(ChangeRcaspCachedDetails).contains(rcaspDetails)
          }
        )
      }

      "must populate user answers and redirect to ChangeDetails when only second contact phone is absent" in {
        val rcaspDetails: OrganisationRcaspDetails =
          organisationRcaspDetailsViewUpdate.copy(
            SecondaryContactDetails =
              Some(rcaspContactDetails.copy(ContactName = testOrgContactName, PhoneNumber = None))
          )

        when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

        val result: Future[Result] = helper.populateUserAnswersForOrganisation(userAnswersId, rcaspDetails)

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.changeDetails.routes.ChangeDetailsController
          .onPageLoad(rcaspId)
          .url

        verify(mockSessionRepository, times(1)).set(
          argThat { ua =>
            ua.id == userAnswersId &&
            !ua.rcaspIsRegisteredBusiness &&
            ua.get(ReportForRegisteredBusinessPage).contains(false) &&
            ua.get(OrganisationOrIndividualPage).contains(Organisation) &&
            ua.get(OrganisationNamePage).contains(testOrgName) &&
            ua.get(OverwritableOrganisationName).contains(testOrgName) &&
            ua.get(HaveTradingNamePage).contains(true) &&
            ua.get(TradingNamePage).contains(testTradingName) &&
            ua.get(UtrPage).contains(testUtr.uniqueTaxPayerReference) &&
            ua.get(UkAddressInUserAnswers).contains(testAddressUk) &&
            ua.get(OrganisationFirstContactNamePage).contains(testIndividualName.fullName) &&
            ua.get(OrganisationFirstContactEmailPage).contains(testEmail) &&
            ua.get(OrganisationFirstContactHavePhonePage).contains(true) &&
            ua.get(OrganisationFirstContactPhoneNumberPage).contains(testPhone) &&
            ua.get(OrganisationHaveSecondContactPage).contains(true) &&
            ua.get(OrganisationSecondContactNamePage).contains(testOrgContactName) &&
            ua.get(OrganisationSecondContactEmailPage).contains(testEmail) &&
            ua.get(OrganisationSecondContactHavePhonePage).contains(false) &&
            ua.get(OrganisationSecondContactPhoneNumberPage).isEmpty &&
            ua.get(ChangeRcaspCachedDetails).contains(rcaspDetails)
          }
        )
      }

      "must redirect to journey recovery" - {
        "when TINDetails is missing" in {
          val rcaspDetails: OrganisationRcaspDetails = organisationRcaspDetailsViewUpdate.copy(TINDetails = None)

          val result: Future[Result] = helper.populateUserAnswersForOrganisation(userAnswersId, rcaspDetails)

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

          verify(mockSessionRepository, times(0)).set(any())
        }

        "when TINDetails contains an empty list" in {
          val rcaspDetails: OrganisationRcaspDetails =
            organisationRcaspDetailsViewUpdate.copy(TINDetails = Some(List.empty))

          val result: Future[Result] = helper.populateUserAnswersForOrganisation(userAnswersId, rcaspDetails)

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

          verify(mockSessionRepository, times(0)).set(any())
        }

        "when PrimaryContactDetails is missing" in {
          val rcaspDetails: OrganisationRcaspDetails =
            organisationRcaspDetailsViewUpdate.copy(PrimaryContactDetails = None)

          val result: Future[Result] = helper.populateUserAnswersForOrganisation(userAnswersId, rcaspDetails)

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

          verify(mockSessionRepository, times(0)).set(any())
        }

        "when address cannot be converted to AddressUk" in {
          val rcaspDetails: OrganisationRcaspDetails =
            organisationRcaspDetailsViewUpdate.copy(AddressDetails = rcaspAddressEmptyOptionals)

          val result: Future[Result] = helper.populateUserAnswersForOrganisation(userAnswersId, rcaspDetails)

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

          verify(mockSessionRepository, times(0)).set(any())
        }
      }
    }

    ".populateUserAnswersForRegisteredBusiness" - {
      val businessDetailsFromService =
        BusinessDetails(
          name = "Timmy Ltd",
          address = AddressRegistrationResponse(
            addressLine1 = "1 Test",
            addressLine2 = Some("Test Street"),
            addressLine3 = Some("Test Region"),
            addressLine4 = Some("Testingtown"),
            postalCode = Some(testPostcode),
            countryCode = "GB"
          )
        )

      "must populate user answers and redirect to RegisteredBusinessChangeDetails when trading name is different from RCASP name" in {
        val rcaspDetails: OrganisationRcaspDetails =
          organisationRcaspDetailsViewUpdate.copy(
            IsRCASPUser = true,
            PrimaryContactDetails = None,
            SecondaryContactDetails = None
          )

        when(mockRegistrationService.getBusinessWithCtUtr(eqTo(testUtr.uniqueTaxPayerReference))(any()))
          .thenReturn(ResultT.fromValue(businessDetailsFromService))
        when(mockCountryListFactory.getDescriptionFromCode(any())).thenReturn(Some("United Kingdom"))
        when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

        val result: Future[Result] =
          helper.populateUserAnswersForRegisteredBusiness(userAnswersId, testUtr, rcaspDetails)

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          controllers.changeDetails.routes.RegisteredBusinessChangeDetailsController.onPageLoad(rcaspId).url

        verify(mockRegistrationService, times(1)).getBusinessWithCtUtr(eqTo(testUtr.uniqueTaxPayerReference))(any())
        verify(mockCountryListFactory, times(1)).getDescriptionFromCode(eqTo("GB"))
        verify(mockSessionRepository, times(1)).set(
          argThat { ua =>
            ua.id == userAnswersId &&
            ua.rcaspIsRegisteredBusiness &&
            ua.get(ReportForRegisteredBusinessPage).contains(true) &&
            ua.get(OrganisationNamePage).contains(testOrgName) &&
            ua.get(OverwritableOrganisationName).contains(testOrgName) &&
            ua.get(HaveTradingNamePage).contains(true) &&
            ua.get(TradingNamePage).contains(testTradingName) &&
            ua.get(UtrPage).contains(testUtr.uniqueTaxPayerReference) &&
            ua.get(UkAddressInUserAnswers).contains(testAddressUk) &&
            ua.get(AddressPagePrePop).contains(testAddressUk) &&
            ua.get(CachedBusinessDetailsPage).contains(cachedBusinessDetails) &&
            ua.get(ChangeRcaspCachedDetails).contains(rcaspDetails)
          }
        )
      }

      "must populate user answers and redirect to RegisteredBusinessChangeDetails when trading name is the same as RCASP name" in {
        val rcaspDetails: OrganisationRcaspDetails =
          organisationRcaspDetailsViewUpdate.copy(
            IsRCASPUser = true,
            TradingName = testOrgName,
            PrimaryContactDetails = None,
            SecondaryContactDetails = None
          )

        when(mockRegistrationService.getBusinessWithCtUtr(eqTo(testUtr.uniqueTaxPayerReference))(any()))
          .thenReturn(ResultT.fromValue(businessDetailsFromService))
        when(mockCountryListFactory.getDescriptionFromCode(any())).thenReturn(Some("United Kingdom"))
        when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

        val result: Future[Result] =
          helper.populateUserAnswersForRegisteredBusiness(userAnswersId, testUtr, rcaspDetails)

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          controllers.changeDetails.routes.RegisteredBusinessChangeDetailsController.onPageLoad(rcaspId).url

        verify(mockRegistrationService, times(1)).getBusinessWithCtUtr(eqTo(testUtr.uniqueTaxPayerReference))(any())
        verify(mockCountryListFactory, times(1)).getDescriptionFromCode(eqTo("GB"))
        verify(mockSessionRepository, times(1)).set(
          argThat { ua =>
            ua.id == userAnswersId &&
            ua.rcaspIsRegisteredBusiness &&
            ua.get(ReportForRegisteredBusinessPage).contains(true) &&
            ua.get(OrganisationNamePage).contains(testOrgName) &&
            ua.get(OverwritableOrganisationName).contains(testOrgName) &&
            ua.get(HaveTradingNamePage).contains(false) &&
            ua.get(TradingNamePage).isEmpty &&
            ua.get(UtrPage).contains(testUtr.uniqueTaxPayerReference) &&
            ua.get(UkAddressInUserAnswers).contains(testAddressUk) &&
            ua.get(AddressPagePrePop).contains(testAddressUk) &&
            ua.get(CachedBusinessDetailsPage).contains(cachedBusinessDetails) &&
            ua.get(ChangeRcaspCachedDetails).contains(rcaspDetails)
          }
        )
      }

      "must redirect to journey recovery" - {
        "when an error is returned from RegistrationService" in {
          val rcaspDetails: OrganisationRcaspDetails =
            organisationRcaspDetailsViewUpdate.copy(
              IsRCASPUser = true,
              PrimaryContactDetails = None,
              SecondaryContactDetails = None
            )

          when(mockRegistrationService.getBusinessWithCtUtr(eqTo(testUtr.uniqueTaxPayerReference))(any()))
            .thenReturn(ResultT.fromError(InternalServerError))

          val result: Future[Result] =
            helper.populateUserAnswersForRegisteredBusiness(userAnswersId, testUtr, rcaspDetails)

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

          verify(mockRegistrationService, times(1)).getBusinessWithCtUtr(eqTo(testUtr.uniqueTaxPayerReference))(any())
          verify(mockCountryListFactory, times(0)).getDescriptionFromCode(any())
          verify(mockSessionRepository, times(0)).set(any())
        }

        "when country code lookup fails for the address from ETMP" in {
          val rcaspDetails: OrganisationRcaspDetails =
            organisationRcaspDetailsViewUpdate.copy(
              IsRCASPUser = true,
              PrimaryContactDetails = None,
              SecondaryContactDetails = None
            )

          when(mockRegistrationService.getBusinessWithCtUtr(eqTo(testUtr.uniqueTaxPayerReference))(any()))
            .thenReturn(ResultT.fromValue(businessDetailsFromService))
          when(mockCountryListFactory.getDescriptionFromCode(any())).thenReturn(None)

          val result: Future[Result] =
            helper.populateUserAnswersForRegisteredBusiness(userAnswersId, testUtr, rcaspDetails)

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

          verify(mockRegistrationService, times(1)).getBusinessWithCtUtr(eqTo(testUtr.uniqueTaxPayerReference))(any())
          verify(mockCountryListFactory, times(1)).getDescriptionFromCode(eqTo("GB"))
          verify(mockSessionRepository, times(0)).set(any())
        }

        "when TINDetails is missing" in {
          val rcaspDetails: OrganisationRcaspDetails =
            organisationRcaspDetailsViewUpdate.copy(
              IsRCASPUser = true,
              PrimaryContactDetails = None,
              SecondaryContactDetails = None,
              TINDetails = None
            )

          val result: Future[Result] =
            helper.populateUserAnswersForRegisteredBusiness(userAnswersId, testUtr, rcaspDetails)

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

          verify(mockRegistrationService, times(0)).getBusinessWithCtUtr(any())(any())
          verify(mockCountryListFactory, times(0)).getDescriptionFromCode(any())
          verify(mockSessionRepository, times(0)).set(any())
        }

        "when TINDetails contains an empty list" in {
          val rcaspDetails: OrganisationRcaspDetails =
            organisationRcaspDetailsViewUpdate.copy(
              IsRCASPUser = true,
              PrimaryContactDetails = None,
              SecondaryContactDetails = None,
              TINDetails = Some(List.empty)
            )

          val result: Future[Result] =
            helper.populateUserAnswersForRegisteredBusiness(userAnswersId, testUtr, rcaspDetails)

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

          verify(mockRegistrationService, times(0)).getBusinessWithCtUtr(any())(any())
          verify(mockCountryListFactory, times(0)).getDescriptionFromCode(any())
          verify(mockSessionRepository, times(0)).set(any())
        }

        "when address cannot be converted to AddressUk" in {
          val rcaspDetails: OrganisationRcaspDetails =
            organisationRcaspDetailsViewUpdate.copy(
              IsRCASPUser = true,
              PrimaryContactDetails = None,
              SecondaryContactDetails = None,
              AddressDetails = rcaspAddressEmptyOptionals
            )

          val result: Future[Result] =
            helper.populateUserAnswersForRegisteredBusiness(userAnswersId, testUtr, rcaspDetails)

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

          verify(mockRegistrationService, times(0)).getBusinessWithCtUtr(any())(any())
          verify(mockCountryListFactory, times(0)).getDescriptionFromCode(any())
          verify(mockSessionRepository, times(0)).set(any())
        }

        "when country name cannot be found from country code in AddressDetails" in {
          val rcaspDetails: OrganisationRcaspDetails =
            organisationRcaspDetailsViewUpdate.copy(
              IsRCASPUser = true,
              PrimaryContactDetails = None,
              SecondaryContactDetails = None,
              AddressDetails = testAddressUkRcaspAddress.copy(CountryCode = "NA")
            )

          val result: Future[Result] =
            helper.populateUserAnswersForRegisteredBusiness(userAnswersId, testUtr, rcaspDetails)

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

          verify(mockRegistrationService, times(0)).getBusinessWithCtUtr(any())(any())
          verify(mockCountryListFactory, times(0)).getDescriptionFromCode(any())
          verify(mockSessionRepository, times(0)).set(any())
        }
      }
    }
  }
}
