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

package services

import base.SpecBase
import config.Constants.organisationPartyType
import models.OrganisationOrIndividual.{Individual, Organisation}
import models.audit.*
import models.errors.ApiError.InternalServerError
import models.individual.IndividualName
import models.{formatAddress, formatRcaspAddress}
import org.mockito.ArgumentMatchers.{any, argThat}
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalactic.Prettifier.default
import pages.*
import pages.changeDetails.ChangeRcaspCachedDetails
import pages.combined.OrganisationOrIndividualPage
import pages.individual.*
import pages.organisation.*
import play.api.libs.json.Json
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.http.connector.AuditResult.{Disabled, Failure, Success}

import scala.concurrent.Future

class AuditServiceSpec extends SpecBase {

  private val mockAuditConnector = mock[AuditConnector]
  private val service            = new AuditService(mockAuditConnector)

  override def beforeEach(): Unit = {
    reset(mockAuditConnector)
    super.beforeEach()
  }

  "AuditService" - {
    "Add RCASP Audit Event" - {

      "should return success for Individual" in {
        val userAnswers = emptyUserAnswers
          .withPage(OrganisationOrIndividualPage, Individual)
          .withPage(IndividualNamePage, testIndividualName)
          .withPage(NiNumberPage, testNiNumber)
          .withPage(FindAddressPage, testFindAddress)
          .withPage(UkAddressInUserAnswers, testAddressUk)
          .withPage(AddressUPRNUserAnswers, testUPRN.toLong)
          .withPage(ChooseAddressPage, "address")
          .withPage(IndividualEmailPage, testEmail)
          .withPage(IndividualHavePhonePage, true)
          .withPage(IndividualPhonePage, testPhone)

        val expectedAudit = AddRcaspAuditEvent(
          organisationCTMatch = None,
          isRCASPAnOrganisationOrIndividual = Some(Individual),
          addRCASPIndividual = Some(
            AddRcaspIndividual(
              individualRCASPFirstName = testIndividualName.firstName,
              individualRCASPLastName = testIndividualName.lastName,
              RCASPNino = testNiNumber
            )
          ),
          addRCASPOrganisation = None,
          addressLookup = Some(
            AddressLookup(
              findAddress = testFindAddress.postcode,
              propertyNameOrNumber = testFindAddress.propertyNameOrNumber,
              UPRN = Some(testUPRN),
              chooseAddress = Some("address"),
              RCASPAddressLine1 = testAddressUk.addressLine1,
              RCASPAddressLine2 = testAddressUk.addressLine2,
              RCASPTown = testAddressUk.townOrCity,
              RCASPCounty = testAddressUk.addressLine3,
              RCASPPostcode = testAddressUk.postCode
            )
          ),
          individualContactDetails = Some(
            IndividualContactDetails(
              individualEmailAddress = testEmail,
              individualContactByPhone = true,
              individualPhoneNumber = Some(testPhone)
            )
          ),
          organisationContactDetails = None
        )

        when(mockAuditConnector.sendExtendedEvent(any())(any(), any()))
          .thenReturn(Future.successful(Success))

        val result = service.auditAddRcasp(userAnswers).value.futureValue

        result mustBe Right(())

        verify(mockAuditConnector, times(1)).sendExtendedEvent(
          argThat(event =>
            event.auditSource == "carf-management-frontend" && event.auditType == "AddRCASP"
              && event.detail == Json.toJson(expectedAudit)
          )
        )(any(), any())
      }

      "should return success for Organisation" in {
        val userAnswers = emptyUserAnswers
          .withPage(OrganisationOrIndividualPage, Organisation)
          .withPage(OrganisationNamePage, testOrgName)
          .withPage(HaveTradingNamePage, true)
          .withPage(FindAddressPage, testFindAddress)
          .withPage(UkAddressInUserAnswers, testAddressUk)
          .withPage(AddressUPRNUserAnswers, testUPRN.toLong)
          .withPage(ChooseAddressPage, "address")
          .withPage(TradingNamePage, testTradingName)
          .withPage(UtrPage, testUtr.toString)
          .withPage(RegisteredBusinessIsTheAddressCorrectPage, true)
          .withPage(OrganisationFirstContactNamePage, testOrgContactName)
          .withPage(OrganisationFirstContactEmailPage, testEmail)
          .withPage(OrganisationFirstContactHavePhonePage, true)
          .withPage(OrganisationFirstContactPhoneNumberPage, testPhone)
          .withPage(OrganisationHaveSecondContactPage, true)
          .withPage(OrganisationSecondContactNamePage, testOrgContactName)
          .withPage(OrganisationSecondContactEmailPage, testEmail)
          .withPage(OrganisationSecondContactHavePhonePage, true)
          .withPage(OrganisationSecondContactPhoneNumberPage, testPhone)

        val expectedAudit = AddRcaspAuditEvent(
          organisationCTMatch = None,
          isRCASPAnOrganisationOrIndividual = Some(Organisation),
          addRCASPIndividual = None,
          addRCASPOrganisation = Some(
            AddRcaspOrganisation(
              organisationName = testOrgName,
              doesRCASPTradeUnderDifferentName = true,
              RCASPTradingName = Some(testTradingName),
              RCASPUTR = Some(testUtr.toString),
              confirmRCASPregisteredAddress = Some(true)
            )
          ),
          addressLookup = Some(
            AddressLookup(
              findAddress = testFindAddress.postcode,
              propertyNameOrNumber = testFindAddress.propertyNameOrNumber,
              UPRN = Some(testUPRN),
              chooseAddress = Some("address"),
              RCASPAddressLine1 = testAddressUk.addressLine1,
              RCASPAddressLine2 = testAddressUk.addressLine2,
              RCASPTown = testAddressUk.townOrCity,
              RCASPCounty = testAddressUk.addressLine3,
              RCASPPostcode = testAddressUk.postCode
            )
          ),
          individualContactDetails = None,
          organisationContactDetails = Some(
            OrganisationContactDetails(
              Contact1Name = testOrgContactName,
              Contact1Email = testEmail,
              Contact1ByPhone = true,
              Contact1PhoneNumber = Some(testPhone),
              Contact2 = true,
              Contact2Name = Some(testOrgContactName),
              Contact2Email = Some(testEmail),
              Contact2Phone = Some(true),
              Contact2PhoneNumber = Some(testPhone)
            )
          )
        )

        when(mockAuditConnector.sendExtendedEvent(any())(any(), any()))
          .thenReturn(Future.successful(Success))

        val result = service.auditAddRcasp(userAnswers).value.futureValue

        result mustBe Right(())

        verify(mockAuditConnector, times(1)).sendExtendedEvent(
          argThat(event =>
            event.auditSource == "carf-management-frontend" && event.auditType == "AddRCASP"
              && event.detail == Json.toJson(expectedAudit)
          )
        )(any(), any())
      }

      "should return success for Organisation CT Automatch" in {
        val userAnswers = emptyUserAnswers
          .withPage(ReportForRegisteredBusinessPage, true)
          .withPage(RegisteredBusinessIsThisYourBusinessNamePage, true)
          .withPage(FindAddressPage, testFindAddress)
          .withPage(UkAddressInUserAnswers, testAddressUk)
          .withPage(AddressUPRNUserAnswers, testUPRN.toLong)
          .withPage(ChooseAddressPage, "address")
          .withPage(OrganisationNamePage, testOrgName)
          .withPage(HaveTradingNamePage, true)
          .withPage(TradingNamePage, testTradingName)
          .withPage(UtrPage, testUtr.toString)
          .withPage(RegisteredBusinessIsTheAddressCorrectPage, true)

        val expectedAudit = AddRcaspAuditEvent(
          organisationCTMatch = Some(OrganisationCtMatch(isBusinessAnRCASP = true, isBusinessNameCorrect = Some(true))),
          isRCASPAnOrganisationOrIndividual = None,
          addRCASPIndividual = None,
          addRCASPOrganisation = Some(
            AddRcaspOrganisation(
              organisationName = testOrgName,
              doesRCASPTradeUnderDifferentName = true,
              RCASPTradingName = Some(testTradingName),
              RCASPUTR = Some(testUtr.toString),
              confirmRCASPregisteredAddress = Some(true)
            )
          ),
          addressLookup = Some(
            AddressLookup(
              findAddress = testFindAddress.postcode,
              propertyNameOrNumber = testFindAddress.propertyNameOrNumber,
              UPRN = Some(testUPRN),
              chooseAddress = Some("address"),
              RCASPAddressLine1 = testAddressUk.addressLine1,
              RCASPAddressLine2 = testAddressUk.addressLine2,
              RCASPTown = testAddressUk.townOrCity,
              RCASPCounty = testAddressUk.addressLine3,
              RCASPPostcode = testAddressUk.postCode
            )
          ),
          individualContactDetails = None,
          organisationContactDetails = None
        )

        when(mockAuditConnector.sendExtendedEvent(any())(any(), any()))
          .thenReturn(Future.successful(Success))

        val result = service.auditAddRcasp(userAnswers).value.futureValue

        result mustBe Right(())

        verify(mockAuditConnector, times(1)).sendExtendedEvent(
          argThat(event =>
            event.auditSource == "carf-management-frontend" && event.auditType == "AddRCASP"
              && event.detail == Json.toJson(expectedAudit)
          )
        )(any(), any())
      }

      "should return success for Organisation CT Automatch when answering no to ReportForRegisteredBusiness" in {
        val userAnswers = emptyUserAnswers
          .withPage(ReportForRegisteredBusinessPage, false)
          .withPage(OrganisationOrIndividualPage, Organisation)
          .withPage(OrganisationNamePage, testOrgName)
          .withPage(HaveTradingNamePage, true)
          .withPage(FindAddressPage, testFindAddress)
          .withPage(UkAddressInUserAnswers, testAddressUk)
          .withPage(AddressUPRNUserAnswers, testUPRN.toLong)
          .withPage(ChooseAddressPage, "address")
          .withPage(TradingNamePage, testTradingName)
          .withPage(UtrPage, testUtr.toString)
          .withPage(RegisteredBusinessIsTheAddressCorrectPage, true)
          .withPage(OrganisationFirstContactNamePage, testOrgContactName)
          .withPage(OrganisationFirstContactEmailPage, testEmail)
          .withPage(OrganisationFirstContactHavePhonePage, true)
          .withPage(OrganisationFirstContactPhoneNumberPage, testPhone)
          .withPage(OrganisationHaveSecondContactPage, true)
          .withPage(OrganisationSecondContactNamePage, testOrgContactName)
          .withPage(OrganisationSecondContactEmailPage, testEmail)
          .withPage(OrganisationSecondContactHavePhonePage, true)
          .withPage(OrganisationSecondContactPhoneNumberPage, testPhone)

        val expectedAudit = AddRcaspAuditEvent(
          organisationCTMatch = Some(OrganisationCtMatch(isBusinessAnRCASP = false, isBusinessNameCorrect = None)),
          isRCASPAnOrganisationOrIndividual = Some(Organisation),
          addRCASPIndividual = None,
          addRCASPOrganisation = Some(
            AddRcaspOrganisation(
              organisationName = testOrgName,
              doesRCASPTradeUnderDifferentName = true,
              RCASPTradingName = Some(testTradingName),
              RCASPUTR = Some(testUtr.toString),
              confirmRCASPregisteredAddress = Some(true)
            )
          ),
          addressLookup = Some(
            AddressLookup(
              findAddress = testFindAddress.postcode,
              propertyNameOrNumber = testFindAddress.propertyNameOrNumber,
              UPRN = Some(testUPRN),
              chooseAddress = Some("address"),
              RCASPAddressLine1 = testAddressUk.addressLine1,
              RCASPAddressLine2 = testAddressUk.addressLine2,
              RCASPTown = testAddressUk.townOrCity,
              RCASPCounty = testAddressUk.addressLine3,
              RCASPPostcode = testAddressUk.postCode
            )
          ),
          individualContactDetails = None,
          organisationContactDetails = Some(
            OrganisationContactDetails(
              Contact1Name = testOrgContactName,
              Contact1Email = testEmail,
              Contact1ByPhone = true,
              Contact1PhoneNumber = Some(testPhone),
              Contact2 = true,
              Contact2Name = Some(testOrgContactName),
              Contact2Email = Some(testEmail),
              Contact2Phone = Some(true),
              Contact2PhoneNumber = Some(testPhone)
            )
          )
        )

        when(mockAuditConnector.sendExtendedEvent(any())(any(), any()))
          .thenReturn(Future.successful(Success))

        val result = service.auditAddRcasp(userAnswers).value.futureValue

        result mustBe Right(())

        verify(mockAuditConnector, times(1)).sendExtendedEvent(
          argThat(event =>
            event.auditSource == "carf-management-frontend" && event.auditType == "AddRCASP"
              && event.detail == Json.toJson(expectedAudit)
          )
        )(any(), any())
      }

      "should return Internal server error when Disabled is returned by audit connector" in {

        when(mockAuditConnector.sendExtendedEvent(any())(any(), any()))
          .thenReturn(Future.successful(Disabled))

        val result = service.auditAddRcasp(emptyUserAnswers).value.futureValue

        result mustBe Left(InternalServerError)

      }

      "should return Internal server error when Failure is returned by audit connector" in {

        when(mockAuditConnector.sendExtendedEvent(any())(any(), any()))
          .thenReturn(Future.successful(Failure("uh oh")))

        val result = service.auditAddRcasp(emptyUserAnswers).value.futureValue

        result mustBe Left(InternalServerError)

      }

      "should return Internal server error when call to audit connector's future fails" in {

        when(mockAuditConnector.sendExtendedEvent(any())(any(), any()))
          .thenReturn(Future.successful(Future.failed(new Exception("uh oh"))))

        val result = service.auditAddRcasp(emptyUserAnswers).value.futureValue

        result mustBe Left(InternalServerError)

      }
    }

    "Change RCASP Audit Event" - {

      "should return success for rcasp user" in {
        val userAnswers = emptyUserAnswers
          .withPage(ReportForRegisteredBusinessPage, true)
          .withPage(OrganisationNamePage, testOrgName)
          .withPage(HaveTradingNamePage, true)
          .withPage(TradingNamePage, testTradingName)
          .withPage(UkAddressInUserAnswers, testAddressUk)
          .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate.copy(IsRCASPUser = true))

        val expectedAudit = ChangeRcaspAuditEvent(
          changeRCASPIsUserUpdatedValues = Some(
            ChangeRcaspIsUserValues(
              isBusinessAnRCASP = true,
              organisationName = testOrgName,
              doesRCASPTradeUnderDifferentName = true,
              RCASPTradingName = Some(testTradingName),
              RCASPAddress = testAddressUk.formatAddress
            )
          ),
          changeRCASPIsUserOriginalValues = Some(
            ChangeRcaspIsUserValues(
              isBusinessAnRCASP = true,
              organisationName = testOrgName,
              doesRCASPTradeUnderDifferentName = true,
              RCASPTradingName = Some(testTradingName),
              RCASPAddress = testAddressUkRcaspAddress.formatRcaspAddress
            )
          ),
          changeRCASPisNotUserUpdatedValues = None,
          changeRCASPisNotUserOriginalValues = None
        )

        when(mockAuditConnector.sendExtendedEvent(any())(any(), any()))
          .thenReturn(Future.successful(Success))

        val result = service.auditChangeRcasp(userAnswers).value.futureValue

        result mustBe Right(())

        verify(mockAuditConnector, times(1)).sendExtendedEvent(
          argThat(event =>
            event.auditSource == "carf-management-frontend" && event.auditType == "ChangeRCASP"
              && event.detail == Json.toJson(expectedAudit)
          )
        )(any(), any())
      }

      "should return success for non rcasp user when user is an organisation" in {
        val userAnswers = emptyUserAnswers
          .withPage(ReportForRegisteredBusinessPage, false)
          .withPage(OrganisationOrIndividualPage, Organisation)
          .withPage(OrganisationNamePage, testOrgName)
          .withPage(HaveTradingNamePage, true)
          .withPage(TradingNamePage, testTradingName)
          .withPage(UtrPage, testUtr.uniqueTaxPayerReference)
          .withPage(OrganisationFirstContactNamePage, testOrgContactName)
          .withPage(OrganisationFirstContactEmailPage, testEmail)
          .withPage(OrganisationFirstContactHavePhonePage, true)
          .withPage(OrganisationFirstContactPhoneNumberPage, testPhone)
          .withPage(OrganisationHaveSecondContactPage, true)
          .withPage(OrganisationSecondContactNamePage, testOrgContactName)
          .withPage(OrganisationSecondContactEmailPage, testEmail)
          .withPage(OrganisationSecondContactHavePhonePage, true)
          .withPage(OrganisationSecondContactPhoneNumberPage, testPhone)
          .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate)
          .withPage(IndividualNamePage, testIndividualName)
          .withPage(UkAddressInUserAnswers, testAddressUk)

        val expectedAudit = ChangeRcaspAuditEvent(
          changeRCASPIsUserUpdatedValues = None,
          changeRCASPIsUserOriginalValues = None,
          changeRCASPisNotUserUpdatedValues = Some(
            ChangeRcaspIsNotUserValues(
              isBusinessAnRCASP = false,
              isRCASPAnOrganisationOrIndividual = "Organisation",
              organisationName = Some(testOrgName),
              doesRCASPTradeUnderDifferentName = Some(true),
              RCASPTradeName = Some(testTradingName),
              RCASPUTR = Some(testUtr.uniqueTaxPayerReference),
              IndividualRCASPFirstName = None,
              IndividualRCASPLastName = None,
              IndividualRCASPNino = None,
              IndividualRCASPAddress = None,
              Contact1Name = Some(testOrgContactName),
              Contact1EmailAddress = Some(testEmail),
              Contact1ContactByPhone = Some(true),
              Contact1PhoneNumber = Some(testPhone),
              Contact2 = Some(true),
              Contact2Name = Some(testOrgContactName),
              Contact2EmailAddress = Some(testEmail),
              Contact2ContactByPhone = Some(true),
              Contact2PhoneNumber = Some(testPhone),
              individualEmailAddress = None,
              individualContactByPhone = None,
              individuaPhoneNumber = None
            )
          ),
          changeRCASPisNotUserOriginalValues = Some(
            ChangeRcaspIsNotUserValues(
              isBusinessAnRCASP = false,
              isRCASPAnOrganisationOrIndividual = organisationPartyType,
              organisationName = Some(organisationRcaspDetailsViewUpdate.RCASPName),
              doesRCASPTradeUnderDifferentName = Some(true),
              RCASPTradeName = Some(organisationRcaspDetailsViewUpdate.TradingName),
              RCASPUTR = Some(testUtr.uniqueTaxPayerReference),
              IndividualRCASPFirstName = None,
              IndividualRCASPLastName = None,
              IndividualRCASPNino = None,
              IndividualRCASPAddress = None,
              Contact1Name = Some(rcaspContactDetails.ContactName),
              Contact1EmailAddress = Some(rcaspContactDetails.EmailAddress),
              Contact1ContactByPhone = Some(true),
              Contact1PhoneNumber = rcaspContactDetails.PhoneNumber,
              Contact2 = Some(true),
              Contact2Name = Some(testOrgContactName),
              Contact2EmailAddress = Some(rcaspContactDetails.EmailAddress),
              Contact2ContactByPhone = Some(true),
              Contact2PhoneNumber = rcaspContactDetails.PhoneNumber,
              individualEmailAddress = None,
              individualContactByPhone = None,
              individuaPhoneNumber = None
            )
          )
        )

        when(mockAuditConnector.sendExtendedEvent(any())(any(), any()))
          .thenReturn(Future.successful(Success))

        val result = service.auditChangeRcasp(userAnswers).value.futureValue

        result mustBe Right(())

        verify(mockAuditConnector, times(1)).sendExtendedEvent(
          argThat(event =>
            event.auditSource == "carf-management-frontend" && event.auditType == "ChangeRCASP"
              && event.detail == Json.toJson(expectedAudit)
          )
        )(any(), any())
      }

