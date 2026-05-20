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
class UploadInformationService @Inject() ()(implicit ec: ExecutionContext) extends Logging {

  def hasUserUploadedFilesInLast28Days(carfId: String): Future[Boolean] =
    carfId.dropRight(1).last.toString match {
      case "9" => Future.failed(new Exception("[hasUserUploadedFilesInLast28Days] Error!"))
      case "1" => Future.successful(true)
      case _   => Future.successful(false)
    }

}
