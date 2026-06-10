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

package testOnly.controllers

import com.google.inject.Inject
import connectors.RcaspConnector
import models.responses.ViewRcaspResponse
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import types.ResultT
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import scala.concurrent.ExecutionContext

class RcaspController @Inject() (
    connector: RcaspConnector,
    val controllerComponents: MessagesControllerComponents,
    implicit val executionContext: ExecutionContext
) extends FrontendBaseController
    with I18nSupport {

  def viewRcasp(carfId: String): Action[AnyContent] = Action.async { implicit request =>
    connector.viewRcasp(carfId).processResponse
  }

  extension (result: ResultT[ViewRcaspResponse]) {
    private def processResponse =
      result.value
        .map {
          case Right(data) => Ok(Json.prettyPrint(Json.toJson(data)))
          case Left(error) => Ok(error.toString)
        }
  }
}
