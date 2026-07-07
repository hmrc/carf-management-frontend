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
import models.errors.ApiError.InternalServerError
import models.{UniqueTaxpayerReference, UserAnswers}
import models.errors.MandatoryInformationMissingError
import models.requests.CreateRcaspRequest
import models.responses.{SubmitRcaspResponse, SubmitResponseDetails, SubmitReturnParameters}
import play.api.Logging
import types.ResultT
import uk.gov.hmrc.http.HeaderCarrier
import utils.RcaspSubmissionHelper

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class RcaspSubmissionService @Inject (
    rcaspConnector: RcaspConnector,
    rcaspSubmissionHelper: RcaspSubmissionHelper
) extends Logging {

  def submitRegisteredBusinessRcasp(
      carfId: String,
      utr: UniqueTaxpayerReference,
      userAnswers: UserAnswers
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): ResultT[SubmitRcaspResponse] =
    rcaspSubmissionHelper.createRcaspRequestForRegisteredBusiness(carfId, utr, userAnswers) match {
      case Some(request) =>
        rcaspConnector
          .createRcasp(request)
          .leftMap { error =>
            logger.warn(s"[RcaspSubmissionService][submitRegisteredBusinessRcasp] Failed to add RCASP: $error")
            error
          }
      case None          =>
        logger.warn(
          "[RcaspSubmissionService][submitRegisteredBusinessRcasp] Error building the CreateRcaspRequest from userAnswers"
        )
        ResultT.fromError(MandatoryInformationMissingError("Error building the CreateRcaspRequest from userAnswers"))
    }

  def submitRcasp(
      carfId: String,
      userAnswers: UserAnswers
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): ResultT[SubmitRcaspResponse] =
    rcaspSubmissionHelper.createRcaspRequest(carfId, userAnswers) match {
      case Some(request) =>
        rcaspConnector
          .createRcasp(request)
          .leftMap { error =>
            logger.warn(s"[RcaspSubmissionService][submitRcasp] Failed to add RCASP: $error")
            error
          }
      case None          =>
        logger.warn("[RcaspSubmissionService][submitRcasp] Error building the CreateRcaspRequest from userAnswers")
        ResultT.fromError(MandatoryInformationMissingError("Error building the CreateRcaspRequest from userAnswers"))
    }

  // TODO: Replace with actual call to update RCASP
  def updateRcasp(
      carfId: String,
      userAnswers: UserAnswers
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): ResultT[SubmitRcaspResponse] =
    carfId.takeRight(1) match {
      case "4" | "5" | "6" | "7" | "8" | "9" => ResultT.fromError(InternalServerError)
      case _                                 =>
        ResultT.fromValue(
          SubmitRcaspResponse(SubmitResponseDetails(SubmitReturnParameters(Key = "RCASPID", Value = "ZMCAR0123456789")))
        )
    }
}
