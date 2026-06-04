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
import forms.GenericYesNoPageFormProvider
import models.Mode
import navigation.Navigator
import pages.combined.ReportForRegisteredBusinessPage
import pages.organisation.OverwritableOrganisationName
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.combined.ReportForRegisteredBusinessView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ReportForRegisteredBusinessController @Inject() (
    override val messagesApi: MessagesApi,
    sessionRepository: SessionRepository,
    navigator: Navigator,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    formProvider: GenericYesNoPageFormProvider,
    val controllerComponents: MessagesControllerComponents,
    view: ReportForRegisteredBusinessView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  val form: Form[Boolean] = formProvider("reportForRegisteredBusiness.error.required")

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify() andThen getData() andThen requireData) {
    implicit request =>
      if (!request.userAnswers.isCtAutoMatched) {
        Redirect(controllers.combined.routes.OrganisationOrIndividualController.onPageLoad(mode))
      } else {
        val preparedForm = request.userAnswers.get(ReportForRegisteredBusinessPage).fold(form)(form.fill)

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
          }(orgName => Ok(view(preparedForm, mode, orgName)))
      }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify() andThen getData() andThen requireData).async {
    implicit request =>
      if (!request.userAnswers.isCtAutoMatched) {
        Future.successful(Redirect(controllers.combined.routes.OrganisationOrIndividualController.onPageLoad(mode)))
      } else {
        form
          .bindFromRequest()
          .fold(
            formWithErrors =>
              request.userAnswers
                .get(OverwritableOrganisationName)
                .fold {
                  logger.warn(
                    "[ReportForRegisteredBusinessController][onSubmit] Error! Organisation name could not be retrieved from user answers"
                  )
                  Future.successful(
                    Redirect(
                      controllers.routes.PlaceholderController
                        .onPageLoad("Should redirect to Some Information is Missing Page (CARF-293)")
                    )
                  )
                }(orgName => Future.successful(BadRequest(view(formWithErrors, mode, orgName)))),
            value =>
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(ReportForRegisteredBusinessPage, value))
                _              <- sessionRepository.set(updatedAnswers)
              } yield Redirect(navigator.nextPage(ReportForRegisteredBusinessPage, mode, updatedAnswers))
          )
      }
  }
}
