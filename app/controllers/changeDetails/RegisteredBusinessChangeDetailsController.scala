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
import controllers.actions.*
import pages.SubmissionSucceededPage
import pages.changeDetails.ChangeRcaspCachedDetails
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.RcaspSubmissionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.{CheckDetailsHelper, CheckDetailsRegisteredBusinessHelper}
import views.html.changeDetails.RegisteredBusinessChangeDetailsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RegisteredBusinessChangeDetailsController @Inject() (
    override val messagesApi: MessagesApi,
    sessionRepository: SessionRepository,
    identify: IdentifierAction,
    ctUtrRetrievalAction: CtUtrRetrievalAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    submissionLock: SubmissionLockAction,
    view: RegisteredBusinessChangeDetailsView,
    val controllerComponents: MessagesControllerComponents,
    checkDetailsHelper: CheckDetailsHelper,
    checkDetailsRegisteredBusinessHelper: CheckDetailsRegisteredBusinessHelper,
    rcaspSubmissionService: RcaspSubmissionService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(rcaspId: String): Action[AnyContent] =
    (identify() andThen getData() andThen submissionLock andThen requireData) { implicit request =>
      val userAnswers          = request.userAnswers
      lazy val ifEmptyProtocol = Redirect(controllers.routes.InformationMissingController.onPageLoad())

      userAnswers.get(ChangeRcaspCachedDetails) match {
        case Some(cachedDetails) if cachedDetails.RCASPID.toUpperCase == rcaspId.toUpperCase =>
          (
            checkDetailsHelper.haveAnswersChangedFromApi(userAnswers),
            checkDetailsRegisteredBusinessHelper.getRegisteredBusinessSection(userAnswers, changeJourney = true)
          ).mapN { (hasDataChanged, section) =>
            Ok(view(section, rcaspId, hasDataChanged))
          }.getOrElse {
            logger.warn(
              "[RegisteredBusinessChangeDetailsController][onPageLoad] Error! Could not load page due to missing answers"
            )
            ifEmptyProtocol
          }
        case _                                                                               =>
          logger.warn(
            "[RegisteredBusinessChangeDetailsController][onPageLoad] Error! Missing ChangeRcaspCachedDetails."
          )
          ifEmptyProtocol
      }
    }

  def onSubmit(rcaspId: String): Action[AnyContent] =
    (identify() andThen ctUtrRetrievalAction() andThen getData() andThen submissionLock andThen requireData).async {
      implicit request =>
        request.utr match {
          case Some(utr) =>
            rcaspSubmissionService
              .updateRegisteredBusinessRcasp(request.carfId, utr, request.userAnswers)
              .value
              .flatMap {
                case Right(_)    =>
                  for {
                    uaWithSuccessFlag <- Future.fromTry(request.userAnswers.set(SubmissionSucceededPage, true))
                    _                 <- sessionRepository.set(uaWithSuccessFlag)
                  } yield Redirect(
                    controllers.routes.PlaceholderController
                      .onPageLoad(s"Successful submission for $rcaspId. Should redirect to /details-updated (CARF-353)")
                  )
                case Left(error) =>
                  logger.warn(
                    s"[RegisteredBusinessChangeDetailsController][onSubmit] Unable to update RCASP $rcaspId: $error"
                  )
                  Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
              }
          case None      =>
            logger.warn("[RegisteredBusinessChangeDetailsController][onSubmit] CT UTR not found in request")
            Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
        }
    }
}
