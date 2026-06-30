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

package controllers.individual

import controllers.actions.*
import forms.GenericEmailFormProvider

import javax.inject.Inject
import models.Mode
import navigation.Navigator
import pages.individual.{IndividualEmailPage, IndividualNamePage}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.individual.IndividualEmailView
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

class IndividualEmailController @Inject() (
    override val messagesApi: MessagesApi,
    sessionRepository: SessionRepository,
    navigator: Navigator,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    submissionLock: SubmissionLockAction,
    formProvider: GenericEmailFormProvider,
    val controllerComponents: MessagesControllerComponents,
    view: IndividualEmailView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  val form: Form[String] = formProvider(messageKey = "individualEmail")

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify() andThen getData() andThen submissionLock andThen requireData) { implicit request =>

      val preparedForm = request.userAnswers.get(IndividualEmailPage).fold(form)(form.fill)

      request.userAnswers
        .get(IndividualNamePage)
        .fold {
          logger.warn(
            "[IndividualEmailController][onPageLoad] Error! Individual name could not be retrieved from user answers"
          )
          Redirect(
            controllers.routes.InformationMissingController.onPageLoad()
          )
        }(individualName => Ok(view(preparedForm, mode, individualName.fullName)))
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify() andThen getData() andThen submissionLock andThen requireData).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            request.userAnswers
              .get(IndividualNamePage)
              .fold {
                logger.warn(
                  "[IndividualEmailController][onSubmit] Error! Individual name could not be retrieved from user answers"
                )
                Future.successful(
                  Redirect(
                    controllers.routes.InformationMissingController.onPageLoad()
                  )
                )
              }(individualName => Future.successful(BadRequest(view(formWithErrors, mode, individualName.fullName)))),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(IndividualEmailPage, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(IndividualEmailPage, mode, updatedAnswers))
        )
    }
}
