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
import play.api.Logging
import types.ResultT
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class AccountService @Inject (
    rcaspConnector: RcaspConnector
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

  def hasOrganisationContactDetails(carfId: String): ResultT[Boolean] =
    carfId.dropRight(2).last.toString match {
      case "9" =>
        logger.warn("[hasOrganisationContactDetails] Error!")
        ResultT.fromError(InternalServerError)
      case "1" => ResultT.fromValue(true)
      case _   => ResultT.fromValue(false)
    }

  def getOrganisationName(carfId: String): ResultT[Option[String]] =
    carfId.dropRight(3).last.toString match {
      case "9" =>
        logger.warn("[getOrganisationName] Error!")
        ResultT.fromError(InternalServerError)
      case "8" => ResultT.fromValue(None)
      case _   => ResultT.fromValue(Some("Timmy's Turtles Ltd"))
    }

}
