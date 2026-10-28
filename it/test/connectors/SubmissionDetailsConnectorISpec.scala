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

import com.github.tomakehurst.wiremock.client.WireMock.*
import itutil.ApplicationWithWiremock
import models.errors.ApiError.{InternalServerError, JsonValidationError}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import play.api.http.Status.*

class SubmissionDetailsConnectorISpec
    extends ApplicationWithWiremock
    with Matchers
    with ScalaFutures
    with IntegrationPatience {

  lazy val connector: SubmissionDetailsConnector = app.injector.instanceOf[SubmissionDetailsConnector]

  ".checkForSentFiles" must {
    val baseUrlPattern = "/carf-reporting/check-for-sent-files/.*"

    "successfully retrieve a Boolean given a 200 response" in {
      stubFor(
        get(urlPathMatching(baseUrlPattern))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody("false")
          )
      )

      val result = connector.checkForSentFiles(testCarfId).value.futureValue

      result mustBe Right(false)
    }

    "return JsonValidationError when response JSON is invalid" in {
      stubFor(
        get(urlPathMatching(baseUrlPattern))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody("""{"not": "the expected shape"}""")
          )
      )

      val result = connector.checkForSentFiles(testCarfId).value.futureValue

      result mustBe Left(JsonValidationError)
    }

    "must return InternalServerError given a 400 response" in {
      stubFor(
        get(urlPathMatching(baseUrlPattern))
          .willReturn(
            aResponse()
              .withStatus(BAD_REQUEST)
          )
      )

      val result = connector.checkForSentFiles(testCarfId).value.futureValue

      result mustBe Left(InternalServerError)
    }

    "must return InternalServerError given a 500 response" in {
      stubFor(
        get(urlPathMatching(baseUrlPattern))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )

      val result = connector.checkForSentFiles(testCarfId).value.futureValue

      result mustBe Left(InternalServerError)
    }
  }
}
