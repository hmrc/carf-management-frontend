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

package connectors

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.*
import itutil.ApplicationWithWiremock
import models.{RcaspAddress, RcaspContactDetails, TinDetails}
import models.errors.{InternalServerError, JsonValidationError}
import models.requests.{CreateRcaspRequest, RCASPManagementRequest, RcaspCreateRequestCommon, RequestParameter}
import models.responses
import models.requests
import models.responses.{RcaspResponseDetails, SubmitRcaspResponse, SubmitResponseDetails, SubmitReturnParameters, ViewRcasp, ViewRcaspResponse}
import org.scalactic.Prettifier.default
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import play.api.http.Status.*
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext

class RcaspConnectorISpec extends ApplicationWithWiremock with Matchers with ScalaFutures with IntegrationPatience {

  implicit val hc: HeaderCarrier    = HeaderCarrier()
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

  val connector: RcaspConnector = app.injector.instanceOf[RcaspConnector]

  val testCarfId: String = "CARF0000000001"

  val exampleContact        =
    RcaspContactDetails(ContactName = "Prof Sada", EmailAddress = "test@example.com", PhoneNumber = Some("07123412345"))
  val exampleCarfId         = "XCCAR0024000102"
  val exampleRcaspId        = "none"
  val exampleResponseCommon = responses.RcaspResponseCommon(
    OriginatingSystem = "CADX",
    TransmittingSystem = "EIS",
    RequestType = "VIEW",
    Regime = "CARF",
    ResponseParameters = None
  )

  val testViewRcaspResponse = ViewRcaspResponse(
    ViewRCASP = ViewRcasp(
      ResponseCommon = exampleResponseCommon,
      ResponseDetails = RcaspResponseDetails(
        RCASPList = List(
          responses.OrganisationRcaspDetails(
            SubscriptionID = exampleCarfId,
            RCASPID = exampleRcaspId,
            IsRCASPUser = true,
            PartyType = "Organisation",
            RCASPName = "Mesagoza",
            TradingName = "Uva Academy",
            TINDetails = Some(List(TinDetails(TINType = "UTR", TIN = "68936493", IssuedBy = "GB"))),
            AddressDetails = testAddressResponse,
            PrimaryContactDetails = Some(exampleContact),
            SecondaryContactDetails = Some(exampleContact.copy(ContactName = "Prof Turo"))
          )
        )
      )
    )
  )

  private def testAddressResponse = RcaspAddress(
    AddressLine1 = "64",
    AddressLine2 = Some("Zoo"),
    AddressLine3 = Some("Lane"),
    AddressLine4 = Some("Sixty Four"),
    PostalCode = "G66 2AZ",
    CountryCode = "GB"
  )

  val testViewRcaspResponseJson: String =
    """{
      |  "ViewRCASP": {
      |    "ResponseCommon": {
      |      "OriginatingSystem": "CADX",
      |      "TransmittingSystem": "EIS",
      |      "RequestType": "VIEW",
      |      "Regime": "CARF"
      |    },
      |    "ResponseDetails": {
      |      "RCASPList": [
      |        {
      |          "SubscriptionID": "XCCAR0024000102",
      |          "RCASPID": "none",
      |          "IsRCASPUser": true,
      |          "PartyType": "Organisation",
      |          "RCASPName": "Mesagoza",
      |          "TradingName": "Uva Academy",
      |          "TINDetails": [
      |            {
      |              "TINType": "UTR",
      |              "TIN": "68936493",
      |              "IssuedBy": "GB"
      |            }
      |          ],
      |          "AddressDetails": {
      |            "AddressLine1": "64",
      |            "AddressLine2": "Zoo",
      |            "AddressLine3": "Lane",
      |            "AddressLine4": "Sixty Four",
      |            "PostalCode": "G66 2AZ",
      |            "CountryCode": "GB"
      |          },
      |          "PrimaryContactDetails": {
      |            "ContactName": "Prof Sada",
      |            "EmailAddress": "test@example.com",
      |            "PhoneNumber": "07123412345"
      |          },
      |          "SecondaryContactDetails": {
      |            "ContactName": "Prof Turo",
      |            "EmailAddress": "test@example.com",
      |            "PhoneNumber": "07123412345"
      |          }
      |        }
      |      ]
      |    }
      |  }
      |}""".stripMargin

  "viewRcasp" should {

    val baseUrlPattern = s"/carf-management/view-rcasp/.*"

    "successfully retrieve a ViewRcaspResponse" in {
      stubFor(
        get(urlPathMatching(baseUrlPattern))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(testViewRcaspResponseJson)
          )
      )

      val result = connector.viewRcasp(testCarfId).value.futureValue
      result shouldBe Right(testViewRcaspResponse)
    }

    "return JsonValidationError when response JSON is invalid" in {
      stubFor(
        get(urlPathMatching(baseUrlPattern))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody("""{"incorrect": "structure"}""")
          )
      )

