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

package controllers

import controllers.actions.*
import models.{Mode, UniqueTaxpayerReference, UserAnswers}
import navigation.Navigator
import pages.{AddressPagePrePop, ReviewAddressPageForNavigatorOnly, UkAddressInUserAnswers}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.RcaspHelper
import views.html.ReviewAddressView
import com.google.inject.Inject
import services.AccountService

import scala.concurrent.{ExecutionContext, Future}

class ReviewAddressController @Inject() (
    override val messagesApi: MessagesApi,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    navigator: Navigator,
    sessionRepository: SessionRepository,
    accountService: AccountService,
    val controllerComponents: MessagesControllerComponents,
    view: ReviewAddressView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify() andThen getData() andThen requireData).async {
    implicit request =>

      val editAddressLink: String =
        routes.PlaceholderController.onPageLoad("Should nav to /address (CARF-203)").url

      request.userAnswers.get(AddressPagePrePop) match {
        case Some(address) =>
          RcaspHelper.retrieveRcaspName(request.userAnswers) match {
            case Some(name) => Future.successful(Ok(view(address, mode, editAddressLink, name)))
            case None       =>
              logger.warn(
                "[ReviewAddressController] Could not retrieve IndividualNamePage and/or OverwritableOrganisationName onPageLoad"
              )
              Future.successful(Redirect(controllers.routes.InformationMissingController.onPageLoad()))
          }
        case None          =>
          logger.warn("No address found in user answers")
          Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
      }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify() andThen getData() andThen requireData).async {
    implicit request =>
      request.userAnswers.get(AddressPagePrePop) match {
        case Some(address) =>
          for {
            updatedAnswers <-
              Future.fromTry(request.userAnswers.set(UkAddressInUserAnswers, address))
            _              <- sessionRepository.set(updatedAnswers)
            result         <-
              accountService
                .getNumberOfRcaspsCurrentlyAdded(request.carfId)
                .map(count => Redirect(isRcaspUserRedirect(count, request.utr, updatedAnswers, mode)))
                .leftMap { error =>
                  logger.warn(s"[ReviewAddressController] Error retrieving RCASP count: $error")
                  Redirect(routes.JourneyRecoveryController.onPageLoad())
                }
                .merge
          } yield result
        case None          =>
          logger.error("No address found in user answers")
          Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
      }
  }

  private def isRcaspUserRedirect(
      rcaspCount: Int,
      ctUtr: Option[UniqueTaxpayerReference],
      userAnswers: UserAnswers,
      mode: Mode
  ): Call =
    if (RcaspHelper.isRcaspUser(rcaspCount, ctUtr, userAnswers)) {
      routes.PlaceholderController.onPageLoad("Should nav to /registered-business/check-answers (CARF-294)")
    } else {
      navigator.nextPage(ReviewAddressPageForNavigatorOnly, mode, userAnswers)
    }

}
