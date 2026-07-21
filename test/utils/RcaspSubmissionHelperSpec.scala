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
import models.requests.createRcasp.RcaspRequest as CreateRcaspRequest
import models.requests.updateRcasp.RcaspRequest as UpdateRcaspRequest
import models.requests.{createRcasp, updateRcasp, RequestType}
import models.{RcaspAddress, RcaspContactDetails, TinDetails}
import pages.UkAddressInUserAnswers
import pages.changeDetails.ChangeRcaspCachedDetails
import pages.combined.OrganisationOrIndividualPage
import pages.individual.*
import pages.organisation.*

class RcaspSubmissionHelperSpec extends SpecBase {

  val rcaspSubmissionHelper: RcaspSubmissionHelper = new RcaspSubmissionHelper

  "RcaspSubmissionHelper" - {
    ".createRcaspRequestForRegisteredBusiness" - {
      "must build the request successfully with all required fields" in {
        val userAnswers = emptyUserAnswers
          .withPage(ReportForRegisteredBusinessPage, true)
          .withPage(OverwritableOrganisationName, testOrgName)
          .withPage(HaveTradingNamePage, true)
          .withPage(TradingNamePage, testTradingName)
          .withPage(UkAddressInUserAnswers, testAddressUk)

        val result: Option[CreateRcaspRequest] =
          rcaspSubmissionHelper.createRcaspRequestForRegisteredBusiness(carfId, testUtr, userAnswers)

        result mustBe Some(
          CreateRcaspRequest(
            createRcasp.RcaspManagementRequest(
              RequestCommon = rcaspRequestCommon(RequestType.Create),
              RequestDetails = registeredBusinessRcaspDetailsRequest
            )
          )
        )
      }

      "must build the request without trading name" in {
        val userAnswers = emptyUserAnswers
          .withPage(ReportForRegisteredBusinessPage, true)
          .withPage(OverwritableOrganisationName, testOrgName)
          .withPage(HaveTradingNamePage, false)
          .withPage(UkAddressInUserAnswers, testAddressUk)

        val result: Option[CreateRcaspRequest] =
          rcaspSubmissionHelper.createRcaspRequestForRegisteredBusiness(carfId, testUtr, userAnswers)

        result mustBe Some(
          CreateRcaspRequest(
            createRcasp.RcaspManagementRequest(
              RequestCommon = rcaspRequestCommon(RequestType.Create),
              RequestDetails = registeredBusinessRcaspDetailsRequest.copy(TradingName = testOrgName)
            )
          )
        )
      }

      "must return None when organisation name is missing" in {
        val userAnswers = emptyUserAnswers
          .withPage(ReportForRegisteredBusinessPage, true)
          .withPage(HaveTradingNamePage, true)
          .withPage(TradingNamePage, testTradingName)
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
          .withPage(UkAddressInUserAnswers, testAddressUk)

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
              createRcasp.RcaspManagementRequest(
                RequestCommon = rcaspRequestCommon(RequestType.Create),
                RequestDetails = individualRcaspDetailsRequest
              )
            )
          )
        }

