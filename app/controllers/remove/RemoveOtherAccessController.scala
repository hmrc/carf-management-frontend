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
import pages.remove.{RemoveOtherAccessPage, RemoveRcaspCachedDetails}
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
    sessionRepository: SessionRepository,
    formProvider: GenericYesNoPageFormProvider,
    val controllerComponents: MessagesControllerComponents,
    view: RemoveOtherAccessView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  private val journeyRecovery: Result =
    Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())

  def onPageLoad(): Action[AnyContent] =
    (identify() andThen getData() andThen requireData).async { implicit request =>
      request.userAnswers.get(RemoveRcaspCachedDetails) match {
        case Some(details) =>
          val vm           = RemoveOtherAccessViewModel.from(details, formProvider)
          val preparedForm = request.userAnswers.get(RemoveOtherAccessPage).fold(vm.form)(vm.form.fill)

          Future.successful(
            Ok(
              view(
                preparedForm,
                vm.titleKey,
                vm.headingKey,
                vm.errorKey,
                vm.rcaspName
              )
            )
          )

        case None =>
          logger.warn("[RemoveOtherAccessController][onPageLoad] RemoveRcaspCachedDetails not found in UserAnswers")
          Future.successful(journeyRecovery)
      }
    }

  def onSubmit(): Action[AnyContent] =
    (identify() andThen getData() andThen requireData).async { implicit request =>
      request.userAnswers.get(RemoveRcaspCachedDetails) match {
        case Some(details) =>
          val vm = RemoveOtherAccessViewModel.from(details, formProvider)

          vm.form
            .bindFromRequest()
            .fold(
              formWithErrors =>
                Future.successful(
                  BadRequest(
                    view(
                      formWithErrors,
                      vm.titleKey,
                      vm.headingKey,
                      vm.errorKey,
                      vm.rcaspName
                    )
                  )
                ),
              value =>
                for {
                  updatedAnswers <- Future.fromTry(request.userAnswers.set(RemoveOtherAccessPage, value))
                  _              <- sessionRepository.set(updatedAnswers)
                } yield Redirect(
                  controllers.routes.PlaceholderController.onPageLoad("Should nav to /remove/remove-rcasp (CARF-549)")
                )
            )

        case None =>
          logger.warn("[RemoveOtherAccessController][onSubmit] RemoveRcaspCachedDetails not found in UserAnswers")
          Future.successful(journeyRecovery)
      }
    }
}
