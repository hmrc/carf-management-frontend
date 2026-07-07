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

import config.Constants.ZERO
import controllers.actions.{CtUtrRetrievalAction, DataRetrievalAction, IdentifierAction}
import models.{ChangeMode, Mode, NormalMode, UserAnswers}
import pages.SubmissionSucceededPage
import pages.changeDetails.ChangeRcaspCachedDetails
import play.api.Logging
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
          val updatedAnswers = mode match {
            case NormalMode =>
              request.userAnswers
                .flatMap(ua =>
                  if (ua.get(SubmissionSucceededPage).contains(true) || ua.get(ChangeRcaspCachedDetails).isDefined) None
                  else request.userAnswers
                )
                .getOrElse(UserAnswers(id = request.userId, rcaspIsRegisteredBusiness = false))
            case ChangeMode =>
              request.userAnswers.getOrElse(UserAnswers(id = request.userId, rcaspIsRegisteredBusiness = false))
          }

          val redirectUrl =
            if (numberOfRcasps == ZERO && request.utr.isDefined) {
              controllers.organisation.routes.ReportForRegisteredBusinessController.onPageLoad(mode)
            } else {
              controllers.combined.routes.OrganisationOrIndividualController.onPageLoad(mode)
            }

          for {
            _ <- sessionRepository.set(updatedAnswers)
          } yield Redirect(redirectUrl)
      }
    }
}
