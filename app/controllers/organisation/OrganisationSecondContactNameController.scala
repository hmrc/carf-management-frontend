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
import forms.organisation.OrganisationSecondContactNameFormProvider
import models.Mode
import navigation.Navigator
import pages.organisation.{OrganisationSecondContactNamePage, OverwritableOrganisationName}
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.organisation.OrganisationSecondContactNameView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class OrganisationSecondContactNameController @Inject() (
    override val messagesApi: MessagesApi,
    sessionRepository: SessionRepository,
    navigator: Navigator,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    formProvider: OrganisationSecondContactNameFormProvider,
    val controllerComponents: MessagesControllerComponents,
    view: OrganisationSecondContactNameView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  val form: Form[String] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify() andThen getData() andThen requireData) {
    implicit request =>

      val preparedForm = request.userAnswers.get(OrganisationSecondContactNamePage).fold(form)(form.fill)

      request.userAnswers.get(OverwritableOrganisationName) match {
        case Some(organisationName) => Ok(view(preparedForm, mode, organisationName))
        case None                   =>
          logger.warn(
            "[OrganisationSecondContactNameController] Could not retrieve OverwritableOrganisationName onPageLoad"
          )
          Redirect(
            controllers.routes.PlaceholderController.onPageLoad(
              "Should redirect to Some Information is Missing Page (CARF-293)"
            )
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
                  "[OrganisationSecondContactNameController] Could not retrieve OverwritableOrganisationName onPageSubmit"
                )
                Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
            },
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(OrganisationSecondContactNamePage, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(OrganisationSecondContactNamePage, mode, updatedAnswers))
        )
  }
}
