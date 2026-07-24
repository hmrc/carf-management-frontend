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
import models.requests.DataRequest
import models.{NormalMode, UserAnswers}
import pages.changeDetails.ChangeRcaspCachedDetails
import pages.organisation.ReportForRegisteredBusinessPage
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.*
import repositories.SessionRepository
import services.AccountService
import types.ResultT
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.combined.ChangeReportForRegisteredBusinessView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ChangeReportForRegisteredBusinessController @Inject() (
    override val messagesApi: MessagesApi,
    sessionRepository: SessionRepository,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    submissionLock: SubmissionLockAction,
    requireData: DataRequiredAction,
    formProvider: GenericYesNoPageFormProvider,
    accountService: AccountService,
    val controllerComponents: MessagesControllerComponents,
    view: ChangeReportForRegisteredBusinessView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  val form: Form[Boolean]         = formProvider("reportForRegisteredBusiness.changeDetails.error.required")
  private lazy val recovery: Call = controllers.routes.JourneyRecoveryController.onPageLoad()

  def onPageLoad: Action[AnyContent] =
    (identify() andThen getData() andThen submissionLock andThen requireData) { implicit request =>
      request.userAnswers
        .get(ReportForRegisteredBusinessPage)
        .fold {
          Redirect(recovery)
        }(value => Ok(view(form.fill(value))))
    }

  def onSubmit: Action[AnyContent] =
    (identify() andThen getData() andThen submissionLock andThen requireData).async { implicit request =>
      val userAnswers                              = request.userAnswers
      lazy val hasValueChanged: Boolean => Boolean =
        newValue => !userAnswers.get(ReportForRegisteredBusinessPage).contains(newValue)

      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors))),
          value => if hasValueChanged(value) then ifChanged(value) else Future.successful(ifUnchanged(value))
        )
    }

  private def ifChanged(newValue: Boolean)(implicit request: DataRequest[AnyContent]): Future[Result] =
    setRcaspIsRegisteredBusinessFlag(
      userAnswers = request.userAnswers,
      pageAnswer = newValue,
      carfId = request.carfId,
      ctUtr = request.utr.map(_.uniqueTaxPayerReference)
    ).value.flatMap {
      case Right(userAnswers) =>
        for {
          updatedAnswers <- Future.fromTry(userAnswers.set(ReportForRegisteredBusinessPage, newValue))
          _              <- sessionRepository.set(updatedAnswers)
        } yield Redirect {
          (
            request.userAnswers.get(ChangeRcaspCachedDetails),
            newValue
          ) match {
            case (Some(details), true)  =>
              controllers.organisation.routes.RegisteredBusinessIsThisYourBusinessNameController.onPageLoad(NormalMode)
            case (Some(details), false) =>
              controllers.combined.routes.OrganisationOrIndividualController.onPageLoad(NormalMode)
            case _                      => recovery
          }
        }
      case Left(error)        =>
        logger.error(
          s"[ChangeReportForRegisteredBusinessController][onSubmit] Error getting how many Rcasps user has: $error"
        )
        Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
    }

  private def ifUnchanged(value: Boolean)(implicit request: DataRequest[AnyContent]): Result = Redirect {
    (request.userAnswers.get(ChangeRcaspCachedDetails), value) match {
      case (Some(details), true)  =>
        controllers.changeDetails.routes.RegisteredBusinessChangeDetailsController.onPageLoad(details.RCASPID)
      case (Some(details), false) =>
        controllers.changeDetails.routes.ChangeDetailsController.onPageLoad(details.RCASPID)
      case _                      => recovery
    }
  }

  private def setRcaspIsRegisteredBusinessFlag(
      userAnswers: UserAnswers,
      pageAnswer: Boolean,
      carfId: String,
      ctUtr: Option[String]
  )(implicit hc: HeaderCarrier): ResultT[UserAnswers] =
    accountService.getNumberOfRcaspsCurrentlyAdded(carfId = carfId).map { numberOfRcasps =>
      val zeroPlusCurrent = 1
      if (numberOfRcasps == zeroPlusCurrent && pageAnswer && ctUtr.nonEmpty) {
        userAnswers.copy(rcaspIsRegisteredBusiness = true)
      } else {
        userAnswers.copy(rcaspIsRegisteredBusiness = false)
      }
    }
}
