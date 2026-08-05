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

import cats.syntax.all.*
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction, SubmissionLockAction}
import models.OrganisationOrIndividual.*
import pages.combined.OrganisationOrIndividualPage
import pages.individual.IndividualNamePage
import pages.organisation.OverwritableOrganisationName
import pages.{RcaspIdPage, SubmissionSucceededPage}
import utils.LoggerUtil.*
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.RcaspSubmissionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.DetailsHelper
import views.html.CheckDetailsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckDetailsController @Inject() (
    override val messagesApi: MessagesApi,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    submissionLock: SubmissionLockAction,
    sessionRepository: SessionRepository,
    view: CheckDetailsView,
    val controllerComponents: MessagesControllerComponents,
    helper: DetailsHelper,
    rcaspSubmissionService: RcaspSubmissionService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad: Action[AnyContent] = (identify() andThen getData() andThen submissionLock andThen requireData) {
    implicit request =>
      val userAnswers          = request.userAnswers
      lazy val ifEmptyProtocol =
        Redirect(controllers.routes.InformationMissingController.onPageLoad())

      userAnswers
        .get(OrganisationOrIndividualPage)
        .fold {
          logWarn("[CheckDetailsController][onPageLoad] Error! OrganisationOrIndividualPage not populated")
          ifEmptyProtocol
        } {
          case Individual   =>
            (
              userAnswers.get(IndividualNamePage),
              helper.getIndividualSectionMaybe(userAnswers, changeJourney = false),
              helper.getIndividualContactDetailsMaybe(userAnswers)
            )
              .mapN { (name, individualSection, contactDetailsSection) =>
                Ok(view(Seq(individualSection, contactDetailsSection), name.fullName))
              }
              .getOrElse {
                logWarn(
                  "[CheckDetailsController][onPageLoad] Error! Could not load page due to missing answers (individual)"
                )
                ifEmptyProtocol
              }
          case Organisation =>
            (
              userAnswers.get(OverwritableOrganisationName),
              helper.getOrganisationSectionMaybe(userAnswers, changeJourney = false),
              helper.getOrganisationFirstContactDetailsMaybe(userAnswers),
              helper.getOrganisationSecondContactDetailsMaybe(userAnswers)
            )
              .mapN { (orgName, organisationSection, firstContactDetailsSection, secondContactDetailsSection) =>
                Ok(view(Seq(organisationSection, firstContactDetailsSection, secondContactDetailsSection), orgName))
              }
              .getOrElse {
                logWarn(
                  "[CheckDetailsController][onPageLoad] Error! Could not load page due to missing answers (organisation)"
                )
                ifEmptyProtocol
              }
        }
  }

  def onSubmit: Action[AnyContent] = (identify() andThen getData() andThen submissionLock andThen requireData).async {
    implicit request =>
      rcaspSubmissionService.createRcasp(request.carfId, request.userAnswers).value.flatMap {
        case Right(response) =>
          val rcaspId = response.ResponseDetails.ReturnParameters.Value
          for {
            ua                <- Future.fromTry(request.userAnswers.set(RcaspIdPage, rcaspId))
            uaWithSuccessFlag <- Future.fromTry(ua.set(SubmissionSucceededPage, true))
            _                 <- sessionRepository.set(uaWithSuccessFlag)
          } yield Redirect(controllers.routes.RcaspAddedConfirmationController.onPageLoad())
        case Left(error)     =>
          logWarn(s"[CheckDetailsController][onSubmit] Unable to add RCASP: $error")
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }
  }

}
