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

import models.responses.{SubmitRcaspResponse, SubmitResponseDetails, SubmitReturnParameters}
import play.api.Logging
import types.ResultT

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class SubmitRcaspService @Inject() ()(implicit ec: ExecutionContext) extends Logging {

  // TODO - Current impl is to allow RcaspAddedConfirmationController to retrieve stubbed rcaspId, this will change in CARF-294/CARF-295
  def submitRcasp(): ResultT[SubmitRcaspResponse] =
    ResultT.fromValue(
      SubmitRcaspResponse(
        ResponseDetails = SubmitResponseDetails(
          ReturnParameters = SubmitReturnParameters(Key = "RCASPID", Value = "XACARF0000123456")
        )
      )
    )
}