      val result = connector.viewRcasp(testCarfId).value.futureValue
      result shouldBe Left(JsonValidationError)
    }

    "return InternalServerError when backend returns 400" in {
      val errorResponse = Json.obj(
        "status"  -> "Bad request",
        "message" -> "Invalid request"
      )

      stubFor(
        get(urlPathMatching(baseUrlPattern))
          .willReturn(
            aResponse()
              .withStatus(BAD_REQUEST)
              .withBody(errorResponse.toString)
          )
      )

      val result = connector.viewRcasp(testCarfId).value.futureValue
      result shouldBe Left(InternalServerError)
    }

    "return InternalServerError when backend returns 422" in {
      val errorResponse = Json.obj(
        "status"  -> "Unrpocessable Entity",
        "message" -> "Invalid ID"
      )

      stubFor(
        get(urlPathMatching(baseUrlPattern))
          .willReturn(
            aResponse()
              .withStatus(UNPROCESSABLE_ENTITY)
              .withBody(errorResponse.toString)
          )
      )

      val result = connector.viewRcasp(testCarfId).value.futureValue
      result shouldBe Left(InternalServerError)
    }

    "return InternalServerError when backend returns 500" in {
      stubFor(
        get(urlPathMatching(baseUrlPattern))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
              .withBody(Json.obj("message" -> "Internal server error").toString)
          )
      )

      val result = connector.viewRcasp(testCarfId).value.futureValue
      result shouldBe Left(InternalServerError)
    }

    "return InternalServerError when backend returns 503" in {
      stubFor(
        get(urlPathMatching(baseUrlPattern))
          .willReturn(
            aResponse()
              .withStatus(SERVICE_UNAVAILABLE)
              .withBody(Json.obj("message" -> "Service unavailable").toString)
          )
      )

      val result = connector.viewRcasp(testCarfId).value.futureValue
      result shouldBe Left(InternalServerError)
    }

  }
  
  "createRcasp" should {

    val testUrl = "/carf-management/create"

    val createRcaspRequest: CreateRcaspRequest =
      CreateRcaspRequest(
        RCASPManagementRequest(
          RcaspCreateRequestCommon(
            OriginatingSystem = "CADX",
            TransmittingSystem = "EIS",
            RequestType = "VIEW",
            Regime = "CARF",
            RequestParameters = List(RequestParameter("key", "value"))
          ),
          requests.IndividualRcaspDetails(
            SubscriptionID = "XCARF000000001",
            IsRCASPUser = true,
            PartyType = "Individual",
            FirstName = "Penny",
            LastName = "Cassiopeia",
            TINDetails = Some(
              List(
                TinDetails(
                  TINType = "UTR",
                  TIN = "6893649",
                  IssuedBy = "GB"
                )
              )
            ),
            AddressDetails = RcaspAddress(
              AddressLine1 = "2 High Street",
              AddressLine2 = Some("Birmingham"),
              AddressLine3 = Some("Nowhereshire"),
              AddressLine4 = Some("Down the road"),
              PostalCode = "B23 2AZ",
              CountryCode = "GB"
            ),
            PrimaryContactDetails = Some(
              RcaspContactDetails(
                ContactName = "Penny Cassiopeia",
                EmailAddress = "penny.cassiopeia@uva.edu.org",
                PhoneNumber = Some("07123412345")
              )
            )
          )
        )
      )
      
    val submitStubResponse =
      """
        |{
        |  "ResponseDetails": {
        |    "ReturnParameters": {
        |      "Key": "RCASPID",
        |      "Value": "RCASP12345"
        |    }
        |  }
        |}
        |""".stripMargin

    val expectedResponse = SubmitRcaspResponse(
      SubmitResponseDetails(
        SubmitReturnParameters(
          "RCASPID", "RCASP12345"
        )
      )
    )
    
    "successfully retrieve a SubmitRcaspResponse" in {
      
      stubFor(
        post(urlPathMatching(testUrl))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(submitStubResponse)
          )
      )

      val result = connector.createRcasp(createRcaspRequest).value.futureValue
      result shouldBe Right(expectedResponse)
    }

    "return JsonValidationError when response JSON is invalid" in {
      stubFor(
        post(urlPathMatching(testUrl))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody("""{"incorrect": "structure"}""")
          )
      )

      val result = connector.createRcasp(createRcaspRequest).value.futureValue
      result shouldBe Left(JsonValidationError)
    }

    "return InternalServerError when backend returns 400" in {

      val errorResponse = Json.obj(
        "status" -> "Unrpocessable Entity",
        "message" -> "Invalid ID"
      )
      
      stubFor(
        post(urlPathMatching(testUrl))
          .willReturn(
            aResponse()
              .withStatus(UNPROCESSABLE_ENTITY)
              .withBody(errorResponse.toString)
          )
      )

      val result = connector.createRcasp(createRcaspRequest).value.futureValue
      result shouldBe Left(InternalServerError)
    }

    "return InternalServerError when backend returns 500" in {
      
      stubFor(
        post(urlPathMatching(testUrl))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
              .withBody(Json.obj("message" -> "Internal server error").toString)
          )
      )

      val result = connector.createRcasp(createRcaspRequest).value.futureValue
      result shouldBe Left(InternalServerError)
    }
  }

}
