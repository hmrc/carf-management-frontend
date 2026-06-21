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
import forms.organisation.GenericOrganisationContactNameFormProvider

import javax.inject.Inject
import models.Mode
import navigation.Navigator
import pages.organisation.{OrganisationFirstContactNamePage, OverwritableOrganisationName}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.organisation.OrganisationFirstContactNameView
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

class OrganisationFirstContactNameController @Inject() (
    override val messagesApi: MessagesApi,
    sessionRepository: SessionRepository,
    navigator: Navigator,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    formProvider: GenericOrganisationContactNameFormProvider,
    val controllerComponents: MessagesControllerComponents,
    view: OrganisationFirstContactNameView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  val form: Form[String] = formProvider("organisationFirstContactName")

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify() andThen getData() andThen requireData) {
    implicit request =>

      val preparedForm = request.userAnswers.get(OrganisationFirstContactNamePage).fold(form)(form.fill)

      request.userAnswers.get(OverwritableOrganisationName) match {
        case Some(organisationName) => Ok(view(preparedForm, mode, organisationName))
        case None                   =>
          logger.warn(
            "[OrganisationFirstContactNameController] Could not retrieve OverwritableOrganisationName onPageLoad"
          )
          Redirect(
            controllers.routes.InformationMissingController.onPageLoad()
          )
      }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify() andThen getData() andThen requireData).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            request.userAnswers.get(OverwritableOrganisationName) match {
              case Some(organisationName) => Future.successful(BadRequest(view(formWithErrors, mode, organisationName)))
              case None                   =>
                logger.warn(
                  "[OrganisationFirstContactNameController] Could not retrieve OverwritableOrganisationName onPageSubmit"
                )
                Future.successful(
                  Redirect(
                    controllers.routes.InformationMissingController.onPageLoad()
                  )
                )
            },
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(OrganisationFirstContactNamePage, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(OrganisationFirstContactNamePage, mode, updatedAnswers))
        )
  }
}
