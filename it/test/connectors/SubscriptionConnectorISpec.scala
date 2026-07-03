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
import models.errors.ApiError.*
import models.responses.*
import org.scalactic.Prettifier.default
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import play.api.http.Status.*
import play.api.libs.json.Json

class SubscriptionConnectorISpec
    extends ApplicationWithWiremock
    with Matchers
    with ScalaFutures
    with IntegrationPatience {

  val connector: SubscriptionConnector = app.injector.instanceOf[SubscriptionConnector]

  val testDisplaySubscriptionResponse = DisplaySubscriptionResponse(
    success = DisplaySubscriptionSuccess(
      processingDate = "2024-01-25T09:26:17Z",
      carfSubscriptionDetails = DisplaySubscriptionDetails(
        carfReference = carfId,
        tradingName = Some("CARF LTD"),
        gbUser = true,
        primaryContact = DisplaySubscriptionContact(
          individual = Some(
            DisplaySubscriptionIndividual(
              firstName = "Joe",
              middleName = None,
              lastName = "Smith"
            )
          ),
          email = "GroupRep@FATCACRS.com",
          phone = Some("01232473743"),
          mobile = Some("07232473743"),
          organisation = None
        ),
        secondaryContact = Some(
          DisplaySubscriptionContact(
            individual = Some(
              DisplaySubscriptionIndividual(
                firstName = "Martyn",
                middleName = None,
                lastName = "Smith"
              )
            ),
            email = "GroupRep@FATCACRS.com",
            phone = Some("01232473744"),
            mobile = Some("07232473744"),
            organisation = None
          )
        )
      )
    )
  )

  val testDisplaySubscriptionResponseJson: String =
    """
      |{
      |  "success": {
      |    "processingDate": "2024-01-25T09:26:17Z",
      |    "carfSubscriptionDetails": {
      |      "carfReference": "XCCAR0024000102",
      |      "tradingName": "CARF LTD",
      |      "gbUser": true,
      |      "primaryContact": {
      |        "individual": {
      |          "firstName": "Joe",
      |          "lastName": "Smith"
      |        },
      |        "email": "GroupRep@FATCACRS.com",
      |        "phone": "01232473743",
      |        "mobile": "07232473743"
      |      },
      |      "secondaryContact": {
      |        "individual": {
      |          "firstName": "Martyn",
      |          "lastName": "Smith"
      |        },
      |        "email": "GroupRep@FATCACRS.com",
      |        "phone": "01232473744",
      |        "mobile": "07232473744"
      |      }
      |    }
      |  }
      |}""".stripMargin

  "displaySubscription" should {

    val baseUrlPattern = "/carf-registration/subscription/display/.*"

    "successfully retrieve a DisplaySubscriptionResponse" in {
      stubFor(
        get(urlPathMatching(baseUrlPattern))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(testDisplaySubscriptionResponseJson)
          )
      )

      val result = connector.displaySubscription(carfId).value.futureValue
      result shouldBe Right(testDisplaySubscriptionResponse)
    }

    "return JsonValidationError when response JSON is invalid" in {
      stubFor(
        get(urlPathMatching(baseUrlPattern))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.toJson("invalid response").toString)
          )
      )

      val result = connector.displaySubscription(carfId).value.futureValue
      result shouldBe Left(JsonValidationError)
    }

    "return JsonValidationError when response JSON structure is incorrect" in {
      stubFor(
        get(urlPathMatching(baseUrlPattern))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody("""{"incorrect": "structure"}""")
          )
      )

      val result = connector.displaySubscription(carfId).value.futureValue
      result shouldBe Left(JsonValidationError)
    }

    "return NotFoundError when backend returns 404" in {
      val errorResponse = Json.obj(
        "status"  -> "Not Found",
        "message" -> "Not Found"
      )

      stubFor(
        get(urlPathMatching(baseUrlPattern))
          .willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
              .withBody(errorResponse.toString)
          )
      )

      val result = connector.displaySubscription(carfId).value.futureValue
      result shouldBe Left(NotFoundError)
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

      val result = connector.displaySubscription(carfId).value.futureValue
      result shouldBe Left(InternalServerError)
    }

    "return InternalServerError when backend returns 422" in {
      val errorResponse = Json.obj(
        "status"  -> "Unprocessable Entity",
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

      val result = connector.displaySubscription(carfId).value.futureValue
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

      val result = connector.displaySubscription(carfId).value.futureValue
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

      val result = connector.displaySubscription(carfId).value.futureValue
      result shouldBe Left(InternalServerError)
    }

  }
}
