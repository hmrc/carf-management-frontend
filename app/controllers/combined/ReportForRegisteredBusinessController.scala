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
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.RegistrationService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.combined.ReportForRegisteredBusinessView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ReportForRegisteredBusinessController @Inject() (
    override val messagesApi: MessagesApi,
    sessionRepository: SessionRepository,
    navigator: Navigator,
    identify: IdentifierAction,
    ctUtrRetrievalAction: CtUtrRetrievalAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    formProvider: GenericYesNoPageFormProvider,
    registrationService: RegistrationService,
    val controllerComponents: MessagesControllerComponents,
    view: ReportForRegisteredBusinessView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  val form: Form[Boolean] = formProvider("reportForRegisteredBusiness.error.required")

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify() andThen ctUtrRetrievalAction() andThen getData() andThen requireData).async { implicit request =>
      request.utr match {
        case Some(utr) =>
          registrationService.getBusinessWithUtr(utr.uniqueTaxPayerReference).map { businessDetails =>
            val preparedForm =
              request.userAnswers.get(ReportForRegisteredBusinessPage).fold(form)(form.fill)

            Ok(view(preparedForm, mode, businessDetails.name))
          }

        case None =>
          logger.warn(
            "[ReportForRegisteredBusinessController][onPageLoad] Error! CT UTR could not be retrieved from request"
          )
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify() andThen ctUtrRetrievalAction() andThen getData() andThen requireData).async { implicit request =>
      request.utr match {
        case Some(utr) =>
          form
            .bindFromRequest()
            .fold(
              formWithErrors =>
                registrationService.getBusinessWithUtr(utr.uniqueTaxPayerReference).map { businessDetails =>
                  BadRequest(view(formWithErrors, mode, businessDetails.name))
                },
              value =>
                for {
                  updatedAnswers <- Future.fromTry(request.userAnswers.set(ReportForRegisteredBusinessPage, value))
                  _              <- sessionRepository.set(updatedAnswers)
                } yield Redirect(navigator.nextPage(ReportForRegisteredBusinessPage, mode, updatedAnswers))
            )

        case None =>
          logger.warn(
            "[ReportForRegisteredBusinessController][onSubmit] Error! CT UTR could not be retrieved from request"
          )
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }
    }
}
