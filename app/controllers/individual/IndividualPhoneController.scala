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
import forms.GenericPhoneFormProvider

import javax.inject.Inject
import models.Mode
import navigation.Navigator
import pages.individual.{IndividualNamePage, IndividualPhonePage}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.individual.IndividualPhoneView
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

class IndividualPhoneController @Inject() (
    override val messagesApi: MessagesApi,
    sessionRepository: SessionRepository,
    navigator: Navigator,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    formProvider: GenericPhoneFormProvider,
    val controllerComponents: MessagesControllerComponents,
    view: IndividualPhoneView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  val form: Form[String] = formProvider("individualPhone")

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify() andThen getData() andThen requireData) {
    implicit request =>

      val preparedForm = request.userAnswers.get(IndividualPhonePage).fold(form)(form.fill)

      request.userAnswers
        .get(IndividualNamePage)
        .fold {
          logger.warn(
            "[IndividualPhoneController][onPageLoad] Error! Individual name could not be retrieved from user answers"
          )
          Redirect(
            controllers.routes.PlaceholderController
              .onPageLoad("Should redirect to Some Information is Missing Page (CARF-293)")
          )
        }(individualName => Ok(view(preparedForm, mode, individualName.fullName)))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify() andThen getData() andThen requireData).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            request.userAnswers
              .get(IndividualNamePage)
              .fold {
                logger.warn(
                  "[IndividualPhoneController][onSubmit] Error! Individual name could not be retrieved from user answers"
                )
                Future.successful(
                  Redirect(
                    controllers.routes.PlaceholderController
                      .onPageLoad("Should redirect to Some Information is Missing Page (CARF-293)")
                  )
                )
              }(individualName => Future.successful(BadRequest(view(formWithErrors, mode, individualName.fullName)))),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(IndividualPhonePage, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(IndividualPhonePage, mode, updatedAnswers))
        )
  }
}
