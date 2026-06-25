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

import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import pages.organisation.OverwritableOrganisationName
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.CheckDetailsRegBusinessHelper
import views.html.organisation.CheckDetailsRegBusinessView
import services.RegistrationService
import scala.concurrent.ExecutionContext

import javax.inject.Inject

class CheckDetailsRegBusinessController @Inject() (
    override val messagesApi: MessagesApi,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    view: CheckDetailsRegBusinessView,
    val controllerComponents: MessagesControllerComponents,
    helper: CheckDetailsRegBusinessHelper,
    registrationService: RegistrationService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad: Action[AnyContent] = (identify() andThen getData() andThen requireData) { implicit request =>
    import cats.syntax.all.*

    val userAnswers          = request.userAnswers
    lazy val ifEmptyProtocol =
      Redirect(controllers.routes.InformationMissingController.onPageLoad())

    (
      userAnswers.get(OverwritableOrganisationName),
      helper.getRegisteredBusinessSection(userAnswers)
    )
      .mapN { (name, section) =>
        Ok(view(Seq(section), name))
      }
      .getOrElse {
        logger.warn(
          "[CheckDetailsRegBusinessController][onPageLoad] Error! Could not load page missing answers"
        )
        ifEmptyProtocol
      }
  }

  def onSubmit: Action[AnyContent] = (identify() andThen getData() andThen requireData).async { implicit request =>
    registrationService.registerRcasp("stub").value.map {
      case Right(_)    =>
        Redirect(controllers.routes.PlaceholderController.onPageLoad("[CARF-296] RCASP added page - /rcasp-added"))
      case Left(error) =>
        logger.warn(s"[CheckDetailsRegBusinessController][onSubmit] Failed to register RCASP: $error")
        Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
    }
  }
}
