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

import javax.inject.Inject
import models.Mode
import navigation.Navigator
import pages.organisation.OrganisationHaveSecondContactPage
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.organisation.OrganisationHaveSecondContactView
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

class OrganisationHaveSecondContactController @Inject() (
    override val messagesApi: MessagesApi,
    sessionRepository: SessionRepository,
    navigator: Navigator,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    formProvider: GenericYesNoPageFormProvider,
    val controllerComponents: MessagesControllerComponents,
    view: OrganisationHaveSecondContactView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  val form: Form[Boolean] = formProvider("organisationHaveSecondContact.error.required")

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify() andThen getData() andThen requireData) {
    implicit request =>

      val preparedForm = request.userAnswers.get(OrganisationHaveSecondContactPage).fold(form)(form.fill)

      // TODO: Add FirstContactNamePage (CARF-204)
//      request.userAnswers.get(FirstContactNamePage) match {
//        case Some(firstContactName) => Ok(view(preparedForm, mode, firstContactName))
//        case None                   =>
//          logger.warn("[OrganisationHaveSecondContactController] Could not retrieve FirstContactNamePage onPageLoad")
//          Redirect(
      //          controllers.routes.PlaceholderController.onPageLoad(
      //              "Should redirect to Some Information is Missing Page (CARF-293)"
      //            ))
//      }

      Ok(view(preparedForm, mode, "name"))

  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify() andThen getData() andThen requireData).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            // TODO: Add FirstContactNamePage (CARF-204)
//            request.userAnswers.get(FirstContactNamePage) match {
//              case Some(firstContactName) => Future.successful(BadRequest(view(formWithErrors, mode, firstContactName)))
//              case None                   =>
//                logger.warn(
//                  "[OrganisationHaveSecondContactController] Could not retrieve FirstContactNamePage onPageSubmit"
//                )
//                Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
//            }
            Future.successful(BadRequest(view(formWithErrors, mode, "name"))),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(OrganisationHaveSecondContactPage, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(OrganisationHaveSecondContactPage, mode, updatedAnswers))
        )
  }
}
