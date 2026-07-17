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

import connectors.{RcaspConnector, SubscriptionConnector}
import models.UserBusinessSubscriptionData
import models.errors.ApiError.{InternalServerError, NotFoundError}
import models.requests.RcaspRequestCommon
import models.requests.deleteRcasp.{RcaspDetails as DeleteRcaspDetails, RcaspManagementRequest as DeleteRcaspManagementRequest, RcaspRequest as DeleteRcaspRequest}
import models.viewAndUpdateRcasp.RcaspDetails
import play.api.Logging
import types.ResultT
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class AccountService @Inject() (
    rcaspConnector: RcaspConnector,
    subscriptionConnector: SubscriptionConnector
) extends Logging {

  def getNumberOfRcaspsCurrentlyAdded(carfId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): ResultT[Int] =
    rcaspConnector
      .viewRcasp(carfId)
      .bimap(
        error => {
          logger.warn(s"[AccountService][getNumberOfRcaspsCurrentlyAdded] Error calling viewRcasp: $error")
          error
        },
        viewRcaspResponse => viewRcaspResponse.ViewRCASP.ResponseDetails.RCASPList.size
      )

  def getRcaspDetails(
      carfId: String,
      rcaspId: String
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): ResultT[RcaspDetails] =
    rcaspConnector
      .viewRcasp(carfId)
      .leftMap { error =>
        logger.warn(s"[AccountService][getRcaspDetails] Error calling viewRcasp: $error")
        error
      }
      .subflatMap { viewRcaspResponse =>
        viewRcaspResponse.ViewRCASP.ResponseDetails.RCASPList
          .find(_.RCASPID.toUpperCase == rcaspId.toUpperCase)
          .fold {
            logger.warn(s"[AccountService][getRcaspDetails] No RCASP found with ID $rcaspId")
            Left(NotFoundError)
          }(Right(_))
      }

  def getUserBusinessSubscriptionData(
      carfId: String
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): ResultT[UserBusinessSubscriptionData] =
    subscriptionConnector
      .displaySubscription(carfId)
      .leftMap { error =>
        logger.warn(s"[AccountService][getUserBusinessSubscriptionData] Error calling displaySubscription: $error")
        error
      }
      .subflatMap { displaySubscriptionResponse =>
        displaySubscriptionResponse.hasOrganisationContactDetailsMaybe.fold {
          logger.warn(
            "[AccountService][getUserBusinessSubscriptionData] DisplaySubscriptionResponse has contact details for neither or both individual and organisation"
          )
          Left(InternalServerError)
        } { hasOrganisationContactDetails =>
          Right(
            UserBusinessSubscriptionData(
              hasOrganisationContactDetails = hasOrganisationContactDetails,
              organisationName =
                if (hasOrganisationContactDetails)
                  displaySubscriptionResponse.success.carfSubscriptionDetails.tradingName
                else None
            )
          )
        }
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
          RequestType = "DELETE",
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
