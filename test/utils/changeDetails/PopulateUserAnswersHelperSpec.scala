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
import models.OrganisationOrIndividual.{Individual, Organisation}
import models.viewAndUpdateRcasp.{IndividualRcaspDetails, OrganisationRcaspDetails}
import org.mockito.ArgumentMatchers.{any, argThat, eq as eqTo}
import org.mockito.Mockito.{reset, times, verify, when}
import pages.UkAddressInUserAnswers
import pages.changeDetails.ChangeRcaspCachedDetails
import pages.combined.OrganisationOrIndividualPage
import pages.individual.*
import pages.organisation.*
import play.api.mvc.Result
import play.api.test.Helpers.*
import utils.CountryListFactory

import scala.concurrent.Future

class PopulateUserAnswersHelperSpec extends SpecBase {

  val mockCountryListFactory: CountryListFactory = mock[CountryListFactory]

  val helper = new PopulateUserAnswersHelper(mockSessionRepository, mockCountryListFactory)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockCountryListFactory)
  }

  "PopulateUserAnswersHelper" - {
    ".populateUserAnswersForIndividual" - {
      "must populate user answers and redirect to ChangeDetails when all fields are present" in {
        val rcaspDetails: IndividualRcaspDetails = individualRcaspDetailsResponse

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
            ua.get(IndividualEmailPage).contains(testEmail) &&
            ua.get(IndividualHavePhonePage).contains(true) &&
            ua.get(IndividualPhonePage).contains(testPhone) &&
            ua.get(ChangeRcaspCachedDetails).contains(rcaspDetails)
          }
        )
      }

      "must populate user answers and redirect to ChangeDetails when phone number is absent" in {
        val rcaspDetails: IndividualRcaspDetails =
          individualRcaspDetailsResponse.copy(
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
            ua.get(IndividualEmailPage).contains(testEmail) &&
            ua.get(IndividualHavePhonePage).contains(false) &&
            ua.get(IndividualPhonePage).isEmpty &&
            ua.get(ChangeRcaspCachedDetails).contains(rcaspDetails)
          }
        )
      }

      "must redirect to journey recovery" - {
        "when TINDetails is missing" in {
          val rcaspDetails: IndividualRcaspDetails = individualRcaspDetailsResponse.copy(TINDetails = None)

          val result: Future[Result] = helper.populateUserAnswersForIndividual(userAnswersId, rcaspDetails)

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

          verify(mockSessionRepository, times(0)).set(any())
        }

        "when TINDetails contains an empty list" in {
          val rcaspDetails: IndividualRcaspDetails = individualRcaspDetailsResponse.copy(TINDetails = Some(List.empty))

          val result: Future[Result] = helper.populateUserAnswersForIndividual(userAnswersId, rcaspDetails)

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

          verify(mockSessionRepository, times(0)).set(any())
        }

        "when PrimaryContactDetails is missing" in {
          val rcaspDetails: IndividualRcaspDetails = individualRcaspDetailsResponse.copy(PrimaryContactDetails = None)

          val result: Future[Result] = helper.populateUserAnswersForIndividual(userAnswersId, rcaspDetails)

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

          verify(mockSessionRepository, times(0)).set(any())
        }

        "when address cannot be converted to AddressUk" in {
          val rcaspDetails: IndividualRcaspDetails =
            individualRcaspDetailsResponse.copy(AddressDetails = rcaspAddressEmptyOptionals)

          val result: Future[Result] = helper.populateUserAnswersForIndividual(userAnswersId, rcaspDetails)

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

          verify(mockSessionRepository, times(0)).set(any())
        }
      }
    }

    ".populateUserAnswersForOrganisation" - {
      "must populate user answers and redirect to ChangeDetails when all fields are present" in {
        val rcaspDetails: OrganisationRcaspDetails = organisationRcaspDetailsResponse

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
            ua.get(OrganisationSecondContactNamePage).contains("Prof Turo") &&
            ua.get(OrganisationSecondContactEmailPage).contains(testEmail) &&
            ua.get(OrganisationSecondContactHavePhonePage).contains(true) &&
            ua.get(OrganisationSecondContactPhoneNumberPage).contains(testPhone) &&
            ua.get(ChangeRcaspCachedDetails).contains(rcaspDetails)
          }
        )
      }

      "must populate user answers and redirect to ChangeDetails when separate trading name, first contact phone and second contact details are absent" in {
        val rcaspDetails: OrganisationRcaspDetails =
          organisationRcaspDetailsResponse.copy(
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
          organisationRcaspDetailsResponse.copy(
            SecondaryContactDetails = Some(rcaspContactDetails.copy(ContactName = "Prof Turo", PhoneNumber = None))
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
            ua.get(OrganisationSecondContactNamePage).contains("Prof Turo") &&
            ua.get(OrganisationSecondContactEmailPage).contains(testEmail) &&
            ua.get(OrganisationSecondContactHavePhonePage).contains(false) &&
            ua.get(OrganisationSecondContactPhoneNumberPage).isEmpty &&
            ua.get(ChangeRcaspCachedDetails).contains(rcaspDetails)
          }
        )
      }

      "must redirect to journey recovery" - {
        "when TINDetails is missing" in {
          val rcaspDetails: OrganisationRcaspDetails = organisationRcaspDetailsResponse.copy(TINDetails = None)

          val result: Future[Result] = helper.populateUserAnswersForOrganisation(userAnswersId, rcaspDetails)

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

          verify(mockSessionRepository, times(0)).set(any())
        }

        "when TINDetails contains an empty list" in {
          val rcaspDetails: OrganisationRcaspDetails =
            organisationRcaspDetailsResponse.copy(TINDetails = Some(List.empty))

          val result: Future[Result] = helper.populateUserAnswersForOrganisation(userAnswersId, rcaspDetails)

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

          verify(mockSessionRepository, times(0)).set(any())
        }

        "when PrimaryContactDetails is missing" in {
          val rcaspDetails: OrganisationRcaspDetails =
            organisationRcaspDetailsResponse.copy(PrimaryContactDetails = None)

          val result: Future[Result] = helper.populateUserAnswersForOrganisation(userAnswersId, rcaspDetails)

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

          verify(mockSessionRepository, times(0)).set(any())
        }

        "when address cannot be converted to AddressUk" in {
          val rcaspDetails: OrganisationRcaspDetails =
            organisationRcaspDetailsResponse.copy(AddressDetails = rcaspAddressEmptyOptionals)

          val result: Future[Result] = helper.populateUserAnswersForOrganisation(userAnswersId, rcaspDetails)

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

          verify(mockSessionRepository, times(0)).set(any())
        }
      }
    }

    ".populateUserAnswersForRegisteredBusiness" - {
      "must populate user answers and redirect to RegisteredBusinessChangeDetails when trading name is different from RCASP name" in {
        val rcaspDetails: OrganisationRcaspDetails =
          organisationRcaspDetailsResponse.copy(
            IsRCASPUser = true,
            PrimaryContactDetails = None,
            SecondaryContactDetails = None
          )

        when(mockCountryListFactory.getDescriptionFromCode(any())).thenReturn(Some("United Kingdom"))
        when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

        val result: Future[Result] = helper.populateUserAnswersForRegisteredBusiness(userAnswersId, rcaspDetails)

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          controllers.changeDetails.routes.RegisteredBusinessChangeDetailsController.onPageLoad(rcaspId).url

        verify(mockCountryListFactory, times(1)).getDescriptionFromCode(eqTo("GB"))
        verify(mockSessionRepository, times(1)).set(
          argThat { ua =>
            ua.id == userAnswersId &&
            ua.rcaspIsRegisteredBusiness &&
            ua.get(ReportForRegisteredBusinessPage).contains(true) &&
            ua.get(OrganisationNamePage).contains(testOrgName) &&
            ua.get(OverwritableOrganisationName).contains(testOrgName) &&
            ua.get(RegisteredBusinessIsThisYourBusinessNamePage).contains(true) &&
            ua.get(HaveTradingNamePage).contains(true) &&
            ua.get(TradingNamePage).contains(testTradingName) &&
            ua.get(UtrPage).contains(testUtr.uniqueTaxPayerReference) &&
            ua.get(RegisteredBusinessIsTheAddressCorrectPage).contains(true) &&
            ua.get(CachedBusinessDetailsPage).contains(cachedBusinessDetails) &&
            ua.get(ChangeRcaspCachedDetails).contains(rcaspDetails)
          }
        )
      }

      "must populate user answers and redirect to RegisteredBusinessChangeDetails when trading name is the same as RCASP name" in {
        val rcaspDetails: OrganisationRcaspDetails =
          organisationRcaspDetailsResponse.copy(
            IsRCASPUser = true,
            TradingName = testOrgName,
            PrimaryContactDetails = None,
            SecondaryContactDetails = None
          )

        when(mockCountryListFactory.getDescriptionFromCode(any())).thenReturn(Some("United Kingdom"))
        when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

        val result: Future[Result] = helper.populateUserAnswersForRegisteredBusiness(userAnswersId, rcaspDetails)

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          controllers.changeDetails.routes.RegisteredBusinessChangeDetailsController.onPageLoad(rcaspId).url

        verify(mockCountryListFactory, times(1)).getDescriptionFromCode(eqTo("GB"))
        verify(mockSessionRepository, times(1)).set(
          argThat { ua =>
            ua.id == userAnswersId &&
            ua.rcaspIsRegisteredBusiness &&
            ua.get(ReportForRegisteredBusinessPage).contains(true) &&
            ua.get(OrganisationNamePage).contains(testOrgName) &&
            ua.get(OverwritableOrganisationName).contains(testOrgName) &&
            ua.get(RegisteredBusinessIsThisYourBusinessNamePage).contains(true) &&
            ua.get(HaveTradingNamePage).contains(false) &&
            ua.get(TradingNamePage).isEmpty &&
            ua.get(UtrPage).contains(testUtr.uniqueTaxPayerReference) &&
            ua.get(RegisteredBusinessIsTheAddressCorrectPage).contains(true) &&
            ua.get(CachedBusinessDetailsPage).contains(cachedBusinessDetails) &&
            ua.get(ChangeRcaspCachedDetails).contains(rcaspDetails)
          }
        )
      }

      "must redirect to journey recovery" - {
        "when TINDetails is missing" in {
          val rcaspDetails: OrganisationRcaspDetails =
            organisationRcaspDetailsResponse.copy(
              IsRCASPUser = true,
              PrimaryContactDetails = None,
              SecondaryContactDetails = None,
              TINDetails = None
            )

          when(mockCountryListFactory.getDescriptionFromCode(any())).thenReturn(Some("United Kingdom"))

          val result: Future[Result] = helper.populateUserAnswersForRegisteredBusiness(userAnswersId, rcaspDetails)

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

          verify(mockCountryListFactory, times(1)).getDescriptionFromCode(eqTo("GB"))
          verify(mockSessionRepository, times(0)).set(any())
        }

        "when TINDetails contains an empty list" in {
          val rcaspDetails: OrganisationRcaspDetails =
            organisationRcaspDetailsResponse.copy(
              IsRCASPUser = true,
              PrimaryContactDetails = None,
              SecondaryContactDetails = None,
              TINDetails = Some(List.empty)
            )

          when(mockCountryListFactory.getDescriptionFromCode(any())).thenReturn(Some("United Kingdom"))

          val result: Future[Result] = helper.populateUserAnswersForRegisteredBusiness(userAnswersId, rcaspDetails)

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

          verify(mockCountryListFactory, times(1)).getDescriptionFromCode(eqTo("GB"))
          verify(mockSessionRepository, times(0)).set(any())
        }

        "when country name cannot be found from country code in AddressDetails" in {
          val rcaspDetails: OrganisationRcaspDetails =
            organisationRcaspDetailsResponse.copy(
              IsRCASPUser = true,
              PrimaryContactDetails = None,
              SecondaryContactDetails = None,
              AddressDetails = testAddressUkRcaspAddress.copy(CountryCode = "NA")
            )

          when(mockCountryListFactory.getDescriptionFromCode(any())).thenReturn(None)

          val result: Future[Result] = helper.populateUserAnswersForRegisteredBusiness(userAnswersId, rcaspDetails)

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

          verify(mockCountryListFactory, times(1)).getDescriptionFromCode(eqTo("NA"))
          verify(mockSessionRepository, times(0)).set(any())
        }
      }
    }
  }
}