      "should return success for non rcasp user when user is an individual" in {
        val userAnswers = emptyUserAnswers
          .withPage(ReportForRegisteredBusinessPage, false)
          .withPage(OrganisationOrIndividualPage, Individual)
          .withPage(IndividualNamePage, testIndividualName)
          .withPage(NiNumberPage, testNiNumber)
          .withPage(UkAddressInUserAnswers, testAddressUk)
          .withPage(IndividualEmailPage, testEmail)
          .withPage(IndividualHavePhonePage, true)
          .withPage(IndividualPhonePage, testPhone)
          .withPage(ChangeRcaspCachedDetails, individualRcaspDetailsViewUpdate)

        val expectedAudit = ChangeRcaspAuditEvent(
          changeRCASPIsUserUpdatedValues = None,
          changeRCASPIsUserOriginalValues = None,
          changeRCASPisNotUserUpdatedValues = Some(
            ChangeRcaspIsNotUserValues(
              isBusinessAnRCASP = false,
              isRCASPAnOrganisationOrIndividual = "Individual",
              organisationName = None,
              doesRCASPTradeUnderDifferentName = None,
              RCASPTradeName = None,
              RCASPUTR = None,
              IndividualRCASPFirstName = Some(testIndividualName.firstName),
              IndividualRCASPLastName = Some(testIndividualName.lastName),
              IndividualRCASPNino = Some(testNiNumber),
              IndividualRCASPAddress = Some(testAddressUk.toString),
              Contact1Name = None,
              Contact1EmailAddress = None,
              Contact1ContactByPhone = None,
              Contact1PhoneNumber = None,
              Contact2 = None,
              Contact2Name = None,
              Contact2EmailAddress = None,
              Contact2ContactByPhone = None,
              Contact2PhoneNumber = None,
              individualEmailAddress = Some(testEmail),
              individualContactByPhone = Some(true),
              individuaPhoneNumber = Some(testPhone)
            )
          ),
          changeRCASPisNotUserOriginalValues = Some(
            ChangeRcaspIsNotUserValues(
              isBusinessAnRCASP = false,
              isRCASPAnOrganisationOrIndividual = individualRcaspDetailsViewUpdate.PartyType,
              organisationName = None,
              doesRCASPTradeUnderDifferentName = None,
              RCASPTradeName = None,
              RCASPUTR = None,
              IndividualRCASPFirstName = Some(individualRcaspDetailsViewUpdate.FirstName),
              IndividualRCASPLastName = Some(individualRcaspDetailsViewUpdate.LastName),
              IndividualRCASPNino = individualRcaspDetailsViewUpdate.TINDetails.flatMap(_.headOption.map(_.TIN)),
              IndividualRCASPAddress = Some(individualRcaspDetailsViewUpdate.AddressDetails.toString),
              Contact1Name = None,
              Contact1EmailAddress = None,
              Contact1ContactByPhone = None,
              Contact1PhoneNumber = None,
              Contact2 = None,
              Contact2Name = None,
              Contact2EmailAddress = None,
              Contact2ContactByPhone = None,
              Contact2PhoneNumber = None,
              individualEmailAddress = individualRcaspDetailsViewUpdate.PrimaryContactDetails.map(_.EmailAddress),
              individualContactByPhone =
                Some(individualRcaspDetailsViewUpdate.PrimaryContactDetails.map(_.PhoneNumber).isDefined),
              individuaPhoneNumber = individualRcaspDetailsViewUpdate.PrimaryContactDetails.flatMap(_.PhoneNumber)
            )
          )
        )

        when(mockAuditConnector.sendExtendedEvent(any())(any(), any()))
          .thenReturn(Future.successful(Success))

        val result = service.auditChangeRcasp(userAnswers).value.futureValue

        result mustBe Right(())

        verify(mockAuditConnector, times(1)).sendExtendedEvent(
          argThat(event =>
            event.auditSource == "carf-management-frontend" && event.auditType == "ChangeRCASP"
              && event.detail == Json.toJson(expectedAudit)
          )
        )(any(), any())
      }

