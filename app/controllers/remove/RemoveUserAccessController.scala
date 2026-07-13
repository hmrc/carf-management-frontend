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
import pages.remove.{RemoveRcaspCachedDetails, RemoveUserAccessPage, RemoveUserBusinessNameCached}
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request, Result}
import repositories.SessionRepository
import models.requests.OptionalDataRequest
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

  private val journeyRecovery: Result =
    Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())

  private def buildViewModel(
      details: RcaspDetails,
      userBusinessNameOpt: Option[String]
  ): RemoveUserAccessViewModel =
    RemoveUserAccessViewModel.from(details, userBusinessNameOpt, formProvider)

  private def render(
      form: Form[Boolean],
      rcaspId: String,
      vm: RemoveUserAccessViewModel
  )(implicit request: Request[_], messages: Messages) =
    view(form, rcaspId, vm.titleKey, vm.headingKey, vm.errorKey, vm.rcaspName, vm.userBusinessName)

  private def cacheAndRender(
      rcaspId: String,
      details: RcaspDetails,
      userBusinessNameOpt: Option[String]
  )(implicit request: OptionalDataRequest[AnyContent], messages: Messages): Future[Result] = {
    val resolvedBusinessName: String =
      userBusinessNameOpt.getOrElse(messages("homePage.contactDetails.org.fallbackBusinessName"))

    val userAnswersTry =
      request.userAnswers
        .getOrElse(UserAnswers(id = request.userId, rcaspIsRegisteredBusiness = false))
        .set(RemoveRcaspCachedDetails, details)
        .flatMap(_.set(RemoveUserBusinessNameCached, resolvedBusinessName))

    Future
      .fromTry(userAnswersTry)
      .flatMap { ua =>
        sessionRepository.set(ua).map { _ =>
          val vm = buildViewModel(details, Some(resolvedBusinessName))
          Ok(render(vm.form, rcaspId, vm))
        }
      }
  }

  def onPageLoad(rcaspId: String): Action[AnyContent] =
    (identify() andThen getData()).async { implicit request =>

      val cachedDetails =
        request.userAnswers
          .flatMap(_.get(RemoveRcaspCachedDetails))
          .filter(_.RCASPID.equalsIgnoreCase(rcaspId))

      val cachedBusinessName: Option[String] =
        request.userAnswers.flatMap(_.get(RemoveUserBusinessNameCached))

      (cachedDetails, cachedBusinessName) match {

        case (Some(details), Some(userBusinessName)) =>
          val vm           = buildViewModel(details, Some(userBusinessName))
          val preparedForm = request.userAnswers.flatMap(_.get(RemoveUserAccessPage)).fold(vm.form)(vm.form.fill)
          Future.successful(Ok(render(preparedForm, rcaspId, vm)))

        case _ =>
          accountService.getRcaspDetails(request.carfId, rcaspId).value.flatMap {
            case Right(details) =>
              accountService.getUserBusinessName(request.carfId).value.flatMap {
                case Right(userBusinessNameOpt) =>
                  cacheAndRender(rcaspId, details, userBusinessNameOpt)
                case Left(error)                =>
                  logger.warn(
                    s"[RemoveUserAccessController][onPageLoad] Failed to get user business name: $error — continuing with fallback"
                  )
                  cacheAndRender(rcaspId, details, None)
              }
            case Left(error)    =>
              logger.warn(s"[RemoveUserAccessController][onPageLoad] Failed to get RCASP details: $error")
              Future.successful(journeyRecovery)
          }
      }
    }

  def onSubmit(rcaspId: String): Action[AnyContent] =
    (identify() andThen getData()).async { implicit request =>

      val cachedDetails =
        request.userAnswers
          .flatMap(_.get(RemoveRcaspCachedDetails))
          .filter(_.RCASPID.equalsIgnoreCase(rcaspId))

      val cachedBusinessName: Option[String] =
        request.userAnswers.flatMap(_.get(RemoveUserBusinessNameCached))

      (cachedDetails, cachedBusinessName) match {

        case (Some(details), Some(userBusinessName)) =>
          val vm = buildViewModel(details, Some(userBusinessName))

          vm.form
            .bindFromRequest()
            .fold(
              formWithErrors => Future.successful(BadRequest(render(formWithErrors, rcaspId, vm))),
              value =>
                for {
                  updatedAnswers <- Future.fromTry(
                                      request.userAnswers
                                        .getOrElse(UserAnswers(id = request.userId, rcaspIsRegisteredBusiness = false))
                                        .set(RemoveUserAccessPage, value)
                                    )
                  _              <- sessionRepository.set(updatedAnswers)
                } yield Redirect(controllers.remove.routes.RemoveOtherAccessController.onPageLoad(rcaspId))
            )

        case _ =>
          logger.warn(
            "[RemoveUserAccessController][onSubmit] RemoveRcaspCachedDetails or user business name not found in cache"
          )
          Future.successful(journeyRecovery)
      }
    }
}
