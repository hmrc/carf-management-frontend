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

package common

import generators.Generators
import models.*
import models.countries.CountryUk
import models.individual.IndividualName
import models.requests.{createRcasp, deleteRcasp, updateRcasp, RcaspRequestCommon}
import models.requests.createRcasp.RcaspRequest as CreateRcaspRequest
import models.requests.updateRcasp.RcaspRequest as UpdateRcaspRequest
import models.requests.deleteRcasp.RcaspRequest as DeleteRcaspRequest
import models.responses.*
import org.scalatest.OptionValues.convertOptionToValuable
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Key, SummaryListRow}
import viewmodels.Section
import viewmodels.govuk.all.{ActionItemViewModel, FluentActionItem, SummaryListRowViewModel, ValueViewModel}

import java.time.{Clock, Instant, ZoneId}

trait TestData extends Generators {

  val userAnswersId: String            = "id"
  val testUtr: UniqueTaxpayerReference = UniqueTaxpayerReference("1234567890")
  val testInternalId: String           = "12345"
  val testCarfId: String               = "XE0000123456789"
  val testUPRN: Int                    = 123456789
  val testUPRNAlt: Int                 = 223456789

  private val utcZoneId     = "UTC"
  implicit val clock: Clock = Clock.fixed(Instant.parse("2020-05-20T12:34:56.789012Z"), ZoneId.of(utcZoneId))

  val testOrgName        = "Timmy Ltd"
  val testTradingName    = "Trading Name"
  val testName           = "Timmy"
  val testOrgContactName = "John Doe"
  val testIndividualName = IndividualName("Timmy", "Jimmison")
  val testNiNumber       = "BA123456A"
  val testEmail          = "hi@example.com"
  val testPhone          = "07123456789"

  def emptyUserAnswers: UserAnswers =
    UserAnswers(id = userAnswersId, lastUpdated = Instant.now(clock), rcaspIsRegisteredBusiness = false)

  lazy val testFindAddress: FindAddress = FindAddress("SW1A 1AA", Some("10"))

  lazy val testPostcode: String = validPostcodes.sample.value

  def oneAddressResponse: AddressResponse =
    AddressResponse(
      id = "123",
      uprn = testUPRN,
      address = AddressRecord(
        lines = List("1 Test", "Test Street", "Test Region"),
        town = "Testingtown",
        postcode = testPostcode,
        country = CountryRecord(code = "GB", name = "United Kingdom")
      )
    )

  lazy val testAddressUk: AddressUk = AddressUk(
    addressLine1 = "1 Test",
    addressLine2 = Some("Test Street"),
    addressLine3 = Some("Test Region"),
    townOrCity = "Testingtown",
    postCode = testPostcode,
    countryUk = CountryUk("GB", "United Kingdom")
  )

  lazy val testAddressUkAlt: AddressUk = AddressUk(
    addressLine1 = "2 Test",
    addressLine2 = Some("Test Road"),
    addressLine3 = Some("Test Area"),
    townOrCity = "Testingville",
    postCode = testPostcode,
    countryUk = CountryUk("GB", "United Kingdom")
  )

  lazy val testAddressAndUprns: Seq[AddressAndUPRN] = Seq(
    AddressAndUPRN(testAddressUk, testUPRN),
    AddressAndUPRN(testAddressUk, testUPRN),
    AddressAndUPRN(testAddressUk, testUPRN)
  )

  lazy val multipleAddressResponses: Seq[AddressResponse] =
    Seq(oneAddressResponse, oneAddressResponse, oneAddressResponse)

  val testSignOutUrl: String       = "http://localhost:9553/bas-gateway/sign-out-without-state"
  val testLoginContinueUrl: String = "http://localhost:17000/register-for-cryptoasset-reporting"

  val testAddressRegistrationResponse = AddressRegistrationResponse(
    addressLine1 = "2 High Street",
    addressLine2 = Some("Birmingham"),
    addressLine3 = None,
    addressLine4 = None,
    postalCode = Some("B23 2AZ"),
    countryCode = "GB"
  )

  val cachedBusinessDetails: CachedBusinessDetails =
    CachedBusinessDetails(
      name = "Test Business Ltd",
      address = AddressRegistrationResponse(
        addressLine1 = "1 Test",
        addressLine2 = Some("Test Street"),
        addressLine3 = Some("Test Region"),
        addressLine4 = Some("Testingtown"),
        postalCode = Some(testPostcode),
        countryCode = "GB"
      ),
      countryName = "United Kingdom"
    )

  val carfId: String  = "XCCAR0024000102"
  val rcaspId: String = "ZMCAR0123456789"

