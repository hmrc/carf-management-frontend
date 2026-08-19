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
import models.responses.{CreateRcaspResponse, UpdateDeleteRcaspResponse}
import models.{UniqueTaxpayerReference, UserAnswers}
import types.ResultT
import uk.gov.hmrc.http.HeaderCarrier
import utils.LoggerUtil.*
import utils.RcaspSubmissionHelper

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class RcaspSubmissionService @Inject (
    rcaspConnector: RcaspConnector,
    rcaspSubmissionHelper: RcaspSubmissionHelper
) {

  def createRegisteredBusinessRcasp(
      carfId: String,
      utr: UniqueTaxpayerReference,
      userAnswers: UserAnswers
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): ResultT[CreateRcaspResponse] =
    rcaspSubmissionHelper.createRegisteredBusinessRcaspRequest(carfId, utr, userAnswers) match {
      case Some(request) =>
        rcaspConnector
          .createRcasp(request)
          .leftMap { error =>
            logWarn(s"[RcaspSubmissionService][createRegisteredBusinessRcasp] Failed to add RCASP: $error")
            error
          }
      case None          =>
        logWarn(
          "[RcaspSubmissionService][createRegisteredBusinessRcasp] Error building the RcaspRequest from userAnswers"
        )
        ResultT.fromError(MandatoryInformationMissingError("Error building the RcaspRequest from userAnswers"))
    }

  def createRcasp(
      carfId: String,
      userAnswers: UserAnswers
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): ResultT[CreateRcaspResponse] =
    rcaspSubmissionHelper.createRcaspRequest(carfId, userAnswers) match {
      case Some(request) =>
        rcaspConnector
          .createRcasp(request)
          .leftMap { error =>
            logWarn(s"[RcaspSubmissionService][createRcasp] Failed to add RCASP: $error")
            error
          }
      case None          =>
        logWarn("[RcaspSubmissionService][createRcasp] Error building the RcaspRequest from userAnswers")
        ResultT.fromError(MandatoryInformationMissingError("Error building the RcaspRequest from userAnswers"))
    }

  def updateRegisteredBusinessRcasp(
      carfId: String,
      utr: UniqueTaxpayerReference,
      userAnswers: UserAnswers
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): ResultT[UpdateDeleteRcaspResponse] =
    rcaspSubmissionHelper.updateRegisteredBusinessRcaspRequest(carfId, utr, userAnswers) match {
      case Some(request) =>
        rcaspConnector
          .updateRcasp(request)
          .leftMap { error =>
            logWarn(s"[RcaspSubmissionService][updateRegisteredBusinessRcasp] Failed to update RCASP: $error")
            error
          }
      case None          =>
        logWarn(
          "[RcaspSubmissionService][updateRegisteredBusinessRcasp] Error building the RcaspRequest from userAnswers"
        )
        ResultT.fromError(MandatoryInformationMissingError("Error building the RcaspRequest from userAnswers"))
    }

  def updateRcasp(
      carfId: String,
      userAnswers: UserAnswers
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): ResultT[UpdateDeleteRcaspResponse] =
    rcaspSubmissionHelper.updateRcaspRequest(carfId, userAnswers) match {
      case Some(request) =>
        rcaspConnector
          .updateRcasp(request)
          .leftMap { error =>
            logWarn(s"[RcaspSubmissionService][updateRcasp] Failed to update RCASP: $error")
            error
          }
      case None          =>
        logWarn("[RcaspSubmissionService][updateRcasp] Error building the RcaspRequest from userAnswers")
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
          logWarn(s"[AccountService][removeRcasp] Error calling deleteRcasp: $error")
          error
        },
        _ => ()
      )
  }
}