      "should return success for rcasp user changing to non rcasp user" in {
        val userAnswers = emptyUserAnswers
          .withPage(ReportForRegisteredBusinessPage, false)
          .withPage(OrganisationOrIndividualPage, Organisation)
          .withPage(OrganisationNamePage, testOrgName)
          .withPage(HaveTradingNamePage, true)
          .withPage(TradingNamePage, testTradingName)
          .withPage(UtrPage, testUtr.uniqueTaxPayerReference)
          .withPage(OrganisationFirstContactNamePage, testOrgContactName)
          .withPage(OrganisationFirstContactEmailPage, testEmail)
          .withPage(OrganisationFirstContactHavePhonePage, true)
          .withPage(OrganisationFirstContactPhoneNumberPage, testPhone)
          .withPage(OrganisationHaveSecondContactPage, true)
          .withPage(OrganisationSecondContactNamePage, testOrgContactName)
          .withPage(OrganisationSecondContactEmailPage, testEmail)
          .withPage(OrganisationSecondContactHavePhonePage, true)
          .withPage(OrganisationSecondContactPhoneNumberPage, testPhone)
          .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate.copy(IsRCASPUser = true))
          .withPage(IndividualNamePage, testIndividualName)
          .withPage(UkAddressInUserAnswers, testAddressUk)

