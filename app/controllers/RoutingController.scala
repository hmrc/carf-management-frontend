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

import controllers.actions.{CtUtrRetrievalAction, DataRetrievalAction, IdentifierAction}
import models.Mode
import play.api.Logging
import models.UserAnswers
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.AccountService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RoutingController @Inject() (
    identify: IdentifierAction,
    ctUtrRetrievalAction: CtUtrRetrievalAction,
    accountService: AccountService,
    sessionRepository: SessionRepository,
    getData: DataRetrievalAction,
    val controllerComponents: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with Logging {

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify() andThen ctUtrRetrievalAction() andThen getData()).async { implicit request =>
      val carfId = request.carfId

      accountService.getNumberOfRcaspsCurrentlyAdded(carfId).value.flatMap {
        case Left(error) =>
          logger.warn(s"[RoutingController] Failed to get RCASP count: $error")
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))

        case Right(numberOfRcasps) =>
          val updatedAnswers =
            request.userAnswers.getOrElse(UserAnswers(id = request.userId))

          val redirectUrl =
            if (numberOfRcasps > 0) {
              controllers.combined.routes.OrganisationOrIndividualController.onPageLoad(mode)
            } else {
              request.utr match {
                case Some(_) =>
                  controllers.combined.routes.ReportForRegisteredBusinessController.onPageLoad(mode)
                case None    =>
                  controllers.combined.routes.OrganisationOrIndividualController.onPageLoad(mode)
              }
            }

          for {
            _ <- sessionRepository.set(updatedAnswers)
          } yield Redirect(redirectUrl)
      }
    }
}

//jounrey map says
//do any rcasps added check first - if yes send to /org-or-ind
// if no - then do the was user a ct-automatched org? if yes send to /report-for-registered-busienss , if no then send to /org-or-ind
