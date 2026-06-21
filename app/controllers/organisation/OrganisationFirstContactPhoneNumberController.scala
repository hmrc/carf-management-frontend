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
import forms.GenericPhoneFormProvider
import models.Mode
import navigation.Navigator
import pages.organisation.{OrganisationFirstContactNamePage, OrganisationFirstContactPhoneNumberPage}
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.organisation.OrganisationFirstContactPhoneNumberView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class OrganisationFirstContactPhoneNumberController @Inject() (
    override val messagesApi: MessagesApi,
    sessionRepository: SessionRepository,
    navigator: Navigator,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    formProvider: GenericPhoneFormProvider,
    val controllerComponents: MessagesControllerComponents,
    view: OrganisationFirstContactPhoneNumberView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  val form: Form[String] = formProvider("organisationFirstContactPhoneNumber")

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify() andThen getData() andThen requireData) {
    implicit request =>

      val preparedForm = request.userAnswers.get(OrganisationFirstContactPhoneNumberPage).fold(form)(form.fill)

      request.userAnswers.get(OrganisationFirstContactNamePage) match {
        case Some(contactName) => Ok(view(preparedForm, mode, contactName))
        case None              =>
          logger.warn(
            "[OrganisationFirstContactPhoneNumberController] Could not retrieve OrganisationFirstContactNamePage onPageLoad"
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
            request.userAnswers.get(OrganisationFirstContactNamePage) match {
              case Some(contactName) => Future.successful(BadRequest(view(formWithErrors, mode, contactName)))
              case None              =>
                logger.warn(
                  "[OrganisationFirstContactPhoneNumberController] Could not retrieve OrganisationFirstContactNamePage onPageSubmit"
                )
                Future.successful(
                  Redirect(
                    controllers.routes.InformationMissingController.onPageLoad()
                  )
                )
            },
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(OrganisationFirstContactPhoneNumberPage, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(OrganisationFirstContactPhoneNumberPage, mode, updatedAnswers))
        )
  }
}