        "must build the request without optional phone number and when reportForRegisteredBusiness is None" in {
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
              createRcasp.RcaspManagementRequest(
                RequestCommon = rcaspRequestCommon(RequestType.Create),
                RequestDetails = individualRcaspDetailsRequest.copy(
                  PrimaryContactDetails = Some(rcaspContactDetails.copy(PhoneNumber = None))
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
            .withPage(OrganisationFirstContactNamePage, testIndividualName.fullName)
            .withPage(OrganisationFirstContactEmailPage, testEmail)
            .withPage(OrganisationFirstContactHavePhonePage, true)
            .withPage(OrganisationFirstContactPhoneNumberPage, testPhone)
            .withPage(OrganisationHaveSecondContactPage, true)
            .withPage(OrganisationSecondContactNamePage, "Prof Turo")
            .withPage(OrganisationSecondContactEmailPage, testEmail)
            .withPage(OrganisationSecondContactHavePhonePage, true)
            .withPage(OrganisationSecondContactPhoneNumberPage, testPhone)

          val result: Option[CreateRcaspRequest] = rcaspSubmissionHelper.createRcaspRequest(carfId, userAnswers)

          result mustBe Some(
            CreateRcaspRequest(
              createRcasp.RcaspManagementRequest(
                RequestCommon = rcaspRequestCommon(RequestType.Create),
                RequestDetails = organisationRcaspDetailsRequest
              )
            )
          )
        }

        "must build the request successfully without second contact phone and when reportForRegisteredBusiness is None" in {
          val userAnswers = emptyUserAnswers
            .withPage(OrganisationOrIndividualPage, Organisation)
            .withPage(OverwritableOrganisationName, testOrgName)
            .withPage(HaveTradingNamePage, true)
            .withPage(TradingNamePage, testTradingName)
            .withPage(UtrPage, testUtr.uniqueTaxPayerReference)
            .withPage(UkAddressInUserAnswers, testAddressUk)
            .withPage(OrganisationFirstContactNamePage, testIndividualName.fullName)
            .withPage(OrganisationFirstContactEmailPage, testEmail)
            .withPage(OrganisationFirstContactHavePhonePage, true)
            .withPage(OrganisationFirstContactPhoneNumberPage, testPhone)
            .withPage(OrganisationHaveSecondContactPage, true)
            .withPage(OrganisationSecondContactNamePage, testIndividualName.fullName)
            .withPage(OrganisationSecondContactEmailPage, testEmail)
            .withPage(OrganisationSecondContactHavePhonePage, false)

          val result: Option[CreateRcaspRequest] = rcaspSubmissionHelper.createRcaspRequest(carfId, userAnswers)

          result mustBe Some(
            CreateRcaspRequest(
              createRcasp.RcaspManagementRequest(
                RequestCommon = rcaspRequestCommon(RequestType.Create),
                RequestDetails = organisationRcaspDetailsRequest.copy(
                  SecondaryContactDetails = Some(rcaspContactDetails.copy(PhoneNumber = None))
                )
              )
            )
          )
        }

        "must build the request without trading name, first contact phone and second contact, when reportForRegisteredBusiness is None" in {
          val userAnswers = emptyUserAnswers
            .withPage(OrganisationOrIndividualPage, Organisation)
            .withPage(OverwritableOrganisationName, testOrgName)
            .withPage(HaveTradingNamePage, false)
            .withPage(UtrPage, testUtr.uniqueTaxPayerReference)
            .withPage(UkAddressInUserAnswers, testAddressUk)
            .withPage(OrganisationFirstContactNamePage, testIndividualName.fullName)
            .withPage(OrganisationFirstContactEmailPage, testEmail)
            .withPage(OrganisationFirstContactHavePhonePage, false)
            .withPage(OrganisationHaveSecondContactPage, false)

          val result: Option[CreateRcaspRequest] = rcaspSubmissionHelper.createRcaspRequest(carfId, userAnswers)

          result mustBe Some(
            CreateRcaspRequest(
              createRcasp.RcaspManagementRequest(
                RequestCommon = rcaspRequestCommon(RequestType.Create),
                RequestDetails = organisationRcaspDetailsRequest.copy(
                  TradingName = testOrgName,
                  PrimaryContactDetails = Some(rcaspContactDetails.copy(PhoneNumber = None)),
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
            .withPage(OrganisationFirstContactNamePage, testIndividualName.fullName)
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
            .withPage(OrganisationFirstContactNamePage, testIndividualName.fullName)
            .withPage(OrganisationFirstContactEmailPage, testEmail)
            .withPage(OrganisationFirstContactHavePhonePage, true)
            .withPage(OrganisationFirstContactPhoneNumberPage, testPhone)
            .withPage(OrganisationHaveSecondContactPage, true)
            .withPage(OrganisationSecondContactNamePage, testIndividualName.fullName)
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
            .withPage(OrganisationFirstContactNamePage, testIndividualName.fullName)
            .withPage(OrganisationFirstContactEmailPage, testEmail)
            .withPage(OrganisationFirstContactHavePhonePage, true)
            .withPage(OrganisationFirstContactPhoneNumberPage, testPhone)
            .withPage(OrganisationHaveSecondContactPage, true)
            .withPage(OrganisationSecondContactNamePage, testIndividualName.fullName)
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
            .withPage(OrganisationFirstContactNamePage, testIndividualName.fullName)
            .withPage(OrganisationFirstContactEmailPage, testEmail)
            .withPage(OrganisationFirstContactHavePhonePage, true)
            .withPage(OrganisationFirstContactPhoneNumberPage, testPhone)
            .withPage(OrganisationHaveSecondContactPage, true)
            .withPage(OrganisationSecondContactNamePage, testIndividualName.fullName)
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
            .withPage(OrganisationFirstContactNamePage, testIndividualName.fullName)
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

    ".updateRcaspRequestForRegisteredBusiness" - {
      "must build the request successfully with all required fields" in {
        val userAnswers = emptyUserAnswers
          .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate.copy(IsRCASPUser = true))
          .withPage(ReportForRegisteredBusinessPage, true)
          .withPage(OverwritableOrganisationName, testOrgName)
          .withPage(HaveTradingNamePage, true)
          .withPage(TradingNamePage, testTradingName)
          .withPage(UkAddressInUserAnswers, testAddressUk)

        val result: Option[UpdateRcaspRequest] =
          rcaspSubmissionHelper.updateRcaspRequestForRegisteredBusiness(carfId, testUtr, userAnswers)

        result mustBe Some(
          UpdateRcaspRequest(
            updateRcasp.RcaspManagementRequest(
              RequestCommon = rcaspRequestCommon(RequestType.Update),
              RequestDetails = registeredBusinessUpdateRcaspDetailsRequest
            )
          )
        )
      }

      "must build the request without trading name" in {
        val userAnswers = emptyUserAnswers
          .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate.copy(IsRCASPUser = true))
          .withPage(ReportForRegisteredBusinessPage, true)
          .withPage(OverwritableOrganisationName, testOrgName)
          .withPage(HaveTradingNamePage, false)
          .withPage(UkAddressInUserAnswers, testAddressUk)

        val result: Option[UpdateRcaspRequest] =
          rcaspSubmissionHelper.updateRcaspRequestForRegisteredBusiness(carfId, testUtr, userAnswers)

        result mustBe Some(
          UpdateRcaspRequest(
            updateRcasp.RcaspManagementRequest(
              RequestCommon = rcaspRequestCommon(RequestType.Update),
              RequestDetails = registeredBusinessUpdateRcaspDetailsRequest.copy(TradingName = testOrgName)
            )
          )
        )
      }

      "must return None when ChangeRcaspCachedDetails is missing" in {
        val userAnswers = emptyUserAnswers
          .withPage(ReportForRegisteredBusinessPage, true)
          .withPage(OverwritableOrganisationName, testOrgName)
          .withPage(HaveTradingNamePage, true)
          .withPage(TradingNamePage, testTradingName)
          .withPage(UkAddressInUserAnswers, testAddressUk)

        val result: Option[UpdateRcaspRequest] =
          rcaspSubmissionHelper.updateRcaspRequestForRegisteredBusiness(carfId, testUtr, userAnswers)

        result mustBe None
      }

      "must return None when organisation name is missing" in {
        val userAnswers = emptyUserAnswers
          .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate.copy(IsRCASPUser = true))
          .withPage(ReportForRegisteredBusinessPage, true)
          .withPage(HaveTradingNamePage, true)
          .withPage(TradingNamePage, testTradingName)
          .withPage(UkAddressInUserAnswers, testAddressUk)

        val result: Option[UpdateRcaspRequest] =
          rcaspSubmissionHelper.updateRcaspRequestForRegisteredBusiness(carfId, testUtr, userAnswers)

        result mustBe None
      }

      "must return None when haveTradingName is true but trading name is missing" in {
        val userAnswers = emptyUserAnswers
          .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate.copy(IsRCASPUser = true))
          .withPage(ReportForRegisteredBusinessPage, true)
          .withPage(OverwritableOrganisationName, testOrgName)
          .withPage(HaveTradingNamePage, true)
          .withPage(UkAddressInUserAnswers, testAddressUk)

        val result: Option[UpdateRcaspRequest] =
          rcaspSubmissionHelper.updateRcaspRequestForRegisteredBusiness(carfId, testUtr, userAnswers)

        result mustBe None
      }

      "must return None when reportForRegisteredBusiness is not true" - {
        "when reportForRegisteredBusiness is false" - {
          val userAnswers = emptyUserAnswers
            .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate.copy(IsRCASPUser = true))
            .withPage(ReportForRegisteredBusinessPage, false)
            .withPage(OverwritableOrganisationName, testOrgName)
            .withPage(HaveTradingNamePage, true)
            .withPage(TradingNamePage, testTradingName)
            .withPage(RegisteredBusinessIsTheAddressCorrectPage, false)
            .withPage(UkAddressInUserAnswers, testAddressUk)

          val result: Option[UpdateRcaspRequest] =
            rcaspSubmissionHelper.updateRcaspRequestForRegisteredBusiness(carfId, testUtr, userAnswers)

          result mustBe None
        }

        "when reportForRegisteredBusiness is missing" - {
          val userAnswers = emptyUserAnswers
            .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate.copy(IsRCASPUser = true))
            .withPage(OverwritableOrganisationName, testOrgName)
            .withPage(HaveTradingNamePage, true)
            .withPage(TradingNamePage, testTradingName)
            .withPage(RegisteredBusinessIsTheAddressCorrectPage, false)
            .withPage(UkAddressInUserAnswers, testAddressUk)

          val result: Option[UpdateRcaspRequest] =
            rcaspSubmissionHelper.updateRcaspRequestForRegisteredBusiness(carfId, testUtr, userAnswers)

          result mustBe None
        }
      }
    }

    ".updateRcaspRequest" - {
      "for an Individual" - {
        "must build the request successfully with all required fields" in {
          val userAnswers = emptyUserAnswers
            .withPage(ChangeRcaspCachedDetails, individualRcaspDetailsViewUpdate)
            .withPage(ReportForRegisteredBusinessPage, false)
            .withPage(OrganisationOrIndividualPage, Individual)
            .withPage(IndividualNamePage, testIndividualName)
            .withPage(NiNumberPage, testNiNumber)
            .withPage(UkAddressInUserAnswers, testAddressUk)
            .withPage(IndividualEmailPage, testEmail)
            .withPage(IndividualHavePhonePage, true)
            .withPage(IndividualPhonePage, testPhone)

          val result: Option[UpdateRcaspRequest] = rcaspSubmissionHelper.updateRcaspRequest(carfId, userAnswers)

          result mustBe Some(
            UpdateRcaspRequest(
              updateRcasp.RcaspManagementRequest(
                RequestCommon = rcaspRequestCommon(RequestType.Update),
                RequestDetails = individualRcaspDetailsViewUpdate
              )
            )
          )
        }

        "must build the request without optional phone number and when reportForRegisteredBusiness is None" in {
          val userAnswers = emptyUserAnswers
            .withPage(ChangeRcaspCachedDetails, individualRcaspDetailsViewUpdate)
            .withPage(ReportForRegisteredBusinessPage, false)
            .withPage(OrganisationOrIndividualPage, Individual)
            .withPage(IndividualNamePage, testIndividualName)
            .withPage(NiNumberPage, testNiNumber)
            .withPage(UkAddressInUserAnswers, testAddressUk)
            .withPage(IndividualEmailPage, testEmail)
            .withPage(IndividualHavePhonePage, false)

          val result: Option[UpdateRcaspRequest] = rcaspSubmissionHelper.updateRcaspRequest(carfId, userAnswers)

          result mustBe Some(
            UpdateRcaspRequest(
              updateRcasp.RcaspManagementRequest(
                RequestCommon = rcaspRequestCommon(RequestType.Update),
                RequestDetails = individualRcaspDetailsViewUpdate.copy(
                  PrimaryContactDetails = Some(rcaspContactDetails.copy(PhoneNumber = None))
                )
              )
            )
          )
        }

        "must return None when ChangeRcaspCachedDetails is missing" in {
          val userAnswers = emptyUserAnswers
            .withPage(ReportForRegisteredBusinessPage, false)
            .withPage(OrganisationOrIndividualPage, Individual)
            .withPage(IndividualNamePage, testIndividualName)
            .withPage(NiNumberPage, testNiNumber)
            .withPage(UkAddressInUserAnswers, testAddressUk)
            .withPage(IndividualEmailPage, testEmail)
            .withPage(IndividualHavePhonePage, true)
            .withPage(IndividualPhonePage, testPhone)

          val result: Option[UpdateRcaspRequest] = rcaspSubmissionHelper.updateRcaspRequest(carfId, userAnswers)

          result mustBe None
        }

        "must return None when NINO is missing" in {
          val userAnswers = emptyUserAnswers
            .withPage(ChangeRcaspCachedDetails, individualRcaspDetailsViewUpdate)
            .withPage(ReportForRegisteredBusinessPage, false)
            .withPage(OrganisationOrIndividualPage, Individual)
            .withPage(IndividualNamePage, testIndividualName)
            .withPage(UkAddressInUserAnswers, testAddressUk)
            .withPage(IndividualEmailPage, testEmail)
            .withPage(IndividualHavePhonePage, true)
            .withPage(IndividualPhonePage, testPhone)

          val result: Option[UpdateRcaspRequest] = rcaspSubmissionHelper.updateRcaspRequest(carfId, userAnswers)

          result mustBe None
        }

        "must return None when email is missing" in {
          val userAnswers = emptyUserAnswers
            .withPage(ChangeRcaspCachedDetails, individualRcaspDetailsViewUpdate)
            .withPage(ReportForRegisteredBusinessPage, false)
            .withPage(OrganisationOrIndividualPage, Individual)
            .withPage(IndividualNamePage, testIndividualName)
            .withPage(NiNumberPage, testNiNumber)
            .withPage(UkAddressInUserAnswers, testAddressUk)
            .withPage(IndividualHavePhonePage, true)
            .withPage(IndividualPhonePage, testPhone)

          val result: Option[UpdateRcaspRequest] = rcaspSubmissionHelper.updateRcaspRequest(carfId, userAnswers)

          result mustBe None
        }

        "must return None when havePhone is true but phone number is missing" in {
          val userAnswers = emptyUserAnswers
            .withPage(ChangeRcaspCachedDetails, individualRcaspDetailsViewUpdate)
            .withPage(ReportForRegisteredBusinessPage, false)
            .withPage(OrganisationOrIndividualPage, Individual)
            .withPage(IndividualNamePage, testIndividualName)
            .withPage(NiNumberPage, testNiNumber)
            .withPage(UkAddressInUserAnswers, testAddressUk)
            .withPage(IndividualEmailPage, testEmail)
            .withPage(IndividualHavePhonePage, true)

          val result: Option[UpdateRcaspRequest] = rcaspSubmissionHelper.updateRcaspRequest(carfId, userAnswers)

          result mustBe None
        }

        "must return None when reportForRegisteredBusiness is not false" - {
          "when reportForRegisteredBusiness is true" - {
            val userAnswers = emptyUserAnswers
              .withPage(ChangeRcaspCachedDetails, individualRcaspDetailsViewUpdate)
              .withPage(ReportForRegisteredBusinessPage, true)
              .withPage(OrganisationOrIndividualPage, Individual)
              .withPage(IndividualNamePage, testIndividualName)
              .withPage(NiNumberPage, testNiNumber)
              .withPage(UkAddressInUserAnswers, testAddressUk)
              .withPage(IndividualEmailPage, testEmail)
              .withPage(IndividualHavePhonePage, false)

            val result: Option[UpdateRcaspRequest] = rcaspSubmissionHelper.updateRcaspRequest(carfId, userAnswers)

            result mustBe None
          }

          "when reportForRegisteredBusiness is missing" - {
            val userAnswers = emptyUserAnswers
              .withPage(ChangeRcaspCachedDetails, individualRcaspDetailsViewUpdate)
              .withPage(OrganisationOrIndividualPage, Individual)
              .withPage(IndividualNamePage, testIndividualName)
              .withPage(NiNumberPage, testNiNumber)
              .withPage(UkAddressInUserAnswers, testAddressUk)
              .withPage(IndividualEmailPage, testEmail)
              .withPage(IndividualHavePhonePage, false)

            val result: Option[UpdateRcaspRequest] = rcaspSubmissionHelper.updateRcaspRequest(carfId, userAnswers)

            result mustBe None
          }
        }
      }

      "for an Organisation" - {
        "must build the request successfully with all required fields" in {
          val userAnswers = emptyUserAnswers
            .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate)
            .withPage(ReportForRegisteredBusinessPage, false)
            .withPage(OrganisationOrIndividualPage, Organisation)
            .withPage(OverwritableOrganisationName, testOrgName)
            .withPage(HaveTradingNamePage, true)
            .withPage(TradingNamePage, testTradingName)
            .withPage(UtrPage, testUtr.uniqueTaxPayerReference)
            .withPage(UkAddressInUserAnswers, testAddressUk)
            .withPage(OrganisationFirstContactNamePage, testIndividualName.fullName)
            .withPage(OrganisationFirstContactEmailPage, testEmail)
            .withPage(OrganisationFirstContactHavePhonePage, true)
            .withPage(OrganisationFirstContactPhoneNumberPage, testPhone)
            .withPage(OrganisationHaveSecondContactPage, true)
            .withPage(OrganisationSecondContactNamePage, "Prof Turo")
            .withPage(OrganisationSecondContactEmailPage, testEmail)
            .withPage(OrganisationSecondContactHavePhonePage, true)
            .withPage(OrganisationSecondContactPhoneNumberPage, testPhone)

          val result: Option[UpdateRcaspRequest] = rcaspSubmissionHelper.updateRcaspRequest(carfId, userAnswers)

          result mustBe Some(
            UpdateRcaspRequest(
              updateRcasp.RcaspManagementRequest(
                RequestCommon = rcaspRequestCommon(RequestType.Update),
                RequestDetails = organisationRcaspDetailsViewUpdate
              )
            )
          )
        }

        "must build the request successfully without second contact phone" in {
          val userAnswers = emptyUserAnswers
            .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate)
            .withPage(ReportForRegisteredBusinessPage, false)
            .withPage(OrganisationOrIndividualPage, Organisation)
            .withPage(OverwritableOrganisationName, testOrgName)
            .withPage(HaveTradingNamePage, true)
            .withPage(TradingNamePage, testTradingName)
            .withPage(UtrPage, testUtr.uniqueTaxPayerReference)
            .withPage(UkAddressInUserAnswers, testAddressUk)
            .withPage(OrganisationFirstContactNamePage, testIndividualName.fullName)
            .withPage(OrganisationFirstContactEmailPage, testEmail)
            .withPage(OrganisationFirstContactHavePhonePage, true)
            .withPage(OrganisationFirstContactPhoneNumberPage, testPhone)
            .withPage(OrganisationHaveSecondContactPage, true)
            .withPage(OrganisationSecondContactNamePage, testIndividualName.fullName)
            .withPage(OrganisationSecondContactEmailPage, testEmail)
            .withPage(OrganisationSecondContactHavePhonePage, false)

          val result: Option[UpdateRcaspRequest] = rcaspSubmissionHelper.updateRcaspRequest(carfId, userAnswers)

          result mustBe Some(
            UpdateRcaspRequest(
              updateRcasp.RcaspManagementRequest(
                RequestCommon = rcaspRequestCommon(RequestType.Update),
                RequestDetails = organisationRcaspDetailsViewUpdate.copy(
                  SecondaryContactDetails = Some(rcaspContactDetails.copy(PhoneNumber = None))
                )
              )
            )
          )
        }

        "must build the request without trading name, first contact phone and second contact" in {
          val userAnswers = emptyUserAnswers
            .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate)
            .withPage(ReportForRegisteredBusinessPage, false)
            .withPage(OrganisationOrIndividualPage, Organisation)
            .withPage(OverwritableOrganisationName, testOrgName)
            .withPage(HaveTradingNamePage, false)
            .withPage(UtrPage, testUtr.uniqueTaxPayerReference)
            .withPage(UkAddressInUserAnswers, testAddressUk)
            .withPage(OrganisationFirstContactNamePage, testIndividualName.fullName)
            .withPage(OrganisationFirstContactEmailPage, testEmail)
            .withPage(OrganisationFirstContactHavePhonePage, false)
            .withPage(OrganisationHaveSecondContactPage, false)

          val result: Option[UpdateRcaspRequest] = rcaspSubmissionHelper.updateRcaspRequest(carfId, userAnswers)

          result mustBe Some(
            UpdateRcaspRequest(
              updateRcasp.RcaspManagementRequest(
                RequestCommon = rcaspRequestCommon(RequestType.Update),
                RequestDetails = organisationRcaspDetailsViewUpdate.copy(
                  TradingName = testOrgName,
                  PrimaryContactDetails = Some(rcaspContactDetails.copy(PhoneNumber = None)),
                  SecondaryContactDetails = None
                )
              )
            )
          )
        }

