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

package controllers.remove

import connectors.RcaspConnector
import controllers.actions.*
import forms.GenericYesNoPageFormProvider
import models.UserAnswers
import pages.remove.RemoveUserAccessPage
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.SessionRepository
import services.AccountService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.remove.RemoveUserAccessViewModel
import views.html.remove.RemoveUserAccessView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RemoveUserAccessController @Inject() (
    override val messagesApi: MessagesApi,
    identify: IdentifierAction,
    sessionRepository: SessionRepository,
    formProvider: GenericYesNoPageFormProvider,
    rcaspConnector: RcaspConnector,
    accountService: AccountService,
    val controllerComponents: MessagesControllerComponents,
    view: RemoveUserAccessView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  private val journeyRecovery: Result =
    Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())

  private def buildViewModel(
      carfId: String,
      rcaspId: String
  )(implicit hc: HeaderCarrier): Future[Either[Result, RemoveUserAccessViewModel]] =
    for {
      rcaspResult        <- rcaspConnector.viewRcasp(carfId).value
      userBusinessResult <- accountService.getUserBusinessName(carfId).value
    } yield (rcaspResult, userBusinessResult) match {
      case (Right(viewRcaspResponse), Right(userBusinessNameOpt)) =>
        RemoveUserAccessViewModel
          .from(rcaspId, viewRcaspResponse, userBusinessNameOpt, formProvider)
          .left
          .map { errorMessage =>
            logger.warn(s"[RemoveUserAccessController][buildViewModel] $errorMessage")
            journeyRecovery
          }

      case _ =>
        logger.warn(
          "[RemoveUserAccessController][buildViewModel] Failed to retrieve RCASP details or user business name"
        )
        Left(journeyRecovery)
    }

  def onPageLoad(rcaspId: String): Action[AnyContent] =
    identify().async { implicit request =>
      val freshAnswers = UserAnswers(
        id = request.userId,
        rcaspIsRegisteredBusiness = false
      )

      for {
        _         <- sessionRepository.set(freshAnswers)
        viewModel <- buildViewModel(request.carfId, rcaspId)
      } yield viewModel match {
        case Right(vm) =>
          Ok(
            view(
              vm.form,
              rcaspId,
              vm.titleKey,
              vm.headingKey,
              vm.errorKey,
              vm.rcaspName,
              vm.userBusinessName
            )
          )

        case Left(recovery) =>
          recovery
      }
    }

  def onSubmit(rcaspId: String): Action[AnyContent] =
    identify().async { implicit request =>
      buildViewModel(request.carfId, rcaspId).flatMap {
        case Right(vm) =>
          vm.form
            .bindFromRequest()
            .fold(
              formWithErrors =>
                Future.successful(
                  BadRequest(
                    view(
                      formWithErrors,
                      rcaspId,
                      vm.titleKey,
                      vm.headingKey,
                      vm.errorKey,
                      vm.rcaspName,
                      vm.userBusinessName
                    )
                  )
                ),
              value =>
                for {
                  userAnswers <- Future.fromTry(
                                   UserAnswers(
                                     id = request.userId,
                                     rcaspIsRegisteredBusiness = false
                                   ).set(RemoveUserAccessPage, value)
                                 )
                  _           <- sessionRepository.set(userAnswers)
                } yield Redirect(
                  controllers.remove.routes.RemoveOtherAccessController.onPageLoad(rcaspId)
                )
            )

        case Left(recovery) =>
          Future.successful(recovery)
      }
    }
}
