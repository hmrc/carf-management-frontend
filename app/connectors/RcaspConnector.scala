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
import models.errors.ApiError.{InternalServerError, JsonValidationError}
import models.responses.ViewRcaspResponse
import play.api.Logging
import play.api.http.Status.OK
import types.ResultT
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

class RcaspConnector @Inject() (val config: FrontendAppConfig, val http: HttpClientV2) extends Logging {

  def viewRcasp(
      carfId: String
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): ResultT[ViewRcaspResponse] = {
    val baseUrl = url"${config.carfManagementBaseUrl}/view-rcasp/$carfId/none"

    logger.debug(
      s"[RcaspConnector] Viewing RCASP with ID: $carfId"
    )

    ResultT.fromFuture(
      http
        .get(baseUrl)
        .execute[HttpResponse]
        .map { httpResponse =>
          httpResponse.status match {
            case OK =>
              Try(httpResponse.json.as[ViewRcaspResponse]) match {
                case Success(data)      => Right(data)
                case Failure(exception) =>
                  logger.warn(s"Error parsing ViewRcaspResponse with endpoint: ${baseUrl.toURI}")
                  Left(JsonValidationError)
              }
            case _  =>
              logger.warn(s"Unexpected response: status  code: ${httpResponse.status}, from endpoint: ${baseUrl.toURI}")
              Left(InternalServerError)
          }
        }
    )
  }
}
