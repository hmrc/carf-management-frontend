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

package utils

import base.SpecBase
import models.OrganisationOrIndividual.{Individual, Organisation}
import models.{RcaspAddress, RcaspContactDetails, TinDetails}
import models.requests.{CreateRcaspRequest, IndividualRcaspDetails, OrganisationRcaspDetails, RCASPManagementRequest}
import pages.UkAddressInUserAnswers
import pages.combined.OrganisationOrIndividualPage
import pages.individual.*
import pages.organisation.*

class RcaspSubmissionHelperSpec extends SpecBase {

  val rcaspSubmissionHelper: RcaspSubmissionHelper = new RcaspSubmissionHelper

  private val rcaspAddress = RcaspAddress(
    AddressLine1 = "1 Test",
    AddressLine2 = Some("Test Street"),
    AddressLine3 = Some("Testingtown"),
    AddressLine4 = Some("Test Region"),
    PostalCode = testPostcode,
    CountryCode = "GB"
  )

  private val individualContactDetails = RcaspContactDetails(
    ContactName = testIndividualName.fullName,
    EmailAddress = testEmail,
    PhoneNumber = Some(testPhone)
  )

  private val organisationContactDetails = RcaspContactDetails(
    ContactName = testOrgContactName,
    EmailAddress = testEmail,
    PhoneNumber = Some(testPhone)
  )

  private val individualRcaspDetails = IndividualRcaspDetails(
    SubscriptionID = carfId,
    IsRCASPUser = false,
    PartyType = "Individual",
    FirstName = testIndividualName.firstName,
    LastName = testIndividualName.lastName,
    TINDetails = Some(
      List(
        TinDetails(
          TINType = "OTHER",
          TIN = testNiNumber,
          IssuedBy = "GB"
        )
      )
    ),
    AddressDetails = rcaspAddress,
    PrimaryContactDetails = Some(individualContactDetails)
  )

  private val organisationRcaspDetails = OrganisationRcaspDetails(
    SubscriptionID = carfId,
    IsRCASPUser = false,
    PartyType = "Organisation",
    RCASPName = testOrgName,
    TradingName = testTradingName,
    TINDetails = Some(
      List(
        TinDetails(
          TINType = "UTR",
          TIN = testUtr.uniqueTaxPayerReference,
          IssuedBy = "GB"
        )
      )
    ),
    AddressDetails = rcaspAddress,
    PrimaryContactDetails = Some(organisationContactDetails),
    SecondaryContactDetails = Some(organisationContactDetails)
  )

  private val registeredBusinessRcaspDetails = organisationRcaspDetails.copy(
    IsRCASPUser = true,
    PrimaryContactDetails = None,
    SecondaryContactDetails = None
  )

