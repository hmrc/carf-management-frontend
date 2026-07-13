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

import cats.syntax.all.*
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction, SubmissionLockAction}
import models.OrganisationOrIndividual.*
import pages.SubmissionSucceededPage
import pages.changeDetails.ChangeRcaspCachedDetails
import pages.combined.OrganisationOrIndividualPage
import pages.individual.IndividualNamePage
import pages.organisation.OverwritableOrganisationName
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.RcaspSubmissionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.CheckDetailsHelper
import views.html.changeDetails.ChangeDetailsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ChangeDetailsController @Inject() (
    override val messagesApi: MessagesApi,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    submissionLock: SubmissionLockAction,
    sessionRepository: SessionRepository,
    view: ChangeDetailsView,
    val controllerComponents: MessagesControllerComponents,
    helper: CheckDetailsHelper,
    rcaspSubmissionService: RcaspSubmissionService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(rcaspId: String): Action[AnyContent] =
    (identify() andThen getData() andThen submissionLock andThen requireData) { implicit request =>
      val userAnswers          = request.userAnswers
      lazy val ifEmptyProtocol =
        Redirect(controllers.routes.InformationMissingController.onPageLoad())

      (userAnswers.get(ChangeRcaspCachedDetails), userAnswers.get(OrganisationOrIndividualPage)) match {
        case (Some(cachedDetails), Some(orgOrIndividual)) if cachedDetails.RCASPID.toUpperCase == rcaspId.toUpperCase =>
          orgOrIndividual match {
            case Individual   =>
              (
                userAnswers.get(IndividualNamePage),
                helper.haveAnswersChangedFromApi(userAnswers),
                helper.getIndividualSectionMaybe(userAnswers, changeJourney = true),
                helper.getIndividualContactDetailsMaybe(userAnswers)
              )
                .mapN { (name, hasDataChanged, individualSection, contactDetailsSection) =>
                  Ok(view(Seq(individualSection, contactDetailsSection), name.fullName, rcaspId, hasDataChanged))
                }
                .getOrElse {
                  logger.warn(
                    "[ChangeDetailsController][onPageLoad] Error! Could not load page due to missing answers (individual)"
                  )
                  ifEmptyProtocol
                }
            case Organisation =>
              (
                userAnswers.get(OverwritableOrganisationName),
                helper.haveAnswersChangedFromApi(userAnswers),
                helper.getOrganisationSectionMaybe(userAnswers, changeJourney = true),
                helper.getOrganisationFirstContactDetailsMaybe(userAnswers),
                helper.getOrganisationSecondContactDetailsMaybe(userAnswers)
              )
                .mapN {
                  (
                      orgName,
                      hasDataChanged,
                      organisationSection,
                      firstContactDetailsSection,
                      secondContactDetailsSection
                  ) =>
                    Ok(
                      view(
                        Seq(organisationSection, firstContactDetailsSection, secondContactDetailsSection),
                        orgName,
                        rcaspId,
                        hasDataChanged
                      )
                    )
                }
                .getOrElse {
                  logger.warn(
                    "[ChangeDetailsController][onPageLoad] Error! Could not load page due to missing answers (organisation)"
                  )
                  ifEmptyProtocol
                }
          }
        case _                                                                                                        =>
          logger.warn(
            "[ChangeDetailsController][onPageLoad] Error! Missing ChangeRcaspCachedDetails or OrganisationOrIndividual"
          )
          ifEmptyProtocol
      }
    }

  def onSubmit(rcaspId: String): Action[AnyContent] =
    (identify() andThen getData() andThen submissionLock andThen requireData).async { implicit request =>
      rcaspSubmissionService.updateRcasp(request.carfId, request.userAnswers).value.flatMap {
        case Right(_)    =>
          for {
            uaWithSuccessFlag <- Future.fromTry(request.userAnswers.set(SubmissionSucceededPage, true))
            _                 <- sessionRepository.set(uaWithSuccessFlag)
          } yield Redirect(
            controllers.routes.PlaceholderController
              .onPageLoad(s"Successful submission for $rcaspId. Should redirect to /details-updated (CARF-353)")
          )
        case Left(error) =>
          logger.warn(s"[ChangeDetailsController][onSubmit] Unable to update RCASP $rcaspId: $error")
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }
    }

}
