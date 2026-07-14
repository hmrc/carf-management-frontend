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
import models.HomePageSubscriptionData
import models.errors.ApiError
import models.errors.ApiError.{InternalServerError, NotFoundError}
import models.viewAndUpdateRcasp.RcaspDetails
import play.api.Logging
import types.ResultT
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class AccountService @Inject (
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

  def getHomePageSubscriptionData(
      carfId: String
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): ResultT[HomePageSubscriptionData] =
    subscriptionConnector
      .displaySubscription(carfId)
      .leftMap { error =>
        logger.warn(s"[AccountService][getHomePageSubscriptionData] Error calling displaySubscription: $error")
        error
      }
      .subflatMap { displaySubscriptionResponse =>
        displaySubscriptionResponse.hasOrganisationContactDetailsMaybe.fold {
          logger.warn(
            s"[AccountService][getHomePageSubscriptionData] DisplaySubscriptionResponse has contact details for neither or both individual and organisation"
          )
          Left(InternalServerError)
        } { hasOrganisationContactDetails =>
          Right(
            HomePageSubscriptionData(
              hasOrganisationContactDetails = hasOrganisationContactDetails,
              organisationName =
                if (hasOrganisationContactDetails)
                  displaySubscriptionResponse.success.carfSubscriptionDetails.tradingName
                else None
            )
          )
        }
      }

  def getUserBusinessName(
      carfId: String
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): ResultT[Option[String]] =
    subscriptionConnector
      .displaySubscription(carfId)
      .leftMap { error =>
        logger.warn(s"[AccountService][getUserBusinessName] Error calling displaySubscription: $error")
        error
      }
      .subflatMap { displaySubscriptionResponse =>
        displaySubscriptionResponse.hasOrganisationContactDetailsMaybe.fold {
          logger.warn(
            "[AccountService][getUserBusinessName] DisplaySubscriptionResponse has contact details for neither or both individual and organisation"
          )
          Left(InternalServerError)
        } { hasOrganisationContactDetails =>
          val name =
            if (hasOrganisationContactDetails) {
              displaySubscriptionResponse.success.carfSubscriptionDetails.tradingName
            } else {
              displaySubscriptionResponse.success.carfSubscriptionDetails.primaryContact.individual.map { individual =>
                s"${individual.firstName} ${individual.lastName}"
              }
            }

          Right(name)
        }
      }
}