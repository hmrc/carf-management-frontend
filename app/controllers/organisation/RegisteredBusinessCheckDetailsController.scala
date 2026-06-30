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

import cats.syntax.all.*
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import pages.organisation.{OverwritableOrganisationName, ReportForRegisteredBusinessPage}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.RegistrationService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.CheckDetailsRegisteredBusinessHelper
import views.html.organisation.CheckDetailsRegBusinessView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class RegisteredBusinessCheckDetailsController @Inject() (
    override val messagesApi: MessagesApi,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    view: CheckDetailsRegBusinessView,
    val controllerComponents: MessagesControllerComponents,
    helper: CheckDetailsRegisteredBusinessHelper,
    registrationService: RegistrationService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad: Action[AnyContent] = (identify() andThen getData() andThen requireData) { implicit request =>
    val userAnswers          = request.userAnswers
    lazy val ifEmptyProtocol = Redirect(controllers.routes.InformationMissingController.onPageLoad())

    userAnswers.get(ReportForRegisteredBusinessPage) match {
      case Some(true) =>
        (
          userAnswers.get(OverwritableOrganisationName),
          helper.getRegisteredBusinessSection(userAnswers)
        )
          .mapN { (name, section) =>
            Ok(view(section, name))
          }
          .getOrElse {
            logger.warn(
              "[CheckDetailsRegBusinessController][onPageLoad] Error! Could not load page missing answers"
            )
            ifEmptyProtocol
          }

      case _ =>
        logger.warn(
          "[CheckDetailsRegBusinessController][onPageLoad] ReportForRegisteredBusiness is false or missing. Redirecting to SIIM."
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
