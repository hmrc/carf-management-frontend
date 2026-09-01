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

import javax.inject.Inject
import models.Mode
import navigation.Navigator
import pages.organisation.{OrganisationFirstContactEmailPage, OrganisationFirstContactNamePage, OverwritableOrganisationName}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import forms.GenericEmailFormProvider
import utils.LoggerUtil.*
import play.api.data.Form
import views.html.organisation.OrganisationFirstContactEmailView

import scala.concurrent.{ExecutionContext, Future}

class OrganisationFirstContactEmailController @Inject() (
    override val messagesApi: MessagesApi,
    sessionRepository: SessionRepository,
    navigator: Navigator,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    submissionLock: SubmissionLockAction,
    formProvider: GenericEmailFormProvider,
    val controllerComponents: MessagesControllerComponents,
    view: OrganisationFirstContactEmailView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form: Form[String] = formProvider("organisationFirstContactEmail")

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify andThen getData() andThen submissionLock andThen requireData) { implicit request =>

      val preparedForm = request.userAnswers.get(OrganisationFirstContactEmailPage).fold(form)(form.fill)

      (
        request.userAnswers.get(OrganisationFirstContactNamePage),
        request.userAnswers.get(OverwritableOrganisationName)
      ) match {
        case (Some(firstContactName), Some(rcaspName)) =>
          Ok(view(preparedForm, mode, firstContactName, rcaspName))
        case _                                         =>
          logWarn(
            "[OrganisationFirstContactEmailController] Could not retrieve OrganisationFirstContactNamePage and/or OverwritableOrganisationName onPageLoad"
          )
          Redirect(
            controllers.routes.InformationMissingController.onPageLoad()
          )
      }
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify andThen getData() andThen submissionLock andThen requireData).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            (
              request.userAnswers.get(OrganisationFirstContactNamePage),
              request.userAnswers.get(OverwritableOrganisationName)
            ) match {
              case (Some(firstContactName), Some(organisationName)) =>
                Future.successful(BadRequest(view(formWithErrors, mode, firstContactName, organisationName)))
              case _                                                =>
                logWarn(
                  "[OrganisationFirstContactEmailController] Could not retrieve Contact and/or Org name onPageSubmit"
                )
                Future.successful(
                  Redirect(
                    controllers.routes.InformationMissingController.onPageLoad()
                  )
                )
            },
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(OrganisationFirstContactEmailPage, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(OrganisationFirstContactEmailPage, mode, updatedAnswers))
        )
    }
}
