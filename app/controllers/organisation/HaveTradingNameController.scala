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

package controllers.organisation

import controllers.actions.*
import forms.GenericYesNoPageFormProvider

import javax.inject.Inject
import models.Mode
import navigation.Navigator
import pages.organisation.{HaveTradingNamePage, OrganisationNameInUserAnswers}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.organisation.HaveTradingNameView
import play.api.data.Form

import java.lang.ProcessBuilder
import scala.concurrent.{ExecutionContext, Future}

class HaveTradingNameController @Inject() (
    override val messagesApi: MessagesApi,
    sessionRepository: SessionRepository,
    navigator: Navigator,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    formProvider: GenericYesNoPageFormProvider,
    val controllerComponents: MessagesControllerComponents,
    view: HaveTradingNameView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  val form: Form[Boolean] = formProvider("haveTradingName.error.required")

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify() andThen getData() andThen requireData) {
    implicit request =>

      val preparedForm = request.userAnswers.get(HaveTradingNamePage).fold(form)(form.fill)

      request.userAnswers
        .get(OrganisationNameInUserAnswers)
        .fold {
          logger.warn(
            "[HaveTradingNameController][onPageLoad] Error! Organisation name could not be retrieved from user answers"
          )
          Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
        }(orgName => Ok(view(preparedForm, mode, orgName)))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify() andThen getData() andThen requireData).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            request.userAnswers
              .get(OrganisationNameInUserAnswers)
              .fold {
                logger.warn(
                  "[HaveTradingNameController][onSubmit] Error! Organisation name could not be retrieved from user answers"
                )
                Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
              }(orgName => Future.successful(BadRequest(view(formWithErrors, mode, orgName)))),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(HaveTradingNamePage, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(HaveTradingNamePage, mode, updatedAnswers))
        )
  }
}
