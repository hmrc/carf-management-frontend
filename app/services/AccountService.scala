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

import models.errors.InternalServerError
import play.api.Logging
import types.ResultT

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AccountService @Inject() ()(implicit ec: ExecutionContext) extends Logging {

  def getNumberOfRcaspsCurrentlyAdded(carfId: String): ResultT[Int] =
    carfId.last.toString match {
      case "9" =>
        logger.warn("[getNumberOfRcaspsCurrentlyAdded] Error!")
        ResultT.fromError(InternalServerError)
      case "0" => ResultT.fromValue(0)
      case "1" => ResultT.fromValue(1)
      case _   => ResultT.fromValue(2)
    }

  def hasOrganisationContactDetails(carfId: String): ResultT[Boolean] =
    carfId.dropRight(2).last.toString match {
      case "9" =>
        logger.warn("[hasOrganisationContactDetails] Error!")
        ResultT.fromError(InternalServerError)
      case "1" => ResultT.fromValue(true)
      case _   => ResultT.fromValue(false)
    }

  def getOrganisationName(carfId: String): ResultT[String] =
    carfId.dropRight(3).last.toString match {
      case "9" =>
        logger.warn("[getOrganisationName] Error!")
        ResultT.fromError(InternalServerError)
      case _   => ResultT.fromValue("Timmy's Turtles Ltd")
    }

}
