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

package controllers.remove

import controllers.actions.*
import forms.GenericYesNoPageFormProvider
import pages.SubmissionSucceededPage
import pages.remove.{RemoveOtherAccessPage, RemoveRcaspCachedDetails, RemoveRcaspRemovedDateTimePage}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.AccountService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.remove.RemoveRcaspViewModel
import views.html.remove.RemoveRcaspView

import java.time.Instant
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RemoveRcaspController @Inject() (
    override val messagesApi: MessagesApi,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    submissionLock: SubmissionLockAction,
    sessionRepository: SessionRepository,
    formProvider: GenericYesNoPageFormProvider,
    accountService: AccountService,
    val controllerComponents: MessagesControllerComponents,
    view: RemoveRcaspView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(rcaspId: String): Action[AnyContent] =
    (identify() andThen getData() andThen submissionLock andThen requireData).async { implicit request =>
      val alreadySubmitted = request.userAnswers.get(SubmissionSucceededPage).contains(true)

      if (alreadySubmitted) {
        logger.info(
          "[RemoveRcaspController][onPageLoad] Submission already succeeded - redirecting to page-unavailable (CARF-536)"
        )
        Future.successful(
          Redirect(controllers.routes.PlaceholderController.onPageLoad("/problem/page-unavailable (CARF-536)"))
        )
      } else {
        val cachedDetails = request.userAnswers
          .get(RemoveRcaspCachedDetails)
          .filter(_.RCASPID.equalsIgnoreCase(rcaspId))

        val otherAccessAnswer = request.userAnswers.get(RemoveOtherAccessPage)

        (cachedDetails, otherAccessAnswer) match {
          case (Some(details), Some(answer)) =>
            val vm = RemoveRcaspViewModel.from(details, answer, formProvider)

            Future.successful(
              Ok(view(vm.form, rcaspId, vm.titleKey, vm.headingKey, vm.rcaspName))
            )

          case _ =>
            logger.warn(
              "[RemoveRcaspController][onPageLoad] RemoveRcaspCachedDetails or RemoveOtherAccessPage not found, or rcaspId mismatch"
            )
            Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
        }
      }
    }

  def onSubmit(rcaspId: String): Action[AnyContent] =
    (identify() andThen getData() andThen submissionLock andThen requireData).async { implicit request =>
      val cachedDetails = request.userAnswers
        .get(RemoveRcaspCachedDetails)
        .filter(_.RCASPID.equalsIgnoreCase(rcaspId))

      val otherAccessAnswer = request.userAnswers.get(RemoveOtherAccessPage)

      (cachedDetails, otherAccessAnswer) match {
        case (Some(details), Some(answer)) =>
          val vm = RemoveRcaspViewModel.from(details, answer, formProvider)

          vm.form
            .bindFromRequest()
            .fold(
              formWithErrors =>
                Future.successful(BadRequest(view(formWithErrors, rcaspId, vm.titleKey, vm.headingKey, vm.rcaspName))),
              {
                case false =>
                  Future.successful(Redirect(controllers.routes.YourRcaspsController.onPageLoad()))

                case true =>
                  accountService.removeRcasp(request.carfId, rcaspId).value.flatMap {
                    case Right(_) =>
                      val removedAt = Instant.now()
                      for {
                        updatedAnswers <- Future.fromTry(
                                            request.userAnswers
                                              .set(RemoveRcaspRemovedDateTimePage, removedAt.toString)
                                              .flatMap(_.set(SubmissionSucceededPage, true))
                                          )
                        _              <- sessionRepository.set(updatedAnswers)
                      } yield Redirect(controllers.remove.routes.RcaspRemovedController.onPageLoad(rcaspId))

                    case Left(error) =>
                      logger.warn(s"[RemoveRcaspController][onSubmit] Failed to remove RCASP: $error")
                      Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
                  }
              }
            )

        case _ =>
          logger.warn(
            "[RemoveRcaspController][onSubmit] RemoveRcaspCachedDetails or RemoveOtherAccessPage not found, or rcaspId mismatch"
          )
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }
    }
}