        "must return None when ChangeRcaspCachedDetails is missing" in {
          val userAnswers = emptyUserAnswers
            .withPage(ReportForRegisteredBusinessPage, false)
            .withPage(OrganisationOrIndividualPage, Organisation)
            .withPage(OverwritableOrganisationName, testOrgName)
            .withPage(HaveTradingNamePage, false)
            .withPage(UtrPage, testUtr.uniqueTaxPayerReference)
            .withPage(UkAddressInUserAnswers, testAddressUk)
            .withPage(OrganisationFirstContactNamePage, testIndividualName.fullName)
            .withPage(OrganisationFirstContactEmailPage, testEmail)
            .withPage(OrganisationFirstContactHavePhonePage, false)
            .withPage(OrganisationHaveSecondContactPage, false)

          val result: Option[UpdateRcaspRequest] = rcaspSubmissionHelper.updateRcaspRequest(carfId, userAnswers)

          result mustBe None
        }

        "must return None when organisation name is missing" in {
          val userAnswers = emptyUserAnswers
            .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate)
            .withPage(ReportForRegisteredBusinessPage, false)
            .withPage(OrganisationOrIndividualPage, Organisation)
            .withPage(HaveTradingNamePage, false)
            .withPage(UtrPage, testUtr.uniqueTaxPayerReference)
            .withPage(UkAddressInUserAnswers, testAddressUk)
            .withPage(OrganisationFirstContactNamePage, testIndividualName.fullName)
            .withPage(OrganisationFirstContactEmailPage, testEmail)
            .withPage(OrganisationFirstContactHavePhonePage, false)
            .withPage(OrganisationHaveSecondContactPage, false)

