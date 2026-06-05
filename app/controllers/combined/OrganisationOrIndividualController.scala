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

package controllers.combined

import controllers.actions.*
import forms.OrganisationOrIndividualFormProvider
import models.{Mode, OrganisationOrIndividual}
import navigation.Navigator
import pages.combined.OrganisationOrIndividualPage
import pages.organisation.OverwritableOrganisationName
import play.api.data.Form
import play.api.i18n.Lang.logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.combined.OrganisationOrIndividualView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class OrganisationOrIndividualController @Inject() (
    override val messagesApi: MessagesApi,
    sessionRepository: SessionRepository,
    navigator: Navigator,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    formProvider: OrganisationOrIndividualFormProvider,
    val controllerComponents: MessagesControllerComponents,
    view: OrganisationOrIndividualView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form: Form[OrganisationOrIndividual] = formProvider("organisationOrIndividual.error.required")

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify() andThen getData() andThen requireData) {
    implicit request =>
      if (!request.userAnswers.isCtAutoMatched) {
        Redirect(controllers.combined.routes.OrganisationOrIndividualController.onPageLoad(mode))
      } else {
        val preparedForm = request.userAnswers.get(OrganisationOrIndividualPage).fold(form)(form.fill)

        request.userAnswers
          .get(OverwritableOrganisationName)
          .fold {
            logger.warn(
              "[ReportForRegisteredBusinessController][onPageLoad] Error! Organisation name could not be retrieved from user answers"
            )
            Redirect(
              controllers.routes.PlaceholderController
                .onPageLoad("Should redirect to Some Information is Missing Page (CARF-293)")
            )
          }(orgName => Ok(view(preparedForm, mode)))
      }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify() andThen getData() andThen requireData).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode))),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(OrganisationOrIndividualPage, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(OrganisationOrIndividualPage, mode, updatedAnswers))
        )
  }
}
