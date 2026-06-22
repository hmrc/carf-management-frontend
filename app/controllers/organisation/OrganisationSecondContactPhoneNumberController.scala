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
import pages.organisation.{OrganisationSecondContactNamePage, OrganisationSecondContactPhoneNumberPage}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.organisation.OrganisationSecondContactPhoneNumberView
import play.api.data.Form
import forms.GenericPhoneFormProvider
import scala.concurrent.{ExecutionContext, Future}

class OrganisationSecondContactPhoneNumberController @Inject() (
    override val messagesApi: MessagesApi,
    sessionRepository: SessionRepository,
    navigator: Navigator,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    formProvider: GenericPhoneFormProvider,
    val controllerComponents: MessagesControllerComponents,
    view: OrganisationSecondContactPhoneNumberView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  val form: Form[String] = formProvider("organisationSecondContactPhoneNumber")

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify() andThen getData() andThen requireData) {
    implicit request =>

      val preparedForm = request.userAnswers.get(OrganisationSecondContactPhoneNumberPage).fold(form)(form.fill)

      request.userAnswers.get(OrganisationSecondContactNamePage) match {
        case Some(usersName) => Ok(view(preparedForm, mode, usersName))
        case None            =>
          logger.warn(
            "[OrganisationSecondContactPhoneNumberController] Could not retrieve OrganisationSecondContactNamePage onPageLoad"
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
            request.userAnswers.get(OrganisationSecondContactNamePage) match {
              case Some(usersName) => Future.successful(BadRequest(view(formWithErrors, mode, usersName)))
              case None            =>
                logger.warn(
                  "[OrganisationSecondContactPhoneNumberController] Could not retrieve OrganisationSecondContactNamePage onPageSubmit"
                )
                Future.successful(
                  Redirect(
                    controllers.routes.InformationMissingController.onPageLoad()
                  )
                )
            },
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(OrganisationSecondContactPhoneNumberPage, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(OrganisationSecondContactPhoneNumberPage, mode, updatedAnswers))
        )
  }
}
