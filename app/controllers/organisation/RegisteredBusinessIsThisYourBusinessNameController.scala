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
import pages.organisation.{CachedBusinessDetailsPage, OverwritableOrganisationName, RegisteredBusinessIsThisYourBusinessNamePage}
import utils.LoggerUtil.*
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.organisation.RegisteredBusinessIsThisYourBusinessNameView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

class RegisteredBusinessIsThisYourBusinessNameController @Inject() (
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
    view: RegisteredBusinessIsThisYourBusinessNameView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form: Form[Boolean] = formProvider("registeredBusinessIsThisYourBusinessName.error.required")

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify andThen ctUtrRetrievalAction() andThen getData() andThen submissionLock andThen requireData).async {
      implicit request =>
        request.userAnswers.get(CachedBusinessDetailsPage) match {
          case Some(businessDetails) =>
            val preparedForm =
              request.userAnswers.get(RegisteredBusinessIsThisYourBusinessNamePage).fold(form)(form.fill)

            Future.successful(Ok(view(preparedForm, mode, businessDetails.name)))

          case None =>
            logWarn(
              "[RegisteredBusinessIsThisYourBusinessNameController][onPageLoad] No cached business details found"
            )
            Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
        }
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify andThen ctUtrRetrievalAction() andThen getData() andThen submissionLock andThen requireData).async {
      implicit request =>
        request.userAnswers.get(CachedBusinessDetailsPage) match {
          case Some(businessDetails) =>
            form
              .bindFromRequest()
              .fold(
                formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode, businessDetails.name))),
                value =>
                  for {
                    updatedAnswers     <- Future.fromTry(
                                            request.userAnswers.set(RegisteredBusinessIsThisYourBusinessNamePage, value)
                                          )
                    uaWithOrgNameIfYes <- Future.fromTry(
                                            if (value) {
                                              updatedAnswers.set(OverwritableOrganisationName, businessDetails.name)
                                            } else { Success(updatedAnswers) }
                                          )
                    _                  <- sessionRepository.set(uaWithOrgNameIfYes)
                  } yield Redirect(
                    navigator.nextPage(RegisteredBusinessIsThisYourBusinessNamePage, mode, uaWithOrgNameIfYes)
                  )
              )

          case None =>
            logWarn(
              "[RegisteredBusinessIsThisYourBusinessNameController][onSubmit] No cached business details found"
            )
            Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
        }
    }

}
