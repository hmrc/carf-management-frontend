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
import controllers.routes
import forms.GenericYesNoPageFormProvider
import models.{ChangeMode, Mode, NormalMode, UserAnswers}
import navigation.Navigator
import pages.changeDetails.ChangeRcaspCachedDetails
import pages.organisation.{HaveTradingNamePage, OverwritableOrganisationName}
import utils.LoggerUtil.*
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.organisation.HaveTradingNameView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class HaveTradingNameController @Inject() (
    override val messagesApi: MessagesApi,
    sessionRepository: SessionRepository,
    navigator: Navigator,
    identify: IdentifierAction,
    ctUtrRetrievalAction: CtUtrRetrievalAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    submissionLock: SubmissionLockAction,
    formProvider: GenericYesNoPageFormProvider,
    val controllerComponents: MessagesControllerComponents,
    view: HaveTradingNameView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form: Form[Boolean]         = formProvider("haveTradingName.error.required")
  private lazy val recovery: Call = routes.JourneyRecoveryController.onPageLoad()

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify() andThen ctUtrRetrievalAction() andThen getData() andThen submissionLock andThen requireData) {
      implicit request =>
        val preparedForm = request.userAnswers.get(HaveTradingNamePage).fold(form)(form.fill)

        request.userAnswers
          .get(OverwritableOrganisationName)
          .fold {
            logWarn(
              "[HaveTradingNameController][onPageLoad] Error! Organisation name could not be retrieved from user answers"
            )
            Redirect(controllers.routes.InformationMissingController.onPageLoad())
          }(orgName => Ok(view(preparedForm, mode, orgName)))
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify() andThen ctUtrRetrievalAction() andThen getData() andThen submissionLock andThen requireData)
      .async { implicit request =>
        val userAnswers                              = request.userAnswers
        lazy val hasValueChanged: Boolean => Boolean =
          newValue => !userAnswers.get(HaveTradingNamePage).contains(newValue)

        form
          .bindFromRequest()
          .fold(
            formWithErrors =>
              request.userAnswers
                .get(OverwritableOrganisationName)
                .fold {
                  logWarn(
                    "[HaveTradingNameController][onSubmit] Error! Organisation name could not be retrieved from user answers"
                  )
                  Future.successful(
                    Redirect(controllers.routes.InformationMissingController.onPageLoad())
                  )
                }(orgName => Future.successful(BadRequest(view(formWithErrors, mode, orgName)))),
            value =>
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(HaveTradingNamePage, value))
                _              <- sessionRepository.set(updatedAnswers)
              } yield mode match {
                case NormalMode => Redirect(navigator.nextPage(HaveTradingNamePage, mode, updatedAnswers))
                case ChangeMode =>
                  Redirect {
                    if (hasValueChanged(value)) {
                      navigateFromHaveTradingNamePage(updatedAnswers)
                    } else {
                      changeDetailsNavigation(updatedAnswers)
                    }
                  }
              }
          )
      }

  private def navigateFromHaveTradingNamePage(userAnswers: UserAnswers): Call =
    userAnswers.get(HaveTradingNamePage) match {
      case Some(true)  => controllers.organisation.routes.TradingNameController.onPageLoad(ChangeMode)
      case Some(false) => changeDetailsNavigation(userAnswers)
      case None        => recovery // Code coverage will never be met here. TODO configure to ignore this line [CARF-525]
    }

  private def changeDetailsNavigation(userAnswers: UserAnswers): Call = {
    val maybeRcaspId = userAnswers.get(ChangeRcaspCachedDetails).map(_.RCASPID)

    maybeRcaspId.fold(recovery) { rcaspId =>
      controllers.changeDetails.routes.ChangeDetailsRoutingController.onPageLoad(rcaspId)
    }
  }
}
