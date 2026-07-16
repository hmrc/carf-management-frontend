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

import controllers.actions.{CtUtrRetrievalAction, DataRetrievalAction, IdentifierAction}
import models.viewAndUpdateRcasp.{IndividualRcaspDetails, OrganisationRcaspDetails}
import pages.SubmissionSucceededPage
import pages.changeDetails.ChangeRcaspCachedDetails
import pages.organisation.*
import play.api.Logging
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import services.AccountService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.changeDetails.PopulateUserAnswersHelper

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ChangeDetailsRoutingController @Inject() (
    identify: IdentifierAction,
    ctUtrRetrievalAction: CtUtrRetrievalAction,
    accountService: AccountService,
    getData: DataRetrievalAction,
    populateUserAnswersHelper: PopulateUserAnswersHelper,
    val controllerComponents: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with Logging {

  def onPageLoad(rcaspId: String): Action[AnyContent] =
    (identify() andThen ctUtrRetrievalAction() andThen getData()).async { implicit request =>
      val submissionFlagSet = request.userAnswers.flatMap(_.get(SubmissionSucceededPage)).contains(true)
      (
        request.userAnswers.flatMap(_.get(ChangeRcaspCachedDetails)),
        request.userAnswers.flatMap(_.get(ReportForRegisteredBusinessPage))
      ) match {
        case (Some(cachedDetails), Some(isRcaspUserAnswer))
            if cachedDetails.RCASPID.toUpperCase == rcaspId.toUpperCase && !submissionFlagSet =>
          val redirectCall: Call =
            if (isRcaspUserAnswer)
              controllers.changeDetails.routes.RegisteredBusinessChangeDetailsController.onPageLoad(rcaspId)
            else controllers.changeDetails.routes.ChangeDetailsController.onPageLoad(rcaspId)
          Future.successful(Redirect(redirectCall))
        case _ =>
          accountService.getRcaspDetails(request.carfId, rcaspId).value.flatMap {
            case Left(error)                                               =>
              logger.warn(s"[ChangeDetailsRoutingController][onPageLoad] Failed to get RCASP details: $error")
              Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
            case Right(individualRcaspDetails: IndividualRcaspDetails)     =>
              if (individualRcaspDetails.IsRCASPUser) {
                logger.warn(
                  s"[ChangeDetailsRoutingController][onPageLoad] RCASP $rcaspId is an individual but isRcaspUser = true"
                )
                Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
              } else {
                populateUserAnswersHelper.populateUserAnswersForIndividual(request.userId, individualRcaspDetails)
              }
            case Right(organisationRcaspDetails: OrganisationRcaspDetails) =>
              if (organisationRcaspDetails.IsRCASPUser) {
                request.utr.fold {
                  logger.warn(
                    "[ChangeDetailsRoutingController][onPageLoad] CT UTR not found in request for registered business"
                  )
                  Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
                } { utr =>
                  populateUserAnswersHelper
                    .populateUserAnswersForRegisteredBusiness(request.userId, utr, organisationRcaspDetails)
                }
              } else
                populateUserAnswersHelper.populateUserAnswersForOrganisation(request.userId, organisationRcaspDetails)
          }
      }
    }
}
