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

package base

import controllers.actions.*
import generators.Generators
import models.countries.CountryUk
import models.individual.IndividualName
import models.requests.AddressDetails
import models.responses.{AddressRecord, AddressRegistrationResponse, AddressResponse, CountryRecord}
import models.{AddressAndUPRN, AddressUk, FindAddress, RichJsObject, UniqueTaxpayerReference, UserAnswers}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{BeforeAndAfterEach, OptionValues, TryValues}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.i18n.{Messages, MessagesApi}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.*
import play.api.mvc.PlayBodyParsers
import play.api.test.FakeRequest
import queries.{Gettable, Settable}
import repositories.SessionRepository
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier

import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

trait SpecBase
    extends AnyFreeSpec
    with GuiceOneAppPerSuite
    with Matchers
    with TryValues
    with OptionValues
    with ScalaFutures
    with IntegrationPatience
    with BeforeAndAfterEach
    with MockitoSugar
    with Generators {

  val userAnswersId: String            = "id"
  val testUtr: UniqueTaxpayerReference = UniqueTaxpayerReference("1234567890")
  val testInternalId: String           = "12345"
  val testCarfId: String               = "XE0000123456789"
  val testUPRN: Int                    = 123456789
  val testUPRNAlt: Int                 = 223456789

  private val UtcZoneId     = "UTC"
  implicit val clock: Clock = Clock.fixed(Instant.parse("2020-05-20T12:34:56.789012Z"), ZoneId.of(UtcZoneId))

  def emptyUserAnswers: UserAnswers =
    UserAnswers(id = userAnswersId, lastUpdated = Instant.now(clock))

  def messages(app: Application): Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())

  def injectedParsers: PlayBodyParsers = app.injector.instanceOf[PlayBodyParsers]

  final val mockSessionRepository: SessionRepository       = mock[SessionRepository]
  final val mockDataRetrievalAction: DataRetrievalAction   = mock[DataRetrievalAction]
  final val mockCtUtrRetrievalAction: CtUtrRetrievalAction = mock[CtUtrRetrievalAction]

  protected def applicationBuilder(
      userAnswers: Option[UserAnswers] = None,
      affinityGroup: AffinityGroup = AffinityGroup.Individual,
      requestUtr: Option[String] = None
  ): GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .overrides(
        bind[DataRequiredAction].to[DataRequiredActionImpl],
        bind[IdentifierAction].toInstance(new FakeIdentifierAction(injectedParsers, affinityGroup, requestUtr)),
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalActionProvider(userAnswers)),
        bind[SessionRepository].toInstance(mockSessionRepository)
      )

  implicit val hc: HeaderCarrier    = HeaderCarrier()
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  extension (userAnswers: UserAnswers) {

    def withPage[T](page: Settable[T] & Gettable[T], value: T)(implicit
        writes: Writes[T],
        rds: Reads[T]
    ): UserAnswers = {
      val updatedData = userAnswers.data.setObject(page.path, Json.toJson(value)) match {
        case JsSuccess(jsValue, _) =>
          Success(jsValue)
        case JsError(errors)       =>
          Failure(JsResultException(errors))
      }
      userAnswers.copy(data = updatedData.success.value)
    }

    def withoutPage[T](page: Settable[T])(implicit writes: Writes[T]): UserAnswers =
      userAnswers.remove(page).success.value

  }

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

  val testOrgName        = "Timmy Ltd"
  val testIndividualName = IndividualName("Timmy", "Jimmison")
  val testNiNumber       = "BA123456A"
  val testEmail          = "hi@example.com"
  val testPhone          = "07123456789"

  val testAddressDetails = AddressDetails(
    addressLine1 = "123 Test Street",
    addressLine2 = Some("Test Area"),
    addressLine3 = None,
    townOrCity = "Test City",
    postalCode = Some("TE5T 1NG"),
    countryCode = "GB"
  )

  val testAddressDetailsUk = AddressDetails(
    addressLine1 = "1 Test",
    addressLine2 = Some("Test Street"),
    addressLine3 = Some("Test Region"),
    townOrCity = "Testingtown",
    postalCode = Some(testPostcode),
    countryCode = "GB"
  )
}
