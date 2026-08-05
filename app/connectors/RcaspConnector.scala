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
import models.errors.CarfError
import models.requests.createRcasp
import models.requests.createRcasp.RcaspRequest as CreateRcaspRequest
import models.requests.deleteRcasp.RcaspRequest as DeleteRcaspRequest
import models.requests.updateRcasp.RcaspRequest as UpdateRcaspRequest
import models.responses.*
import models.viewAndUpdateRcasp
import utils.LoggerUtil.*
import play.api.http.Status.*
import play.api.libs.json.*
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import types.ResultT
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import java.net.URL
import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

class RcaspConnector @Inject() (val config: FrontendAppConfig, val http: HttpClientV2) {

  def viewRcasp(
      carfId: String
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): ResultT[List[viewAndUpdateRcasp.RcaspDetails]] = {
    val baseUrl = url"${config.carfManagementBaseUrl}/view-rcasp/$carfId/none"

    logDebug(s"[RcaspConnector] Viewing RCASP with ID: $carfId")

    ResultT.fromFuture {
      http
        .get(baseUrl)
        .execute[HttpResponse]
        .map {
          case response if response.status == OK =>
            Try(response.json.as[ViewRcaspResponse]) match {
              case Success(viewRcaspResponse) => Right(viewRcaspResponse.ViewRCASP.ResponseDetails.RCASPList)
              case Failure(exception)         =>
                logParseWarning(baseUrl, "viewRcasp")
                Left(JsonValidationError)
            }
          case res if res.status == NOT_FOUND    =>
            logInfo(s"No RCASPs found for carfId $carfId")
            Right(List.empty)
          case response                          =>
            logWarn(s"Unexpected response: status code: ${response.status}, from endpoint: ${baseUrl.toURI}")
            Left(InternalServerError)
        }
    }
  }

  def createRcasp(
      createRcaspRequest: CreateRcaspRequest
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): ResultT[SubmitRcaspResponse] = {
    val baseUrl = url"${config.carfManagementBaseUrl}/create"

    logDebug("[RcaspConnector] Creating RCASP")

    sendRequest(
      url = baseUrl,
      requestBuilder = http.post(baseUrl).withBody(Json.toJson(createRcaspRequest))
    ) { httpResponse =>
      Try(httpResponse.json.as[SubmitRcaspResponse]) match {
        case Success(data)      => Right(data)
        case Failure(exception) =>
          logParseWarning(baseUrl, "createRcasp")
          Left(JsonValidationError)
      }
    }
  }

  def updateRcasp(
      updateRcaspRequest: UpdateRcaspRequest
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): ResultT[SubmitRcaspResponse] = {
    val baseUrl = url"${config.carfManagementBaseUrl}/update"

    logDebug("[RcaspConnector] Updating RCASP")

    sendRequest(
      url = baseUrl,
      requestBuilder = http.post(baseUrl).withBody(Json.toJson(updateRcaspRequest))
    ) { httpResponse =>
      Try(httpResponse.json.as[SubmitRcaspResponse]) match {
        case Success(data)      => Right(data)
        case Failure(exception) =>
          logParseWarning(baseUrl, "updateRcasp")
          Left(JsonValidationError)
      }
    }
  }

  def deleteRcasp(
      deleteRcaspRequest: DeleteRcaspRequest
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): ResultT[SubmitRcaspResponse] = {
    val baseUrl = url"${config.carfManagementBaseUrl}/delete"

    logDebug("[RcaspConnector] Deleting RCASP")

    sendRequest(
      url = baseUrl,
      requestBuilder = http.delete(baseUrl).withBody(Json.toJson(deleteRcaspRequest))
    ) { httpResponse =>
      Try(httpResponse.json.as[SubmitRcaspResponse]) match {
        case Success(data)      => Right(data)
        case Failure(exception) =>
          logParseWarning(baseUrl, "deleteRcasp")
          Left(JsonValidationError)
      }
    }
  }

  private def sendRequest[T](url: URL, requestBuilder: RequestBuilder)(
      successfulResult: HttpResponse => Either[CarfError, T]
  )(implicit ec: ExecutionContext): ResultT[T] = {
    logInfo(s"Calling endpoint: ${url.toString}")

    ResultT.fromFuture(
      requestBuilder
        .execute[HttpResponse]
        .map { httpResponse =>
          httpResponse.status match {
            case OK => successfulResult(httpResponse)
            case _  =>
              logWarn(s"Unexpected response: status code: ${httpResponse.status}, from endpoint: ${url.toURI}")
              Left(InternalServerError)
          }
        }
    )
  }

  private def logParseWarning(baseUrl: URL, originatingMethod: String): Unit =
    logWarn(
      s"[RcaspConnector][$originatingMethod] Error parsing SubmitRcaspResponse with endpoint: ${baseUrl.toURI}"
    )
}