  val rcaspContactDetails: RcaspContactDetails =
    RcaspContactDetails(
      ContactName = "Prof Sada",
      EmailAddress = "test@example.com",
      PhoneNumber = Some("07123412345")
    )
  val rcaspResponseCommon: RcaspResponseCommon =
    RcaspResponseCommon(
      OriginatingSystem = "CADX",
      TransmittingSystem = "EIS",
      RequestType = "VIEW",
      Regime = "CARF",
      ResponseParameters = None
    )

  def rcaspAddress: RcaspAddress =
    RcaspAddress(
      AddressLine1 = "64",
      AddressLine2 = Some("Zoo"),
      AddressLine3 = Some("Lane"),
      AddressLine4 = Some("Sixty Four"),
      PostalCode = "G66 2AZ",
      CountryCode = "GB"
    )

  val individualRcaspDetailsResponse: viewAndUpdateRcasp.IndividualRcaspDetails =
    viewAndUpdateRcasp.IndividualRcaspDetails(
      SubscriptionID = "XCARF000000001",
      RCASPID = rcaspId,
      IsRCASPUser = true,
      PartyType = "Individual",
      FirstName = "Penny",
      LastName = "Cassiopeia",
      TINDetails = Some(List(TinDetails(TINType = "OTHER", TIN = "6893649", IssuedBy = "GB"))),
      AddressDetails = rcaspAddress,
      PrimaryContactDetails = Some(rcaspContactDetails)
    )

  val organisationRcaspDetailsResponse: viewAndUpdateRcasp.OrganisationRcaspDetails =
    viewAndUpdateRcasp.OrganisationRcaspDetails(
      SubscriptionID = carfId,
      RCASPID = rcaspId,
      IsRCASPUser = true,
      PartyType = "Organisation",
      RCASPName = "Mesagoza",
      TradingName = "Uva Academy",
      TINDetails = Some(List(TinDetails(TINType = "UTR", TIN = "68936493", IssuedBy = "GB"))),
      AddressDetails = rcaspAddress,
      PrimaryContactDetails = Some(rcaspContactDetails),
      SecondaryContactDetails = Some(rcaspContactDetails.copy(ContactName = "Prof Turo"))
    )

  val testViewRcaspResponse: ViewRcaspResponse =
    ViewRcaspResponse(
      ViewRCASP = ViewRcasp(
        ResponseCommon = rcaspResponseCommon,
        ResponseDetails = RcaspResponseDetails(
          RCASPList = List(
            organisationRcaspDetailsResponse
          )
        )
      )
    )

  val individualRcaspDetailsRequest: createRcasp.IndividualRcaspDetails =
    createRcasp.IndividualRcaspDetails(
      SubscriptionID = carfId,
      IsRCASPUser = true,
      PartyType = "Individual",
      FirstName = "Penny",
      LastName = "Cassiopeia",
      TINDetails = Some(List(TinDetails(TINType = "OTHER", TIN = "6893649", IssuedBy = "GB"))),
      AddressDetails = rcaspAddress,
      PrimaryContactDetails = Some(rcaspContactDetails)
    )

  val updateIndividualRcaspDetailsRequest: viewAndUpdateRcasp.IndividualRcaspDetails =
    viewAndUpdateRcasp.IndividualRcaspDetails(
      RCASPID = rcaspId,
      SubscriptionID = carfId,
      IsRCASPUser = true,
      PartyType = "Individual",
      FirstName = "Penny",
      LastName = "Cassiopeia",
      TINDetails = Some(List(TinDetails(TINType = "OTHER", TIN = "6893649", IssuedBy = "GB"))),
      AddressDetails = rcaspAddress,
      PrimaryContactDetails = Some(rcaspContactDetails)
    )

  val deleteRcaspDetailsRequest: deleteRcasp.RcaspDetails =
    deleteRcasp.RcaspDetails(
      RCASPID = rcaspId,
      SubscriptionID = carfId
    )

  val registeredBusinessRcaspDetailsRequest: createRcasp.OrganisationRcaspDetails =
    createRcasp.OrganisationRcaspDetails(
      SubscriptionID = carfId,
      IsRCASPUser = true,
      PartyType = "Organisation",
      RCASPName = testOrgName,
      TradingName = testTradingName,
      TINDetails = Some(List(TinDetails(TINType = "UTR", TIN = "68936493", IssuedBy = "GB"))),
      AddressDetails = rcaspAddress,
      PrimaryContactDetails = None,
      SecondaryContactDetails = None
    )

  val rcaspRequestCommon: RcaspRequestCommon =
    RcaspRequestCommon(
      OriginatingSystem = "MDTP",
      TransmittingSystem = "EIS",
      RequestType = "CREATE",
      Regime = "CARF",
      RequestParameters = None
    )

