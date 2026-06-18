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

import controllers.actions._
import forms.organisation.UtrFormProvider
import models.Mode
import navigation.Navigator
import pages.organisation.UtrPage
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.organisation.UtrView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UtrController @Inject() (
    override val messagesApi: MessagesApi,
    sessionRepository: SessionRepository,
    navigator: Navigator,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    formProvider: UtrFormProvider,
    val controllerComponents: MessagesControllerComponents,
    view: UtrView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging
    with RcaspNameHelper {

  val form: Form[String] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify() andThen getData() andThen requireData) { implicit request =>
      val rcaspName    = rcaspDisplayName(request.userAnswers).getOrElse("")
      val preparedForm = request.userAnswers
        .get(UtrPage)
        .fold(form)(form.fill)

      Ok(view(preparedForm, mode, rcaspName))
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify() andThen getData() andThen requireData).async { implicit request =>
      val rcaspName = rcaspDisplayName(request.userAnswers).getOrElse("")

      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode, rcaspName))),
          value =>
            for {
              updatedAnswers <- Future.fromTry(
                                  request.userAnswers.set(UtrPage, value)
                                )
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(
              navigator.nextPage(UtrPage, mode, updatedAnswers)
            )
        )
    }
}
