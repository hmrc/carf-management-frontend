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
import pages.remove.{RemoveOtherAccessPage, RemoveRcaspCachedDetails, RemoveUserBusinessInfoCached}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.remove.RemoveOtherAccessViewModel
import views.html.remove.RemoveOtherAccessView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RemoveOtherAccessController @Inject() (
    override val messagesApi: MessagesApi,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    submissionLock: SubmissionLockAction,
    sessionRepository: SessionRepository,
    formProvider: GenericYesNoPageFormProvider,
    val controllerComponents: MessagesControllerComponents,
    view: RemoveOtherAccessView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  private val journeyRecovery: Result = Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())

  def onPageLoad(rcaspId: String): Action[AnyContent] =
    (identify() andThen getData() andThen submissionLock andThen requireData).async { implicit request =>
      val cachedDetails =
        request.userAnswers
          .get(RemoveRcaspCachedDetails)
          .filter(_.RCASPID.toUpperCase == rcaspId.toUpperCase)

      val cachedUserInfo = request.userAnswers.get(RemoveUserBusinessInfoCached)

      (cachedDetails, cachedUserInfo) match {
        case (Some(details), Some(userInfo)) =>
          val vm           = RemoveOtherAccessViewModel.from(details, userInfo.hasOrganisationContactDetails, formProvider)
          val preparedForm = request.userAnswers.get(RemoveOtherAccessPage).fold(vm.form)(vm.form.fill)

          Future.successful(
            Ok(view(preparedForm, rcaspId, vm.titleKey, vm.headingKey, vm.rcaspName))
          )

        case _ =>
          logger.warn(
            "[RemoveOtherAccessController][onPageLoad] RemoveRcaspCachedDetails or user business info not found or rcaspId mismatch"
          )
          Future.successful(journeyRecovery)
      }
    }

  def onSubmit(rcaspId: String): Action[AnyContent] =
    (identify() andThen getData() andThen submissionLock andThen requireData).async { implicit request =>
      val cachedDetails =
        request.userAnswers
          .get(RemoveRcaspCachedDetails)
          .filter(_.RCASPID.toUpperCase == rcaspId.toUpperCase)

      val cachedUserInfo = request.userAnswers.get(RemoveUserBusinessInfoCached)

      (cachedDetails, cachedUserInfo) match {
        case (Some(details), Some(userInfo)) =>
          val vm = RemoveOtherAccessViewModel.from(details, userInfo.hasOrganisationContactDetails, formProvider)

          vm.form
            .bindFromRequest()
            .fold(
              formWithErrors =>
                Future.successful(BadRequest(view(formWithErrors, rcaspId, vm.titleKey, vm.headingKey, vm.rcaspName))),
              value =>
                for {
                  updatedAnswers <- Future.fromTry(request.userAnswers.set(RemoveOtherAccessPage, value))
                  _              <- sessionRepository.set(updatedAnswers)
                } yield Redirect(controllers.remove.routes.RemoveRcaspController.onPageLoad(rcaspId))
            )

        case _ =>
          logger.warn(
            "[RemoveOtherAccessController][onSubmit] RemoveRcaspCachedDetails or user business info not found or rcaspId mismatch"
          )
          Future.successful(journeyRecovery)
      }
    }
}
