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

package controllers

import controllers.actions.*
import pages.RcaspIdPage
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.RcaspAddedConfirmationView

import javax.inject.Inject

class RcaspAddedConfirmationController @Inject() (
    override val messagesApi: MessagesApi,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    val controllerComponents: MessagesControllerComponents,
    view: RcaspAddedConfirmationView
) extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad: Action[AnyContent] = (identify() andThen getData() andThen requireData) { implicit request =>
    val maybeRcaspId   = request.userAnswers.get(RcaspIdPage)
    val maybeRcaspName = request.userAnswers.retrieveRcaspName

    (maybeRcaspId, maybeRcaspName) match {
      case (Some(rcaspId), Some(name)) => Ok(view(rcaspId, name))
      case _                           =>
        logger.warn("[RcaspAddedConfirmationController][onPageLoad] Missing required data")
        Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
    }
  }
}
