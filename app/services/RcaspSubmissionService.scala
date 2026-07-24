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
import models.errors.MandatoryInformationMissingError
import models.requests.deleteRcasp.{RcaspDetails as DeleteRcaspDetails, RcaspManagementRequest as DeleteRcaspManagementRequest, RcaspRequest as DeleteRcaspRequest}
import models.requests.{RcaspRequestCommon, RequestType}
import models.responses.SubmitRcaspResponse
import models.{UniqueTaxpayerReference, UserAnswers}
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

  def createRegisteredBusinessRcasp(
      carfId: String,
      utr: UniqueTaxpayerReference,
      userAnswers: UserAnswers
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): ResultT[SubmitRcaspResponse] =
    rcaspSubmissionHelper.createRegisteredBusinessRcaspRequest(carfId, utr, userAnswers) match {
      case Some(request) =>
        rcaspConnector
          .createRcasp(request)
          .leftMap { error =>
            logger.warn(s"[RcaspSubmissionService][createRegisteredBusinessRcasp] Failed to add RCASP: $error")
            error
          }
      case None          =>
        logger.warn(
          "[RcaspSubmissionService][createRegisteredBusinessRcasp] Error building the RcaspRequest from userAnswers"
        )
        ResultT.fromError(MandatoryInformationMissingError("Error building the RcaspRequest from userAnswers"))
    }

  def createRcasp(
      carfId: String,
      userAnswers: UserAnswers
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): ResultT[SubmitRcaspResponse] =
    rcaspSubmissionHelper.createRcaspRequest(carfId, userAnswers) match {
      case Some(request) =>
        rcaspConnector
          .createRcasp(request)
          .leftMap { error =>
            logger.warn(s"[RcaspSubmissionService][createRcasp] Failed to add RCASP: $error")
            error
          }
      case None          =>
        logger.warn("[RcaspSubmissionService][createRcasp] Error building the RcaspRequest from userAnswers")
        ResultT.fromError(MandatoryInformationMissingError("Error building the RcaspRequest from userAnswers"))
    }

  def updateRegisteredBusinessRcasp(
      carfId: String,
      utr: UniqueTaxpayerReference,
      userAnswers: UserAnswers
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): ResultT[SubmitRcaspResponse] =
    rcaspSubmissionHelper.updateRegisteredBusinessRcaspRequest(carfId, utr, userAnswers) match {
      case Some(request) =>
        rcaspConnector
          .updateRcasp(request)
          .leftMap { error =>
            logger.warn(s"[RcaspSubmissionService][updateRegisteredBusinessRcasp] Failed to update RCASP: $error")
            error
          }
      case None          =>
        logger.warn(
          "[RcaspSubmissionService][updateRegisteredBusinessRcasp] Error building the RcaspRequest from userAnswers"
        )
        ResultT.fromError(MandatoryInformationMissingError("Error building the RcaspRequest from userAnswers"))
    }

  def updateRcasp(
      carfId: String,
      userAnswers: UserAnswers
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): ResultT[SubmitRcaspResponse] =
    rcaspSubmissionHelper.updateRcaspRequest(carfId, userAnswers) match {
      case Some(request) =>
        rcaspConnector
          .updateRcasp(request)
          .leftMap { error =>
            logger.warn(s"[RcaspSubmissionService][updateRcasp] Failed to update RCASP: $error")
            error
          }
      case None          =>
        logger.warn("[RcaspSubmissionService][updateRcasp] Error building the RcaspRequest from userAnswers")
        ResultT.fromError(MandatoryInformationMissingError("Error building the RcaspRequest from userAnswers"))
    }

  def removeRcasp(
      carfId: String,
      rcaspId: String
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): ResultT[Unit] = {

    val deleteRequest = DeleteRcaspRequest(
      RCASPManagement = DeleteRcaspManagementRequest(
        RequestCommon = RcaspRequestCommon(
          OriginatingSystem = "MDTP",
          TransmittingSystem = "EIS",
          RequestType = RequestType.Delete.name,
          Regime = "CARF",
          RequestParameters = None
        ),
        RequestDetails = DeleteRcaspDetails(
          RCASPID = rcaspId,
          SubscriptionID = carfId
        )
      )
    )

    rcaspConnector
      .deleteRcasp(deleteRequest)
      .bimap(
        error => {
          logger.warn(s"[AccountService][removeRcasp] Error calling deleteRcasp: $error")
          error
        },
        _ => ()
      )
  }
}
