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

package controllers.organisation

import controllers.actions._
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.organisation.NotInUkView

import javax.inject.Inject

class NotInUkController @Inject() (
    override val messagesApi: MessagesApi,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    val controllerComponents: MessagesControllerComponents,
    view: NotInUkView
) extends FrontendBaseController
    with I18nSupport
    with Logging
    with RcaspNameHelper {

  def onPageLoad(): Action[AnyContent] =
    (identify() andThen getData() andThen requireData) { implicit request =>
      rcaspDisplayName(request.userAnswers) match {
        case Some(rcaspName) =>
          Ok(view(rcaspName))

        case None =>
          logger.warn(
            "[NotInUkController][onPageLoad] " +
              "No RCASP name found in UserAnswers. Redirecting to journey recovery."
          )
          Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      }
    }
}
