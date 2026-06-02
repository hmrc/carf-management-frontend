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

@Singleton
class UploadInformationService @Inject() extends Logging {

  def hasUserUploadedFilesInLast28Days(carfId: String): ResultT[Boolean] =
    carfId.dropRight(1).last.toString match {
      case "9" =>
        logger.warn("[hasUserUploadedFilesInLast28Days] Error!")
        ResultT.fromError(InternalServerError)
      case "1" => ResultT.fromValue(true)
      case _   => ResultT.fromValue(false)
    }

}