        val expectedAudit = ChangeRcaspAuditEvent(
          changeRCASPIsUserUpdatedValues = None,
          changeRCASPIsUserOriginalValues = Some(
            ChangeRcaspIsUserValues(
              isBusinessAnRCASP = true,
              organisationName = testOrgName,
              doesRCASPTradeUnderDifferentName = true,
              RCASPTradingName = Some(testTradingName),
              RCASPAddress = testAddressUkRcaspAddress.formatRcaspAddress
            )
          ),
          changeRCASPisNotUserUpdatedValues = Some(
            ChangeRcaspIsNotUserValues(
              isBusinessAnRCASP = false,
              isRCASPAnOrganisationOrIndividual = "Organisation",
              organisationName = Some(testOrgName),
              doesRCASPTradeUnderDifferentName = Some(true),
              RCASPTradeName = Some(testTradingName),
              RCASPUTR = Some(testUtr.uniqueTaxPayerReference),
              IndividualRCASPFirstName = None,
              IndividualRCASPLastName = None,
              IndividualRCASPNino = None,
              IndividualRCASPAddress = None,
              Contact1Name = Some(testOrgContactName),
              Contact1EmailAddress = Some(testEmail),
              Contact1ContactByPhone = Some(true),
              Contact1PhoneNumber = Some(testPhone),
              Contact2 = Some(true),
              Contact2Name = Some(testOrgContactName),
              Contact2EmailAddress = Some(testEmail),
              Contact2ContactByPhone = Some(true),
              Contact2PhoneNumber = Some(testPhone),
              individualEmailAddress = None,
              individualContactByPhone = None,
              individuaPhoneNumber = None
            )
          ),
          changeRCASPisNotUserOriginalValues = None
        )

