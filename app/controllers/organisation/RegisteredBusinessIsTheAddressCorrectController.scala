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
import forms.GenericYesNoPageFormProvider
import models.Mode
import navigation.Navigator
import pages.organisation.{CachedBusinessDetailsPage, RegisteredBusinessIsTheAddressCorrectPage}
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.organisation.RegisteredBusinessIsTheAddressCorrectView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RegisteredBusinessIsTheAddressCorrectController @Inject() (
    override val messagesApi: MessagesApi,
    sessionRepository: SessionRepository,
    navigator: Navigator,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    formProvider: GenericYesNoPageFormProvider,
    val controllerComponents: MessagesControllerComponents,
    view: RegisteredBusinessIsTheAddressCorrectView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  val form: Form[Boolean] = formProvider("registeredBusinessIsTheAddressCorrect.error.required")

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify() andThen getData() andThen requireData) { implicit request =>
      request.userAnswers.get(CachedBusinessDetailsPage) match {

        case Some(cached) =>
          val preparedForm =
            request.userAnswers
              .get(RegisteredBusinessIsTheAddressCorrectPage)
              .fold(form)(form.fill)

          Ok(
            view(
              preparedForm,
              mode,
              cached.name,
              cached.address,
              cached.countryName
            )
          )

        case None =>
          logger.warn(
            "[RegisteredBusinessIsTheAddressCorrectController][onPageLoad] No cached business details found. Redirecting to journey recovery."
          )
          Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      }
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify() andThen getData() andThen requireData).async { implicit request =>
      request.userAnswers.get(CachedBusinessDetailsPage) match {

        case Some(cached) =>
          form
            .bindFromRequest()
            .fold(
              formWithErrors =>
                Future.successful(
                  BadRequest(
                    view(
                      formWithErrors,
                      mode,
                      cached.name,
                      cached.address,
                      cached.countryName
                    )
                  )
                ),
              value =>
                for {
                  updatedAnswers <- Future.fromTry(
                                      request.userAnswers.set(
                                        RegisteredBusinessIsTheAddressCorrectPage,
                                        value
                                      )
                                    )
                  _              <- sessionRepository.set(updatedAnswers)
                } yield Redirect(
                  navigator.nextPage(
                    RegisteredBusinessIsTheAddressCorrectPage,
                    mode,
                    updatedAnswers
                  )
                )
            )

        case None =>
          logger.warn(
            "[RegisteredBusinessIsTheAddressCorrectController][onSubmit] No cached business details found. Redirecting to journey recovery."
          )
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }
    }
}
