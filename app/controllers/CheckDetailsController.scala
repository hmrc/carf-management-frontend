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

import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction, SubmissionLockAction}
import models.OrganisationOrIndividual.*
import pages.RcaspIdPage
import pages.combined.OrganisationOrIndividualPage
import pages.individual.IndividualNamePage
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.SubmitRcaspService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.CheckDetailsHelper
import views.html.CheckDetailsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckDetailsController @Inject() (
    override val messagesApi: MessagesApi,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    submissionLock: SubmissionLockAction,
    submitRcaspService: SubmitRcaspService,
    sessionRepository: SessionRepository,
    view: CheckDetailsView,
    val controllerComponents: MessagesControllerComponents,
    helper: CheckDetailsHelper
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad: Action[AnyContent] =
    (identify() andThen getData() andThen submissionLock andThen requireData) { implicit request =>
      import cats.syntax.all.*
      val userAnswers          = request.userAnswers
      lazy val ifEmptyProtocol =
        Redirect(controllers.routes.InformationMissingController.onPageLoad())

      userAnswers
        .get(OrganisationOrIndividualPage)
        .fold {
          logger.warn(
            s"[CheckDetailsController] Error! Could not load page OrganisationOrIndividualPage needed"
          )
          ifEmptyProtocol
        } {
          case Individual   =>
            (
              userAnswers.get(IndividualNamePage),
              helper.getIndividualSectionMaybe(userAnswers),
              helper.getContactDetails(userAnswers)
            )
              .mapN { (name, individualSection, contactDetailsSection) =>
                Ok(view(Seq(individualSection, contactDetailsSection), name.fullName))
              }
              .getOrElse {
                logger.warn(
                  s"[CheckDetailsController] Error! Could not load page missing answers"
                )
                ifEmptyProtocol
              }
          case Organisation => Ok(view(Seq.empty, "Organisation Name")) // TODO [CARF-295] - Replace with real org name
        }
    }

  def onSubmit: Action[AnyContent] =
    (identify() andThen getData() andThen submissionLock andThen requireData).async { implicit request =>
      // TODO - Current impl is to allow RcaspAddedConfirmationController to retrieve stubbed rcaspId, this will change in CARF-294/CARF-295

      submitRcaspService.submitRcasp().value.flatMap {
        case Right(response) =>
          val rcaspId = response.ResponseDetails.ReturnParameters.Value
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(RcaspIdPage, rcaspId))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(controllers.routes.RcaspAddedConfirmationController.onPageLoad())
        case Left(_)         =>
          logger.warn("[CheckDetailsController][onSubmit] Submit RCASP call failed")
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }
    }
}
