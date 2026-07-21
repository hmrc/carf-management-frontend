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
import models.{UserAnswers, UserBusinessSubscriptionData}
import models.viewAndUpdateRcasp.RcaspDetails
import pages.SubmissionSucceededPage
import pages.remove.{RemoveRcaspCachedDetails, RemoveUserAccessPage, RemoveUserBusinessInfoCached}
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
    requireData: DataRequiredAction,
    submissionLock: SubmissionLockAction,
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

  private def pageUnavailable: Result = Redirect(
    controllers.routes.PlaceholderController.onPageLoad("Should nav to /problem/page-unavailable (CARF-308)")
  )

  private def render(
      form: Form[Boolean],
      rcaspId: String,
      vm: RemoveUserAccessViewModel
  )(implicit request: Request[_], messages: Messages) =
    view(form, rcaspId, vm.titleKey, vm.headingKey, vm.rcaspName, vm.userBusinessNameOpt)

  private def cacheAndRender(
      rcaspId: String,
      details: RcaspDetails,
      userInfo: UserBusinessSubscriptionData
  )(implicit request: OptionalDataRequest[AnyContent], messages: Messages): Future[Result] =
    for {
      answers        <- Future.fromTry(
                          UserAnswers(id = request.userId, rcaspIsRegisteredBusiness = false)
                            .set(RemoveRcaspCachedDetails, details)
                        )
      updatedAnswers <- Future.fromTry(
                          answers.set(RemoveUserBusinessInfoCached, userInfo)
                        )
      _              <- sessionRepository.set(updatedAnswers)
    } yield {
      val vm = RemoveUserAccessViewModel.from(details, userInfo, formProvider)
      Ok(render(vm.form, rcaspId, vm))
    }

  private def fetchAndCache(
      rcaspId: String
  )(implicit request: OptionalDataRequest[AnyContent], messages: Messages): Future[Result] =
    accountService.getRcaspDetails(request.carfId, rcaspId).value.flatMap {
      case Right(details) =>
        accountService.getUserBusinessSubscriptionData(request.carfId).value.flatMap {
          case Right(userInfo) => cacheAndRender(rcaspId, details, userInfo)
          case Left(error)     =>
            logger.warn(s"[RemoveUserAccessController][onPageLoad] Failed to get user business info: $error")
            Future.successful(journeyRecovery)
        }
      case Left(error)    =>
        logger.warn(s"[RemoveUserAccessController][onPageLoad] Failed to get RCASP details: $error")
        Future.successful(journeyRecovery)
    }

  def onPageLoad(rcaspId: String): Action[AnyContent] = (identify() andThen getData()).async { implicit request =>
    val submissionSucceeded = request.userAnswers.flatMap(_.get(SubmissionSucceededPage)).contains(true)

    val cachedDetails = request.userAnswers.flatMap(_.get(RemoveRcaspCachedDetails))

    val cachedUserInfo: Option[UserBusinessSubscriptionData] =
      request.userAnswers.flatMap(_.get(RemoveUserBusinessInfoCached))

    val cachedRcaspIdMatches = cachedDetails.exists(_.RCASPID.equalsIgnoreCase(rcaspId))

    (cachedRcaspIdMatches, submissionSucceeded) match {
      case (true, true) =>
        logger.info(
          "[RemoveUserAccessController][onPageLoad] RCASP already removed - redirecting to page-unavailable"
        )
        Future.successful(pageUnavailable)

      case (true, false) =>
        (cachedDetails, cachedUserInfo) match {
          case (Some(details), Some(userInfo)) =>
            val vm           = RemoveUserAccessViewModel.from(details, userInfo, formProvider)
            val preparedForm = request.userAnswers.flatMap(_.get(RemoveUserAccessPage)).fold(vm.form)(vm.form.fill)
            Future.successful(Ok(render(preparedForm, rcaspId, vm)))

          case _ =>
            fetchAndCache(rcaspId)
        }

      case (false, _) =>
        fetchAndCache(rcaspId)
    }
  }

  def onSubmit(rcaspId: String): Action[AnyContent] =
    (identify() andThen getData() andThen submissionLock andThen requireData).async { implicit request =>
      val cachedDetails = request.userAnswers
        .get(RemoveRcaspCachedDetails)
        .filter(_.RCASPID.equalsIgnoreCase(rcaspId))

      val cachedUserInfo: Option[UserBusinessSubscriptionData] = request.userAnswers.get(RemoveUserBusinessInfoCached)

      (cachedDetails, cachedUserInfo) match {
        case (Some(details), Some(userInfo)) =>
          val vm = RemoveUserAccessViewModel.from(details, userInfo, formProvider)

          vm.form
            .bindFromRequest()
            .fold(
              formWithErrors => Future.successful(BadRequest(render(formWithErrors, rcaspId, vm))),
              value =>
                for {
                  updatedAnswers <- Future.fromTry(
                                      request.userAnswers.set(RemoveUserAccessPage, value)
                                    )
                  _              <- sessionRepository.set(updatedAnswers)
                } yield Redirect(controllers.remove.routes.RemoveOtherAccessController.onPageLoad())
            )

        case _ =>
          logger.warn(
            "[RemoveUserAccessController][onSubmit] RemoveRcaspCachedDetails or user business info not found in cache"
          )
          Future.successful(journeyRecovery)
      }
    }
}
