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
import pages.remove.{RcaspRemovedDateTimePage, RemoveOtherAccessPage, RemoveRcaspCachedDetails, RemoveRcaspPage, RemoveUserAccessPage}
import utils.LoggerUtil.*
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.RcaspSubmissionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.remove.RemoveRcaspView

import java.time.{Clock, Instant}
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
    rcaspSubmissionService: RcaspSubmissionService,
    val controllerComponents: MessagesControllerComponents,
    view: RemoveRcaspView,
    clock: Clock
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form: Form[Boolean] = formProvider("removeRcasp.error.required")

  def onPageLoad(): Action[AnyContent] =
    (identify andThen getData() andThen submissionLock andThen requireData) { implicit request =>
      lazy val preparedForm = request.userAnswers.get(RemoveRcaspPage).fold(form)(form.fill)

      val maybeCachedDetails     = request.userAnswers.get(RemoveRcaspCachedDetails)
      val maybeOtherAccessAnswer = request.userAnswers.get(RemoveOtherAccessPage)

      (maybeCachedDetails, maybeOtherAccessAnswer) match {
        case (Some(cachedDetails), Some(otherAccessAnswer)) =>
          Ok(view(preparedForm, otherAccessAnswer, cachedDetails.getName))

        case _ =>
          logWarn(
            "[RemoveRcaspController][onPageLoad] RemoveRcaspCachedDetails or RemoveOtherAccessPage not found"
          )
          Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      }
    }

  def onSubmit(): Action[AnyContent] =
    (identify andThen getData() andThen submissionLock andThen requireData).async { implicit request =>
      val maybeCachedDetails     = request.userAnswers.get(RemoveRcaspCachedDetails)
      val maybeUserAccessAnswer  = request.userAnswers.get(RemoveUserAccessPage)
      val maybeOtherAccessAnswer = request.userAnswers.get(RemoveOtherAccessPage)

      (maybeCachedDetails, maybeUserAccessAnswer, maybeOtherAccessAnswer) match {
        case (Some(cachedDetails), Some(_), Some(otherAccessAnswer)) =>
          form
            .bindFromRequest()
            .fold(
              formWithErrors =>
                Future.successful(BadRequest(view(formWithErrors, otherAccessAnswer, cachedDetails.getName))),
              value =>
                if (!value) {
                  for {
                    updatedAnswers <- Future.fromTry(request.userAnswers.set(RemoveRcaspPage, value))
                    _              <- sessionRepository.set(updatedAnswers)
                  } yield Redirect(controllers.routes.YourRcaspsController.onPageLoad())
                } else {
                  rcaspSubmissionService.removeRcasp(request.carfId, cachedDetails.RCASPID).value.flatMap {
                    case Right(_) =>
                      val currentTime = Instant.now(clock)
                      for {
                        updatedAnswers1 <- Future.fromTry(request.userAnswers.set(RemoveRcaspPage, value))
                        updatedAnswers2 <- Future.fromTry(updatedAnswers1.set(SubmissionSucceededPage, true))
                        updatedAnswers3 <- Future.fromTry(updatedAnswers2.set(RcaspRemovedDateTimePage, currentTime))
                        _               <- sessionRepository.set(updatedAnswers3)
                      } yield Redirect(controllers.remove.routes.RcaspRemovedController.onPageLoad())

                    case Left(error) =>
                      logWarn(s"[RemoveRcaspController][onSubmit] Failed to remove RCASP: $error")
                      Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
                  }
                }
            )

        case _ =>
          logWarn(
            "[RemoveRcaspController][onSubmit] RemoveRcaspCachedDetails, RemoveUserAccessPage or RemoveOtherAccessPage not found"
          )
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }
    }
}