          val result: Option[UpdateRcaspRequest] = rcaspSubmissionHelper.updateRcaspRequest(carfId, userAnswers)

          result mustBe None
        }

        "must return None when haveTradingName is true but trading name is missing" in {
          val userAnswers = emptyUserAnswers
            .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate)
            .withPage(ReportForRegisteredBusinessPage, false)
            .withPage(OrganisationOrIndividualPage, Organisation)
            .withPage(OverwritableOrganisationName, testOrgName)
            .withPage(HaveTradingNamePage, true)
            .withPage(UtrPage, testUtr.uniqueTaxPayerReference)
            .withPage(UkAddressInUserAnswers, testAddressUk)
            .withPage(OrganisationFirstContactNamePage, testIndividualName.fullName)
            .withPage(OrganisationFirstContactEmailPage, testEmail)
            .withPage(OrganisationFirstContactHavePhonePage, true)
            .withPage(OrganisationFirstContactPhoneNumberPage, testPhone)
            .withPage(OrganisationHaveSecondContactPage, true)
            .withPage(OrganisationSecondContactNamePage, testIndividualName.fullName)
            .withPage(OrganisationSecondContactEmailPage, testEmail)
            .withPage(OrganisationSecondContactHavePhonePage, true)
            .withPage(OrganisationSecondContactPhoneNumberPage, testPhone)

          val result: Option[UpdateRcaspRequest] = rcaspSubmissionHelper.updateRcaspRequest(carfId, userAnswers)

          result mustBe None
        }

        "must return None when first contact name is missing" in {
          val userAnswers = emptyUserAnswers
            .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate)
            .withPage(ReportForRegisteredBusinessPage, false)
            .withPage(OrganisationOrIndividualPage, Organisation)
            .withPage(OverwritableOrganisationName, testOrgName)
            .withPage(HaveTradingNamePage, false)
            .withPage(UtrPage, testUtr.uniqueTaxPayerReference)
            .withPage(UkAddressInUserAnswers, testAddressUk)
            .withPage(OrganisationFirstContactEmailPage, testEmail)
            .withPage(OrganisationFirstContactHavePhonePage, false)
            .withPage(OrganisationHaveSecondContactPage, false)

          val result: Option[UpdateRcaspRequest] = rcaspSubmissionHelper.updateRcaspRequest(carfId, userAnswers)

          result mustBe None
        }

        "must return None when haveSecondContact is true but second contact email is missing" in {
          val userAnswers = emptyUserAnswers
            .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate)
            .withPage(ReportForRegisteredBusinessPage, false)
            .withPage(OrganisationOrIndividualPage, Organisation)
            .withPage(OverwritableOrganisationName, testOrgName)
            .withPage(HaveTradingNamePage, true)
            .withPage(TradingNamePage, testTradingName)
            .withPage(UtrPage, testUtr.uniqueTaxPayerReference)
            .withPage(UkAddressInUserAnswers, testAddressUk)
            .withPage(OrganisationFirstContactNamePage, testIndividualName.fullName)
            .withPage(OrganisationFirstContactEmailPage, testEmail)
            .withPage(OrganisationFirstContactHavePhonePage, true)
            .withPage(OrganisationFirstContactPhoneNumberPage, testPhone)
            .withPage(OrganisationHaveSecondContactPage, true)
            .withPage(OrganisationSecondContactNamePage, testIndividualName.fullName)
            .withPage(OrganisationSecondContactHavePhonePage, true)
            .withPage(OrganisationSecondContactPhoneNumberPage, testPhone)

          val result: Option[UpdateRcaspRequest] = rcaspSubmissionHelper.updateRcaspRequest(carfId, userAnswers)

          result mustBe None
        }

        "must return None when secondContactHavePhone is true but second contact phone number is missing" in {
          val userAnswers = emptyUserAnswers
            .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate)
            .withPage(ReportForRegisteredBusinessPage, false)
            .withPage(OrganisationOrIndividualPage, Organisation)
            .withPage(OverwritableOrganisationName, testOrgName)
            .withPage(HaveTradingNamePage, true)
            .withPage(TradingNamePage, testTradingName)
            .withPage(UtrPage, testUtr.uniqueTaxPayerReference)
            .withPage(UkAddressInUserAnswers, testAddressUk)
            .withPage(OrganisationFirstContactNamePage, testIndividualName.fullName)
            .withPage(OrganisationFirstContactEmailPage, testEmail)
            .withPage(OrganisationFirstContactHavePhonePage, true)
            .withPage(OrganisationFirstContactPhoneNumberPage, testPhone)
            .withPage(OrganisationHaveSecondContactPage, true)
            .withPage(OrganisationSecondContactNamePage, testIndividualName.fullName)
            .withPage(OrganisationSecondContactEmailPage, testEmail)
            .withPage(OrganisationSecondContactHavePhonePage, true)

          val result: Option[UpdateRcaspRequest] = rcaspSubmissionHelper.updateRcaspRequest(carfId, userAnswers)

          result mustBe None
        }

        "must return None when reportForRegisteredBusiness is not false" - {
          "when reportForRegisteredBusiness is true" in {
            val userAnswers = emptyUserAnswers
              .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate)
              .withPage(ReportForRegisteredBusinessPage, true)
              .withPage(OrganisationOrIndividualPage, Organisation)
              .withPage(OverwritableOrganisationName, testOrgName)
              .withPage(HaveTradingNamePage, false)
              .withPage(UtrPage, testUtr.uniqueTaxPayerReference)
              .withPage(UkAddressInUserAnswers, testAddressUk)
              .withPage(OrganisationFirstContactNamePage, testIndividualName.fullName)
              .withPage(OrganisationFirstContactEmailPage, testEmail)
              .withPage(OrganisationFirstContactHavePhonePage, false)
              .withPage(OrganisationHaveSecondContactPage, false)

            val result: Option[UpdateRcaspRequest] = rcaspSubmissionHelper.updateRcaspRequest(carfId, userAnswers)

            result mustBe None
          }

          "when reportForRegisteredBusiness is missing" in {
            val userAnswers = emptyUserAnswers
              .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate)
              .withPage(OrganisationOrIndividualPage, Organisation)
              .withPage(OverwritableOrganisationName, testOrgName)
              .withPage(HaveTradingNamePage, false)
              .withPage(UtrPage, testUtr.uniqueTaxPayerReference)
              .withPage(UkAddressInUserAnswers, testAddressUk)
              .withPage(OrganisationFirstContactNamePage, testIndividualName.fullName)
              .withPage(OrganisationFirstContactEmailPage, testEmail)
              .withPage(OrganisationFirstContactHavePhonePage, false)
              .withPage(OrganisationHaveSecondContactPage, false)

            val result: Option[UpdateRcaspRequest] = rcaspSubmissionHelper.updateRcaspRequest(carfId, userAnswers)

            result mustBe None
          }
        }
      }

      "must return None when OrganisationOrIndividual is missing" in {
        val userAnswers = emptyUserAnswers
          .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate)
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