  val createRcaspRequestIndividual: CreateRcaspRequest =
    CreateRcaspRequest(
      createRcasp.RcaspManagementRequest(
        RequestCommon = rcaspRequestCommon,
        RequestDetails = individualRcaspDetailsRequest
      )
    )

  val updateRcaspRequestIndividual: UpdateRcaspRequest =
    UpdateRcaspRequest(
      updateRcasp.RcaspManagementRequest(
        RequestCommon = rcaspRequestCommon,
        RequestDetails = updateIndividualRcaspDetailsRequest
      )
    )

  val deleteRcaspRequest: DeleteRcaspRequest =
    DeleteRcaspRequest(
      deleteRcasp.RcaspManagementRequest(
        RequestCommon = rcaspRequestCommon,
        RequestDetails = deleteRcaspDetailsRequest
      )
    )

  val createRcaspRequestRegisteredBusiness: CreateRcaspRequest =
    CreateRcaspRequest(
      createRcasp.RcaspManagementRequest(
        RequestCommon = rcaspRequestCommon,
        RequestDetails = registeredBusinessRcaspDetailsRequest
      )
    )

  val submitRcaspResponse: SubmitRcaspResponse =
    SubmitRcaspResponse(
      SubmitResponseDetails(
        SubmitReturnParameters(
          Key = "RCASPID",
          Value = rcaspId
        )
      )
    )

  val displaySubscriptionIndividual   =
    DisplaySubscriptionIndividual(firstName = "Joe", middleName = None, lastName = "Smith")
  val displaySubscriptionOrganisation = DisplaySubscriptionOrganisation(name = "Bobby")

  val testIndividualDisplaySubscriptionResponse = DisplaySubscriptionResponse(success =
    DisplaySubscriptionSuccess(
      processingDate = Instant.now(clock).toString,
      carfSubscriptionDetails = DisplaySubscriptionDetails(
        carfReference = carfId,
        tradingName = Some(testTradingName),
        gbUser = true,
        primaryContact = DisplaySubscriptionContact(
          individual = Some(displaySubscriptionIndividual),
          organisation = None,
          email = testEmail,
          phone = Some(testPhone),
          mobile = Some(testPhone)
        ),
        secondaryContact = None
      )
    )
  )

  def testOrganisationDisplaySubscriptionResponse(tradingName: Option[String]) =
    DisplaySubscriptionResponse(success =
      DisplaySubscriptionSuccess(
        processingDate = Instant.now(clock).toString,
        carfSubscriptionDetails = DisplaySubscriptionDetails(
          carfReference = carfId,
          tradingName = tradingName,
          gbUser = true,
          primaryContact = DisplaySubscriptionContact(
            individual = None,
            organisation = Some(DisplaySubscriptionOrganisation(name = "Bobby")),
            email = testEmail,
            phone = Some(testPhone),
            mobile = None
          ),
          secondaryContact = None
        )
      )
    )

  val testInvalidSubscriptionResponseNeither = DisplaySubscriptionResponse(success =
    DisplaySubscriptionSuccess(
      processingDate = Instant.now(clock).toString,
      carfSubscriptionDetails = DisplaySubscriptionDetails(
        carfReference = carfId,
        tradingName = Some(testTradingName),
        gbUser = true,
        primaryContact = DisplaySubscriptionContact(
          individual = None,
          organisation = None,
          email = testEmail,
          phone = Some(testPhone),
          mobile = None
        ),
        secondaryContact = None
      )
    )
  )

  val testInvalidSubscriptionResponseBoth = DisplaySubscriptionResponse(success =
    DisplaySubscriptionSuccess(
      processingDate = Instant.now(clock).toString,
      carfSubscriptionDetails = DisplaySubscriptionDetails(
        carfReference = carfId,
        tradingName = Some(testTradingName),
        gbUser = true,
        primaryContact = DisplaySubscriptionContact(
          individual = Some(displaySubscriptionIndividual),
          organisation = Some(displaySubscriptionOrganisation),
          email = testEmail,
          phone = Some(testPhone),
          mobile = None
        ),
        secondaryContact = None
      )
    )
  )

  lazy val testSummaryListRow: SummaryListRow =
    SummaryListRowViewModel(
      key = Key(Text("TEST Key")),
      value = ValueViewModel(Text("TEST Value")),
      actions = Seq(
        ActionItemViewModel(
          Text("TEST Action"),
          controllers.individual.routes.IndividualNameController.onPageLoad(ChangeMode).url
        ).withVisuallyHiddenText("TEST HIDDEN TEXT")
      )
    )

  lazy val testSection: Section = Section("TEST SECTION NAME", Seq(testSummaryListRow))
}
