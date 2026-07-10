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

import controllers.actions.*
import forms.GenericYesNoPageFormProvider
import models.UserAnswers
import models.viewAndUpdateRcasp.RcaspDetails
import pages.remove.{RemoveRcaspCachedDetails, RemoveUserAccessPage}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.SessionRepository
import services.AccountService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.remove.RemoveUserAccessViewModel
import views.html.remove.RemoveUserAccessView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RemoveUserAccessController @Inject() (
    override val messagesApi: MessagesApi,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    sessionRepository: SessionRepository,
    formProvider: GenericYesNoPageFormProvider,
    accountService: AccountService,
    val controllerComponents: MessagesControllerComponents,
    view: RemoveUserAccessView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  private val journeyRecovery: Result = Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())

  private def buildViewModel(
      details: RcaspDetails,
      userBusinessNameOpt: Option[String]
  ): Either[Result, RemoveUserAccessViewModel] =
    RemoveUserAccessViewModel
      .from(details, userBusinessNameOpt, formProvider)
      .left
      .map { errorMessage =>
        logger.warn(s"[RemoveUserAccessController][buildViewModel] $errorMessage")
        journeyRecovery
      }

  def onPageLoad(rcaspId: String): Action[AnyContent] =
    (identify() andThen getData()).async { implicit request =>

      val cachedDetails = request.userAnswers
        .flatMap(_.get(RemoveRcaspCachedDetails))
        .filter(_.RCASPID.toUpperCase == rcaspId.toUpperCase)

      cachedDetails match {
        case Some(details) =>
          accountService.getUserBusinessName(request.carfId).value.map {
            case Right(userBusinessNameOpt) =>
              buildViewModel(details, userBusinessNameOpt) match {
                case Right(vm)      =>
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
                case Left(recovery) => recovery
              }
            case Left(_)                    =>
              logger.warn("[RemoveUserAccessController][onPageLoad] Failed to get user business name")
              journeyRecovery
          }

        case None =>
          accountService.getRcaspDetails(request.carfId, rcaspId).value.flatMap {
            case Right(details) =>
              for {
                userBusinessResult <- accountService.getUserBusinessName(request.carfId).value
                userBusinessNameOpt = userBusinessResult.toOption.flatten
                userAnswers        <- Future.fromTry(
                                        UserAnswers(
                                          id = request.userId,
                                          rcaspIsRegisteredBusiness = false
                                        ).set(RemoveRcaspCachedDetails, details)
                                      )
                _                  <- sessionRepository.set(userAnswers)
              } yield buildViewModel(details, userBusinessNameOpt) match {
                case Right(vm)      =>
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
                case Left(recovery) => recovery
              }

            case Left(error) =>
              logger.warn(s"[RemoveUserAccessController][onPageLoad] Failed to get RCASP details: $error")
              Future.successful(journeyRecovery)
          }
      }
    }

  def onSubmit(rcaspId: String): Action[AnyContent] =
    (identify() andThen getData()).async { implicit request =>

      val cachedDetails = request.userAnswers
        .flatMap(_.get(RemoveRcaspCachedDetails))
        .filter(_.RCASPID.toUpperCase == rcaspId.toUpperCase)

      cachedDetails match {
        case Some(details) =>
          accountService.getUserBusinessName(request.carfId).value.flatMap {
            case Right(userBusinessNameOpt) =>
              buildViewModel(details, userBusinessNameOpt) match {
                case Right(vm)      =>
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
                          userAnswers <-
                            Future.fromTry(
                              request.userAnswers
                                .getOrElse(UserAnswers(id = request.userId, rcaspIsRegisteredBusiness = false))
                                .set(RemoveUserAccessPage, value)
                            )
                          _           <- sessionRepository.set(userAnswers)
                        } yield Redirect(
                          controllers.remove.routes.RemoveOtherAccessController.onPageLoad(rcaspId)
                        )
                    )
                case Left(recovery) =>
                  Future.successful(recovery)
              }
            case Left(_)                    =>
              logger.warn("[RemoveUserAccessController][onSubmit] Failed to get user business name")
              Future.successful(journeyRecovery)
          }

        case None =>
          logger.warn("[RemoveUserAccessController][onSubmit] RemoveRcaspCachedDetails not found in cache")
          Future.successful(journeyRecovery)
      }
    }
}
