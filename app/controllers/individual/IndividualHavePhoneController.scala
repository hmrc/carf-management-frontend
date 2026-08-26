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
import forms.GenericYesNoPageFormProvider
import models.{ChangeMode, Mode, NormalMode}
import navigation.Navigator
import pages.individual.{IndividualHavePhonePage, IndividualNamePage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.LoggerUtil.*
import views.html.individual.IndividualHavePhoneView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IndividualHavePhoneController @Inject() (
    override val messagesApi: MessagesApi,
    sessionRepository: SessionRepository,
    navigator: Navigator,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    submissionLock: SubmissionLockAction,
    formProvider: GenericYesNoPageFormProvider,
    val controllerComponents: MessagesControllerComponents,
    view: IndividualHavePhoneView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form: Form[Boolean] = formProvider("individualHavePhone.error.required")

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify andThen getData() andThen submissionLock andThen requireData) { implicit request =>

      lazy val preparedForm = request.userAnswers.get(IndividualHavePhonePage).fold(form)(form.fill)

      request.userAnswers
        .get(IndividualNamePage)
        .fold {
          logWarn(
            "[IndividualHavePhoneController][onPageLoad] Error! Individual name could not be retrieved from user answers"
          )
          Redirect(controllers.routes.InformationMissingController.onPageLoad())
        }(individualName => Ok(view(preparedForm, mode, individualName.fullName)))
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify andThen getData() andThen submissionLock andThen requireData).async { implicit request =>

      val userAnswers = request.userAnswers

      lazy val hasValueChanged: Boolean => Boolean =
        newValue => !userAnswers.get(IndividualHavePhonePage).contains(newValue)

      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            request.userAnswers
              .get(IndividualNamePage)
              .fold {
                logWarn(
                  "[IndividualHavePhoneController][onSubmit] Error! Individual name could not be retrieved from user answers"
                )
                Future.successful(Redirect(controllers.routes.InformationMissingController.onPageLoad()))
              }(individualName => Future.successful(BadRequest(view(formWithErrors, mode, individualName.fullName)))),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(IndividualHavePhonePage, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield mode match {
              case NormalMode =>
                Redirect(navigator.nextPage(IndividualHavePhonePage, mode, updatedAnswers))
              case ChangeMode =>
                Redirect {
                  if (hasValueChanged(value)) {
                    navigateFromIndividualHavePhonePage(value)
                  } else {
                    controllers.routes.EndOfJourneyRoutingController.onPageLoad()
                  }
                }
            }
        )
    }

  private def navigateFromIndividualHavePhonePage(havePhone: Boolean): Call =
    if (havePhone) controllers.individual.routes.IndividualPhoneController.onPageLoad(ChangeMode)
    else controllers.routes.EndOfJourneyRoutingController.onPageLoad()
}
