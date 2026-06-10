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
import models.Mode
import navigation.Navigator
import pages.organisation.{OrganisationSecondContactHavePhonePage, OrganisationSecondContactNamePage, OverwritableOrganisationName}
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.organisation.OrganisationSecondContactHavePhoneView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class OrganisationSecondContactHavePhoneController @Inject() (
    override val messagesApi: MessagesApi,
    sessionRepository: SessionRepository,
    navigator: Navigator,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    formProvider: GenericYesNoPageFormProvider,
    val controllerComponents: MessagesControllerComponents,
    view: OrganisationSecondContactHavePhoneView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  val form: Form[Boolean] = formProvider("organisationSecondContactHavePhone.error.required")

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify() andThen getData() andThen requireData) {
    implicit request =>

      val preparedForm = request.userAnswers.get(OrganisationSecondContactHavePhonePage).fold(form)(form.fill)

      (
        request.userAnswers.get(OrganisationSecondContactNamePage),
        request.userAnswers.get(OverwritableOrganisationName)
      ) match {
        case (Some(secondContactName), Some(rcaspName)) =>
          Ok(view(preparedForm, mode, secondContactName, rcaspName))
        case _                                          =>
          logger.warn(
            "[OrganisationSecondContactHavePhoneController] Could not retrieve OrganisationSecondContactNamePage and/or OverwritableOrganisationName onPageLoad"
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
            (
              request.userAnswers.get(OrganisationSecondContactNamePage),
              request.userAnswers.get(OverwritableOrganisationName)
            ) match {
              case (Some(secondContactName), Some(organisationName)) =>
                Future.successful(BadRequest(view(formWithErrors, mode, secondContactName, organisationName)))
              case _                                                 =>
                logger.warn(
                  "[OrganisationSecondContactHavePhoneController] Could not retrieve OrganisationSecondContactNamePage and/or OverwritableOrganisationName onPageSubmit"
                )
                Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
            },
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(OrganisationSecondContactHavePhonePage, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(OrganisationSecondContactHavePhonePage, mode, updatedAnswers))
        )
  }
}
