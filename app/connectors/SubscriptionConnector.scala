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

import config.FrontendAppConfig
import models.errors.ApiError.{InternalServerError, JsonValidationError, NotFoundError}
import models.responses.DisplaySubscriptionResponse
import utils.LoggerUtil.*
import play.api.http.Status.{NOT_FOUND, OK}
import types.ResultT
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

class SubscriptionConnector @Inject() (val config: FrontendAppConfig, val http: HttpClientV2) {

  def displaySubscription(
      carfId: String
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): ResultT[DisplaySubscriptionResponse] = {
    val baseUrl = url"${config.carfRegistrationBaseUrl}/subscription/display/$carfId"

    logDebug(
      s"[SubscriptionConnector] Displaying subscription with ID: $carfId"
    )

    ResultT.fromFuture(
      http
        .get(baseUrl)
        .execute[HttpResponse]
        .map { httpResponse =>
          httpResponse.status match {
            case OK        =>
              Try(httpResponse.json.as[DisplaySubscriptionResponse]) match {
                case Success(data)      => Right(data)
                case Failure(exception) =>
                  logWarn(s"Error parsing DisplaySubscriptionResponse with endpoint: ${baseUrl.toURI}")
                  Left(JsonValidationError)
              }
            case NOT_FOUND =>
              logWarn(
                s"No match could be found for carfId $carfId. Status code: ${httpResponse.status}, from endpoint: ${baseUrl.toURI}"
              )
              Left(NotFoundError)
            case _         =>
              logWarn(s"Unexpected response. Status code: ${httpResponse.status}, from endpoint: ${baseUrl.toURI}")
              Left(InternalServerError)
          }
        }
    )
  }
}
