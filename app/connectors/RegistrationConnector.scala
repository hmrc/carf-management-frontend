/*
 * Copyright 2025 HM Revenue & Customs
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

import cats.data.EitherT
import com.google.inject.Inject
import config.FrontendAppConfig
import models.errors.ApiError
import models.requests.RegisterWithIdRequest
import models.responses.RegisterWithIdResponse
import play.api.Logging
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import types.ResultT
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

class RegistrationConnector @Inject() (val config: FrontendAppConfig, val http: HttpClientV2)(implicit
    ec: ExecutionContext
) extends Logging {

  private val backendBaseUrl = config.carfRegistrationBaseUrl

  def registerOrganisationWithUtrCtAutoMatch(
      request: RegisterWithIdRequest
  )(implicit hc: HeaderCarrier): ResultT[RegisterWithIdResponse] =
    val url = url"$backendBaseUrl/organisation/utr/ct-auto-match"

    EitherT {
      http
        .post(url)
        .withBody(Json.toJson(request))
        .execute[HttpResponse]
        .map {
          case response if response.status == OK        =>
            Try(response.json.as[RegisterWithIdResponse]) match {
              case Success(data)      => Right(data)
              case Failure(exception) =>
                logger.warn(s"Error parsing RegisterWithIdResponse with endpoint: ${url.toURI}")
                Left(ApiError.JsonValidationError)
            }
          case response if response.status == NOT_FOUND =>
            logger.warn(
              s"No match found for organisation: status code: ${response.status}, from endpoint: ${url.toURI}"
            )
            Left(ApiError.NotFoundError)
          case response                                 =>
            logger.warn(
              s"Unexpected response for organisation: status code: ${response.status}, from endpoint: ${url.toURI}"
            )
            Left(ApiError.InternalServerError)
        }
    }
}
