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

package services

import connectors.RcaspConnector
import models.UserAnswers
import models.errors.ApiError.InternalServerError
import models.responses.{SubmitRcaspResponse, SubmitResponseDetails, SubmitReturnParameters}
import play.api.Logging
import types.ResultT
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class RcaspSubmissionService @Inject (
    rcaspConnector: RcaspConnector
) extends Logging {

  def submitRcasp(
      carfId: String,
      userAnswers: UserAnswers
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): ResultT[SubmitRcaspResponse] =
    // TODO: Create actual request and send to stubs [CARF-572]
    carfId.takeRight(1) match {
      case "0" => ResultT.fromError(InternalServerError)
      case _   =>
        ResultT.fromValue(
          SubmitRcaspResponse(
            ResponseDetails = SubmitResponseDetails(
              ReturnParameters = SubmitReturnParameters(Key = "RCASPID", Value = "XACARF0000123456")
            )
          )
        )
    }
}