        when(mockAuditConnector.sendExtendedEvent(any())(any(), any()))
          .thenReturn(Future.successful(Success))

        val result = service.auditChangeRcasp(userAnswers).value.futureValue

        result mustBe Right(())

        verify(mockAuditConnector, times(1)).sendExtendedEvent(
          argThat(event =>
            event.auditSource == "carf-management-frontend" && event.auditType == "ChangeRCASP"
              && event.detail == Json.toJson(expectedAudit)
          )
        )(any(), any())
      }

      "should return Internal server error when Disabled is returned by audit connector" in {

        val userAnswers = emptyUserAnswers
          .withPage(ReportForRegisteredBusinessPage, true)
          .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate.copy(IsRCASPUser = true))

        when(mockAuditConnector.sendExtendedEvent(any())(any(), any()))
          .thenReturn(Future.successful(Disabled))

        val result = service.auditChangeRcasp(userAnswers).value.futureValue

        result mustBe Left(InternalServerError)

      }

      "should return Internal server error when Failure is returned by audit connector" in {
        val userAnswers = emptyUserAnswers
          .withPage(ReportForRegisteredBusinessPage, true)
          .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate.copy(IsRCASPUser = true))
        when(mockAuditConnector.sendExtendedEvent(any())(any(), any()))
          .thenReturn(Future.successful(Failure("uh oh")))

        val result = service.auditChangeRcasp(userAnswers).value.futureValue

        result mustBe Left(InternalServerError)

      }

      "should return Internal server error when call to audit connector's future fails" in {
        val userAnswers = emptyUserAnswers
          .withPage(ReportForRegisteredBusinessPage, true)
          .withPage(ChangeRcaspCachedDetails, organisationRcaspDetailsViewUpdate.copy(IsRCASPUser = true))
        when(mockAuditConnector.sendExtendedEvent(any())(any(), any()))
          .thenReturn(Future.successful(Future.failed(new Exception("uh oh"))))

        val result = service.auditChangeRcasp(userAnswers).value.futureValue

        result mustBe Left(InternalServerError)

      }
    }

    "Remove RCASP Audit Event" - {
      "should return success for valid pages" in {

        val expectedAudit = RemoveRcaspAuditEvent(
          removeUserAccessConfirmation = true,
          removeOtherUserAccessConfirmation = true,
          removeRCASP = true
        )

        when(mockAuditConnector.sendExtendedEvent(any())(any(), any()))
          .thenReturn(Future.successful(Success))

        val result = service.auditRemoveRcasp(true, true, true).value.futureValue

        result mustBe Right(())

        verify(mockAuditConnector, times(1)).sendExtendedEvent(
          argThat(event =>
            event.auditSource == "carf-management-frontend" && event.auditType == "RemoveRCASP"
              && event.detail == Json.toJson(expectedAudit)
          )
        )(any(), any())
      }

      "should return Internal server error when Disabled is returned by audit connector" in {

        when(mockAuditConnector.sendExtendedEvent(any())(any(), any()))
          .thenReturn(Future.successful(Disabled))

        val result = service.auditRemoveRcasp(true, true, true).value.futureValue

        result mustBe Left(InternalServerError)

      }

      "should return Internal server error when Failure is returned by audit connector" in {

        when(mockAuditConnector.sendExtendedEvent(any())(any(), any()))
          .thenReturn(Future.successful(Failure("uh oh")))

        val result = service.auditRemoveRcasp(true, true, true).value.futureValue

        result mustBe Left(InternalServerError)

      }

      "should return Internal server error when call to audit connector's future fails" in {

        when(mockAuditConnector.sendExtendedEvent(any())(any(), any()))
          .thenReturn(Future.successful(Future.failed(new Exception("uh oh"))))

        val result = service.auditRemoveRcasp(true, true, true).value.futureValue

        result mustBe Left(InternalServerError)

      }
    }
  }

}
