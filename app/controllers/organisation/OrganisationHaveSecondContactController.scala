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
import models.{ChangeMode, Mode, NormalMode, UserAnswers}
import navigation.Navigator
import pages.changeDetails.ChangeRcaspCachedDetails
import pages.organisation.{OrganisationFirstContactNamePage, OrganisationHaveSecondContactPage}
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.organisation.OrganisationHaveSecondContactView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class OrganisationHaveSecondContactController @Inject() (
    override val messagesApi: MessagesApi,
    sessionRepository: SessionRepository,
    navigator: Navigator,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    submissionLock: SubmissionLockAction,
    formProvider: GenericYesNoPageFormProvider,
    val controllerComponents: MessagesControllerComponents,
    view: OrganisationHaveSecondContactView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  val form: Form[Boolean]         = formProvider("organisationHaveSecondContact.error.required")
  private lazy val recovery: Call = controllers.routes.JourneyRecoveryController.onPageLoad()

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify() andThen getData() andThen submissionLock andThen requireData) { implicit request =>

      val preparedForm = request.userAnswers.get(OrganisationHaveSecondContactPage).fold(form)(form.fill)

      request.userAnswers.get(OrganisationFirstContactNamePage) match {
        case Some(contactName) => Ok(view(preparedForm, mode, contactName))
        case None              =>
          logger.warn(
            "[OrganisationHaveSecondContactController] Could not retrieve OrganisationFirstContactNamePage onPageLoad"
          )
          Redirect(
            controllers.routes.InformationMissingController.onPageLoad()
          )
      }
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify() andThen getData() andThen submissionLock andThen requireData).async { implicit request =>
      val userAnswers                              = request.userAnswers
      lazy val hasValueChanged: Boolean => Boolean =
        newValue => !userAnswers.get(OrganisationHaveSecondContactPage).contains(newValue)

      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            request.userAnswers.get(OrganisationFirstContactNamePage) match {
              case Some(contactName) => Future.successful(BadRequest(view(formWithErrors, mode, contactName)))
              case None              =>
                logger.warn(
                  "[OrganisationHaveSecondContactController] Could not retrieve OrganisationFirstContactNamePage onPageSubmit"
                )
                Future.successful(
                  Redirect(
                    controllers.routes.InformationMissingController.onPageLoad()
                  )
                )
            },
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(OrganisationHaveSecondContactPage, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield mode match {
              case NormalMode => Redirect(navigator.nextPage(OrganisationHaveSecondContactPage, mode, updatedAnswers))
              case ChangeMode =>
                Redirect {
                  if (hasValueChanged(value)) {
                    navigateFromOrganisationHaveSecondContactController(updatedAnswers)
                  } else {
                    changeDetailsNavigation(updatedAnswers)
                  }
                }
            }
        )
    }

  private def navigateFromOrganisationHaveSecondContactController(userAnswers: UserAnswers): Call =
    userAnswers.get(OrganisationHaveSecondContactPage) match {
      case Some(true)  =>
        controllers.organisation.routes.OrganisationSecondContactNameController.onPageLoad(NormalMode)
      case Some(false) =>
        changeDetailsNavigation(userAnswers)
      case None        => recovery
    }

  private def changeDetailsNavigation(userAnswers: UserAnswers): Call = {
    val maybeRcaspId = userAnswers.get(ChangeRcaspCachedDetails).map(_.RCASPID)

    maybeRcaspId.fold(recovery) { rcaspId =>
      controllers.changeDetails.routes.ChangeDetailsRoutingController.onPageLoad(rcaspId)
    }
  }
}
