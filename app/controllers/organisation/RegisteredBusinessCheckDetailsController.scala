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
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction, SubmissionLockAction}
import pages.RcaspIdPage
import pages.organisation.{OverwritableOrganisationName, ReportForRegisteredBusinessPage}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.RcaspSubmissionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.CheckDetailsRegisteredBusinessHelper
import views.html.organisation.CheckDetailsRegBusinessView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RegisteredBusinessCheckDetailsController @Inject() (
    override val messagesApi: MessagesApi,
    sessionRepository: SessionRepository,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    submissionLock: SubmissionLockAction,
    view: CheckDetailsRegBusinessView,
    val controllerComponents: MessagesControllerComponents,
    helper: CheckDetailsRegisteredBusinessHelper,
    rcaspSubmissionService: RcaspSubmissionService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad: Action[AnyContent] = (identify() andThen getData() andThen submissionLock andThen requireData) {
    implicit request =>
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

  def onSubmit: Action[AnyContent] = (identify() andThen getData() andThen submissionLock andThen requireData).async {
    implicit request =>
      rcaspSubmissionService.submitRcasp(request.carfId, request.userAnswers).value.flatMap {
        case Right(response) =>
          val rcaspId = response.ResponseDetails.ReturnParameters.Value
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(RcaspIdPage, rcaspId))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(controllers.routes.RcaspAddedConfirmationController.onPageLoad())
        case Left(error)     =>
          logger.warn(s"[RegisteredBusinessCheckDetailsController][onSubmit] Unable to add RCASP: $error")
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }
  }
}
