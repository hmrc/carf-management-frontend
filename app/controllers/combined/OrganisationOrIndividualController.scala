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
import controllers.routes
import forms.combined.OrganisationOrIndividualFormProvider
import models.{ChangeMode, Mode, NormalMode, OrganisationOrIndividual, UserAnswers}
import navigation.Navigator
import pages.changeDetails.ChangeRcaspCachedDetails
import pages.combined.OrganisationOrIndividualPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import repositories.SessionRepository
import types.ResultT
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
    submissionLock: SubmissionLockAction,
    formProvider: OrganisationOrIndividualFormProvider,
    val controllerComponents: MessagesControllerComponents,
    view: OrganisationOrIndividualView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form: Form[OrganisationOrIndividual] = formProvider("organisationOrIndividual.error.required")
  private lazy val recovery: Call          = routes.JourneyRecoveryController.onPageLoad()

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify() andThen getData() andThen submissionLock andThen requireData) { implicit request =>
      val preparedForm = request.userAnswers.get(OrganisationOrIndividualPage).fold(form)(form.fill)
      Ok(view(preparedForm, mode))
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify() andThen getData() andThen submissionLock andThen requireData).async { implicit request =>
      val userAnswers                                               = request.userAnswers
      lazy val hasValueChanged: OrganisationOrIndividual => Boolean =
        newValue => !userAnswers.get(OrganisationOrIndividualPage).contains(newValue)

      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode))),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(OrganisationOrIndividualPage, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield mode match {
              case NormalMode => Redirect(navigator.nextPage(OrganisationOrIndividualPage, mode, updatedAnswers))
              case ChangeMode =>
                Redirect {
                  if (hasValueChanged(value)) {
                    navigateFromOrganisationOrIndividualPage(updatedAnswers)
                  } else {
                    changeDetailsNavigation(updatedAnswers)
                  }
                }
            }
        )
    }

  private def navigateFromOrganisationOrIndividualPage(userAnswers: UserAnswers): Call =
    userAnswers.get(OrganisationOrIndividualPage) match {
      case Some(OrganisationOrIndividual.Organisation) =>
        controllers.organisation.routes.OrganisationNameController.onPageLoad(NormalMode)
      case Some(OrganisationOrIndividual.Individual)   =>
        controllers.individual.routes.IndividualNameController.onPageLoad(NormalMode)
      case None                                        => recovery // Code coverage will never be met here. TODO configure to ignore this line [CARF-525]
    }

  private def changeDetailsNavigation(userAnswers: UserAnswers): Call = {
    val maybeRcaspId = userAnswers.get(ChangeRcaspCachedDetails).map(_.RCASPID)

    maybeRcaspId.fold(routes.JourneyRecoveryController.onPageLoad()) { rcaspId =>
      controllers.changeDetails.routes.ChangeDetailsController.onPageLoad(rcaspId)
    }
  }
}
