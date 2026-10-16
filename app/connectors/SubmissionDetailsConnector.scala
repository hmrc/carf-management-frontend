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
import play.api.http.Status.OK
import types.ResultT
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import utils.LoggerUtil.*

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

class SubmissionDetailsConnector @Inject() (httpClient: HttpClientV2, config: FrontendAppConfig) {

  def checkForSentFiles(carfId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): ResultT[Boolean] = {
    val requestUrl = url"${config.carfReportingBaseUrl}/check-for-sent-files/$carfId"

    logInfo(s"[SubmissionDetailsConnector][checkForSentFiles] Checking for sent files for carfId $carfId")

    ResultT.fromFuture {
      httpClient
        .get(requestUrl)
        .execute[HttpResponse]
        .map { response =>
          response.status match {
            case OK     =>
              Try(response.json.as[Boolean]) match {
                case Success(hasSentFiles) => Right(hasSentFiles)
                case Failure(_)            =>
                  logError(
                    s"[SubmissionDetailsConnector][checkForSentFiles] Error parsing response body for carfId $carfId"
                  )
                  Left(JsonValidationError)
              }
            case status =>
              logWarn(
                s"[SubmissionDetailsConnector][checkForSentFiles] Unexpected response: status $status, carfId $carfId"
              )
              Left(InternalServerError)
          }
        }
    }
  }

}
