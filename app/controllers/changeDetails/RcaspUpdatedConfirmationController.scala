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

package controllers.changeDetails

import controllers.actions.*
import pages.SubmissionSucceededPage
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.changeDetails.RcaspUpdatedConfirmationView

import javax.inject.Inject

class RcaspUpdatedConfirmationController @Inject() (
    override val messagesApi: MessagesApi,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    val controllerComponents: MessagesControllerComponents,
    view: RcaspUpdatedConfirmationView
) extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad: Action[AnyContent] = (identify() andThen getData() andThen requireData) { implicit request =>
    val submissionSucceeded = request.userAnswers.get(SubmissionSucceededPage).contains(true)
    val maybeRcaspName      = request.userAnswers.retrieveRcaspName

    maybeRcaspName match {
      case Some(name) if submissionSucceeded =>
        Ok(view(name))
      case _                                 =>
        logger.warn(
          "[RcaspUpdatedConfirmationController][onPageLoad] Missing submission flag or RCASP name in user answers"
        )
        Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
    }
  }
}
