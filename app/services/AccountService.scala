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

import play.api.Logging

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AccountService @Inject() ()(implicit ec: ExecutionContext) extends Logging {

  def getNumberOfRcaspsCurrentlyAdded(carfId: String): Future[Int] =
    carfId.last.toString match {
      case "9" => Future.failed(new Exception("[getNumberOfRcaspsCurrentlyAdded] Error!"))
      case "0" => Future.successful(0)
      case "1" => Future.successful(1)
      case _   => Future.successful(2)
    }

  def hasOrganisationContactDetails(carfId: String): Future[Boolean] =
    carfId.dropRight(2).last.toString match {
      case "9" => Future.failed(new Exception("[hasOrganisationContactDetails] Error!"))
      case "1" => Future.successful(true)
      case _   => Future.successful(false)
    }

  def getOrganisationName(carfId: String): Future[String] =
    carfId.dropRight(3).last.toString match {
      case "9" => Future.failed(new Exception("[getOrganisationName] Error!"))
      case _   => Future.successful("Timmy's Turtles Ltd")
    }

}
