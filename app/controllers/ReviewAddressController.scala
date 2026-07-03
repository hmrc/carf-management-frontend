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

import com.google.inject.Inject
import controllers.actions.*
import models.{Mode, UniqueTaxpayerReference, UserAnswers}
import navigation.Navigator
import pages.{AddressPagePrePop, ReviewAddressPageForNavigatorOnly, UkAddressInUserAnswers}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import repositories.SessionRepository
import services.AccountService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.ReviewAddressView

import scala.concurrent.{ExecutionContext, Future}

class ReviewAddressController @Inject() (
    override val messagesApi: MessagesApi,
    identify: IdentifierAction,
    ctUtrRetrievalAction: CtUtrRetrievalAction,
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
          request.userAnswers.retrieveRcaspName match {
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

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify() andThen ctUtrRetrievalAction() andThen getData() andThen requireData).async { implicit request =>
      request.userAnswers.get(AddressPagePrePop) match {
        case Some(address) =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(UkAddressInUserAnswers, address))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(ReviewAddressPageForNavigatorOnly, mode, updatedAnswers))
        case None          =>
          logger.error("No address found in user answers")
          Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
      }
    }

}