  "RcaspSubmissionHelper" - {
    ".createRcaspRequestForRegisteredBusiness" - {
      "must build the request successfully with all required fields where cached address is correct" in {
        val userAnswers = emptyUserAnswers
          .withPage(ReportForRegisteredBusinessPage, true)
          .withPage(OverwritableOrganisationName, testOrgName)
          .withPage(HaveTradingNamePage, true)
          .withPage(TradingNamePage, testTradingName)
          .withPage(RegisteredBusinessIsTheAddressCorrectPage, true)
          .withPage(CachedBusinessDetailsPage, cachedBusinessDetails)

        val result: Option[CreateRcaspRequest] =
          rcaspSubmissionHelper.createRcaspRequestForRegisteredBusiness(carfId, testUtr, userAnswers)

        result mustBe Some(
          CreateRcaspRequest(
            RCASPManagementRequest(
              RequestCommon = rcaspCreateRequestCommon,
              RequestDetails = registeredBusinessRcaspDetails
            )
          )
        )
      }

      "must build the request without trading name where cached address is not correct" in {
        val userAnswers = emptyUserAnswers
          .withPage(ReportForRegisteredBusinessPage, true)
          .withPage(OverwritableOrganisationName, testOrgName)
          .withPage(HaveTradingNamePage, false)
          .withPage(RegisteredBusinessIsTheAddressCorrectPage, false)
          .withPage(UkAddressInUserAnswers, testAddressUk)

        val result: Option[CreateRcaspRequest] =
          rcaspSubmissionHelper.createRcaspRequestForRegisteredBusiness(carfId, testUtr, userAnswers)

        result mustBe Some(
          CreateRcaspRequest(
            RCASPManagementRequest(
              RequestCommon = rcaspCreateRequestCommon,
              RequestDetails = registeredBusinessRcaspDetails.copy(TradingName = testOrgName)
            )
          )
        )
      }

      "must return None when organisation name is missing" in {
        val userAnswers = emptyUserAnswers
          .withPage(ReportForRegisteredBusinessPage, true)
          .withPage(HaveTradingNamePage, true)
          .withPage(TradingNamePage, testTradingName)
          .withPage(RegisteredBusinessIsTheAddressCorrectPage, false)
          .withPage(UkAddressInUserAnswers, testAddressUk)

        val result: Option[CreateRcaspRequest] =
          rcaspSubmissionHelper.createRcaspRequestForRegisteredBusiness(carfId, testUtr, userAnswers)

        result mustBe None
      }

      "must return None when haveTradingName is true but trading name is missing" in {
        val userAnswers = emptyUserAnswers
          .withPage(ReportForRegisteredBusinessPage, true)
          .withPage(OverwritableOrganisationName, testOrgName)
          .withPage(HaveTradingNamePage, true)
          .withPage(RegisteredBusinessIsTheAddressCorrectPage, false)
          .withPage(UkAddressInUserAnswers, testAddressUk)

        val result: Option[CreateRcaspRequest] =
          rcaspSubmissionHelper.createRcaspRequestForRegisteredBusiness(carfId, testUtr, userAnswers)

        result mustBe None
      }

      "must return None when cached address is correct but CachedBusinessDetails is missing" in {
        val userAnswers = emptyUserAnswers
          .withPage(ReportForRegisteredBusinessPage, true)
          .withPage(OverwritableOrganisationName, testOrgName)
          .withPage(HaveTradingNamePage, false)
          .withPage(RegisteredBusinessIsTheAddressCorrectPage, true)
          .withPage(UkAddressInUserAnswers, testAddressUk)

        val result: Option[CreateRcaspRequest] =
          rcaspSubmissionHelper.createRcaspRequestForRegisteredBusiness(carfId, testUtr, userAnswers)

        result mustBe None
      }

      "must return None when cached address is not correct and UkAddressInUserAnswers is missing" in {
        val userAnswers = emptyUserAnswers
          .withPage(ReportForRegisteredBusinessPage, true)
          .withPage(OverwritableOrganisationName, testOrgName)
          .withPage(HaveTradingNamePage, false)
          .withPage(RegisteredBusinessIsTheAddressCorrectPage, false)
          .withPage(CachedBusinessDetailsPage, cachedBusinessDetails)

        val result: Option[CreateRcaspRequest] =
          rcaspSubmissionHelper.createRcaspRequestForRegisteredBusiness(carfId, testUtr, userAnswers)

        result mustBe None
      }

      "must return None when reportForRegisteredBusiness is not true" - {
        "when reportForRegisteredBusiness is false" - {
          val userAnswers = emptyUserAnswers
            .withPage(ReportForRegisteredBusinessPage, false)
            .withPage(OverwritableOrganisationName, testOrgName)
            .withPage(HaveTradingNamePage, true)
            .withPage(TradingNamePage, testTradingName)
            .withPage(RegisteredBusinessIsTheAddressCorrectPage, false)
            .withPage(UkAddressInUserAnswers, testAddressUk)

          val result: Option[CreateRcaspRequest] =
            rcaspSubmissionHelper.createRcaspRequestForRegisteredBusiness(carfId, testUtr, userAnswers)

          result mustBe None
        }

        "when reportForRegisteredBusiness is missing" - {
          val userAnswers = emptyUserAnswers
            .withPage(OverwritableOrganisationName, testOrgName)
            .withPage(HaveTradingNamePage, true)
            .withPage(TradingNamePage, testTradingName)
            .withPage(RegisteredBusinessIsTheAddressCorrectPage, false)
            .withPage(UkAddressInUserAnswers, testAddressUk)

          val result: Option[CreateRcaspRequest] =
            rcaspSubmissionHelper.createRcaspRequestForRegisteredBusiness(carfId, testUtr, userAnswers)

          result mustBe None
        }
      }
    }

    ".createRcaspRequest" - {
      "for an Individual" - {
        "must build the request successfully with all required fields" in {
          val userAnswers = emptyUserAnswers
            .withPage(ReportForRegisteredBusinessPage, false)
            .withPage(OrganisationOrIndividualPage, Individual)
            .withPage(IndividualNamePage, testIndividualName)
            .withPage(NiNumberPage, testNiNumber)
            .withPage(UkAddressInUserAnswers, testAddressUk)
            .withPage(IndividualEmailPage, testEmail)
            .withPage(IndividualHavePhonePage, true)
            .withPage(IndividualPhonePage, testPhone)

          val result: Option[CreateRcaspRequest] = rcaspSubmissionHelper.createRcaspRequest(carfId, userAnswers)

          result mustBe Some(
            CreateRcaspRequest(
              RCASPManagementRequest(
                RequestCommon = rcaspCreateRequestCommon,
                RequestDetails = individualRcaspDetails
              )
            )
          )
        }

        "must build the request without optional phone number" in {
          val userAnswers = emptyUserAnswers
            .withPage(OrganisationOrIndividualPage, Individual)
            .withPage(IndividualNamePage, testIndividualName)
            .withPage(NiNumberPage, testNiNumber)
            .withPage(UkAddressInUserAnswers, testAddressUk)
            .withPage(IndividualEmailPage, testEmail)
            .withPage(IndividualHavePhonePage, false)

          val result: Option[CreateRcaspRequest] = rcaspSubmissionHelper.createRcaspRequest(carfId, userAnswers)

          result mustBe Some(
            CreateRcaspRequest(
              RCASPManagementRequest(
                RequestCommon = rcaspCreateRequestCommon,
                RequestDetails = individualRcaspDetails.copy(
                  PrimaryContactDetails = Some(individualContactDetails.copy(PhoneNumber = None))
                )
              )
            )
          )
        }

        "must return None when NINO is missing" in {
          val userAnswers = emptyUserAnswers
            .withPage(ReportForRegisteredBusinessPage, false)
            .withPage(OrganisationOrIndividualPage, Individual)
            .withPage(IndividualNamePage, testIndividualName)
            .withPage(UkAddressInUserAnswers, testAddressUk)
            .withPage(IndividualEmailPage, testEmail)
            .withPage(IndividualHavePhonePage, true)
            .withPage(IndividualPhonePage, testPhone)

          val result: Option[CreateRcaspRequest] = rcaspSubmissionHelper.createRcaspRequest(carfId, userAnswers)

          result mustBe None
        }

        "must return None when email is missing" in {
          val userAnswers = emptyUserAnswers
            .withPage(ReportForRegisteredBusinessPage, false)
            .withPage(OrganisationOrIndividualPage, Individual)
            .withPage(IndividualNamePage, testIndividualName)
            .withPage(NiNumberPage, testNiNumber)
            .withPage(UkAddressInUserAnswers, testAddressUk)
            .withPage(IndividualHavePhonePage, true)
            .withPage(IndividualPhonePage, testPhone)

          val result: Option[CreateRcaspRequest] = rcaspSubmissionHelper.createRcaspRequest(carfId, userAnswers)

          result mustBe None
        }

        "must return None when havePhone is true but phone number is missing" in {
          val userAnswers = emptyUserAnswers
            .withPage(ReportForRegisteredBusinessPage, false)
            .withPage(OrganisationOrIndividualPage, Individual)
            .withPage(IndividualNamePage, testIndividualName)
            .withPage(NiNumberPage, testNiNumber)
            .withPage(UkAddressInUserAnswers, testAddressUk)
            .withPage(IndividualEmailPage, testEmail)
            .withPage(IndividualHavePhonePage, true)

          val result: Option[CreateRcaspRequest] = rcaspSubmissionHelper.createRcaspRequest(carfId, userAnswers)

          result mustBe None
        }

        "must return None when reportForRegisteredBusiness is true" in {
          val userAnswers = emptyUserAnswers
            .withPage(ReportForRegisteredBusinessPage, true)
            .withPage(OrganisationOrIndividualPage, Individual)
            .withPage(IndividualNamePage, testIndividualName)
            .withPage(NiNumberPage, testNiNumber)
            .withPage(UkAddressInUserAnswers, testAddressUk)
            .withPage(IndividualEmailPage, testEmail)
            .withPage(IndividualHavePhonePage, false)

          val result: Option[CreateRcaspRequest] = rcaspSubmissionHelper.createRcaspRequest(carfId, userAnswers)

          result mustBe None
        }
      }

      "for an Organisation" - {
        "must build the request successfully with all required fields" in {
          val userAnswers = emptyUserAnswers
            .withPage(ReportForRegisteredBusinessPage, false)
            .withPage(OrganisationOrIndividualPage, Organisation)
            .withPage(OverwritableOrganisationName, testOrgName)
            .withPage(HaveTradingNamePage, true)
            .withPage(TradingNamePage, testTradingName)
            .withPage(UtrPage, testUtr.uniqueTaxPayerReference)
            .withPage(UkAddressInUserAnswers, testAddressUk)
            .withPage(OrganisationFirstContactNamePage, testOrgContactName)
            .withPage(OrganisationFirstContactEmailPage, testEmail)
            .withPage(OrganisationFirstContactHavePhonePage, true)
            .withPage(OrganisationFirstContactPhoneNumberPage, testPhone)
            .withPage(OrganisationHaveSecondContactPage, true)
            .withPage(OrganisationSecondContactNamePage, testOrgContactName)
            .withPage(OrganisationSecondContactEmailPage, testEmail)
            .withPage(OrganisationSecondContactHavePhonePage, true)
            .withPage(OrganisationSecondContactPhoneNumberPage, testPhone)

          val result: Option[CreateRcaspRequest] = rcaspSubmissionHelper.createRcaspRequest(carfId, userAnswers)

          result mustBe Some(
            CreateRcaspRequest(
              RCASPManagementRequest(
                RequestCommon = rcaspCreateRequestCommon,
                RequestDetails = organisationRcaspDetails
              )
            )
          )
        }

        "must build the request without trading name, first contact phone and second contact" in {
          val userAnswers = emptyUserAnswers
            .withPage(OrganisationOrIndividualPage, Organisation)
            .withPage(OverwritableOrganisationName, testOrgName)
            .withPage(HaveTradingNamePage, false)
            .withPage(UtrPage, testUtr.uniqueTaxPayerReference)
            .withPage(UkAddressInUserAnswers, testAddressUk)
            .withPage(OrganisationFirstContactNamePage, testOrgContactName)
            .withPage(OrganisationFirstContactEmailPage, testEmail)
            .withPage(OrganisationFirstContactHavePhonePage, false)
            .withPage(OrganisationHaveSecondContactPage, false)

          val result: Option[CreateRcaspRequest] = rcaspSubmissionHelper.createRcaspRequest(carfId, userAnswers)

          result mustBe Some(
            CreateRcaspRequest(
              RCASPManagementRequest(
                RequestCommon = rcaspCreateRequestCommon,
                RequestDetails = organisationRcaspDetails.copy(
                  TradingName = testOrgName,
                  PrimaryContactDetails = Some(organisationContactDetails.copy(PhoneNumber = None)),
                  SecondaryContactDetails = None
                )
              )
            )
          )
        }

        "must return None when organisation name is missing" in {
          val userAnswers = emptyUserAnswers
            .withPage(ReportForRegisteredBusinessPage, false)
            .withPage(OrganisationOrIndividualPage, Organisation)
            .withPage(HaveTradingNamePage, false)
            .withPage(UtrPage, testUtr.uniqueTaxPayerReference)
            .withPage(UkAddressInUserAnswers, testAddressUk)
            .withPage(OrganisationFirstContactNamePage, testOrgContactName)
            .withPage(OrganisationFirstContactEmailPage, testEmail)
            .withPage(OrganisationFirstContactHavePhonePage, false)
            .withPage(OrganisationHaveSecondContactPage, false)

          val result: Option[CreateRcaspRequest] = rcaspSubmissionHelper.createRcaspRequest(carfId, userAnswers)

          result mustBe None
        }

        "must return None when haveTradingName is true but trading name is missing" in {
          val userAnswers = emptyUserAnswers
            .withPage(ReportForRegisteredBusinessPage, false)
            .withPage(OrganisationOrIndividualPage, Organisation)
            .withPage(OverwritableOrganisationName, testOrgName)
            .withPage(HaveTradingNamePage, true)
            .withPage(UtrPage, testUtr.uniqueTaxPayerReference)
            .withPage(UkAddressInUserAnswers, testAddressUk)
            .withPage(OrganisationFirstContactNamePage, testOrgContactName)
            .withPage(OrganisationFirstContactEmailPage, testEmail)
            .withPage(OrganisationFirstContactHavePhonePage, true)
            .withPage(OrganisationFirstContactPhoneNumberPage, testPhone)
            .withPage(OrganisationHaveSecondContactPage, true)
            .withPage(OrganisationSecondContactNamePage, testOrgContactName)
            .withPage(OrganisationSecondContactEmailPage, testEmail)
            .withPage(OrganisationSecondContactHavePhonePage, true)
            .withPage(OrganisationSecondContactPhoneNumberPage, testPhone)

          val result: Option[CreateRcaspRequest] = rcaspSubmissionHelper.createRcaspRequest(carfId, userAnswers)

          result mustBe None
        }

        "must return None when first contact name is missing" in {
          val userAnswers = emptyUserAnswers
            .withPage(ReportForRegisteredBusinessPage, false)
            .withPage(OrganisationOrIndividualPage, Organisation)
            .withPage(OverwritableOrganisationName, testOrgName)
            .withPage(HaveTradingNamePage, false)
            .withPage(UtrPage, testUtr.uniqueTaxPayerReference)
            .withPage(UkAddressInUserAnswers, testAddressUk)
            .withPage(OrganisationFirstContactEmailPage, testEmail)
            .withPage(OrganisationFirstContactHavePhonePage, false)
            .withPage(OrganisationHaveSecondContactPage, false)

          val result: Option[CreateRcaspRequest] = rcaspSubmissionHelper.createRcaspRequest(carfId, userAnswers)

          result mustBe None
        }

        "must return None when haveSecondContact is true but second contact email is missing" in {
          val userAnswers = emptyUserAnswers
            .withPage(ReportForRegisteredBusinessPage, false)
            .withPage(OrganisationOrIndividualPage, Organisation)
            .withPage(OverwritableOrganisationName, testOrgName)
            .withPage(HaveTradingNamePage, true)
            .withPage(TradingNamePage, testTradingName)
            .withPage(UtrPage, testUtr.uniqueTaxPayerReference)
            .withPage(UkAddressInUserAnswers, testAddressUk)
            .withPage(OrganisationFirstContactNamePage, testOrgContactName)
            .withPage(OrganisationFirstContactEmailPage, testEmail)
            .withPage(OrganisationFirstContactHavePhonePage, true)
            .withPage(OrganisationFirstContactPhoneNumberPage, testPhone)
            .withPage(OrganisationHaveSecondContactPage, true)
            .withPage(OrganisationSecondContactNamePage, testOrgContactName)
            .withPage(OrganisationSecondContactHavePhonePage, true)
            .withPage(OrganisationSecondContactPhoneNumberPage, testPhone)

          val result: Option[CreateRcaspRequest] = rcaspSubmissionHelper.createRcaspRequest(carfId, userAnswers)

          result mustBe None
        }

        "must return None when secondContactHavePhone is true but second contact phone number is missing" in {
          val userAnswers = emptyUserAnswers
            .withPage(ReportForRegisteredBusinessPage, false)
            .withPage(OrganisationOrIndividualPage, Organisation)
            .withPage(OverwritableOrganisationName, testOrgName)
            .withPage(HaveTradingNamePage, true)
            .withPage(TradingNamePage, testTradingName)
            .withPage(UtrPage, testUtr.uniqueTaxPayerReference)
            .withPage(UkAddressInUserAnswers, testAddressUk)
            .withPage(OrganisationFirstContactNamePage, testOrgContactName)
            .withPage(OrganisationFirstContactEmailPage, testEmail)
            .withPage(OrganisationFirstContactHavePhonePage, true)
            .withPage(OrganisationFirstContactPhoneNumberPage, testPhone)
            .withPage(OrganisationHaveSecondContactPage, true)
            .withPage(OrganisationSecondContactNamePage, testOrgContactName)
            .withPage(OrganisationSecondContactEmailPage, testEmail)
            .withPage(OrganisationSecondContactHavePhonePage, true)

          val result: Option[CreateRcaspRequest] = rcaspSubmissionHelper.createRcaspRequest(carfId, userAnswers)

          result mustBe None
        }

        "must return None when reportForRegisteredBusiness is true" in {
          val userAnswers = emptyUserAnswers
            .withPage(ReportForRegisteredBusinessPage, true)
            .withPage(OrganisationOrIndividualPage, Organisation)
            .withPage(OverwritableOrganisationName, testOrgName)
            .withPage(HaveTradingNamePage, false)
            .withPage(UtrPage, testUtr.uniqueTaxPayerReference)
            .withPage(UkAddressInUserAnswers, testAddressUk)
            .withPage(OrganisationFirstContactNamePage, testOrgContactName)
            .withPage(OrganisationFirstContactEmailPage, testEmail)
            .withPage(OrganisationFirstContactHavePhonePage, false)
            .withPage(OrganisationHaveSecondContactPage, false)

          val result: Option[CreateRcaspRequest] = rcaspSubmissionHelper.createRcaspRequest(carfId, userAnswers)

          result mustBe None
        }
      }

      "must return None when OrganisationOrIndividual is missing" in {
        val userAnswers = emptyUserAnswers
          .withPage(ReportForRegisteredBusinessPage, false)
          .withPage(IndividualNamePage, testIndividualName)
          .withPage(NiNumberPage, testNiNumber)
          .withPage(UkAddressInUserAnswers, testAddressUk)
          .withPage(IndividualEmailPage, testEmail)
          .withPage(IndividualHavePhonePage, true)
          .withPage(IndividualPhonePage, testPhone)

        val result: Option[CreateRcaspRequest] = rcaspSubmissionHelper.createRcaspRequest(carfId, userAnswers)

        result mustBe None
      }
    }
  }
}
