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
import models.Mode
import navigation.Navigator
import pages.{AddressPagePrePop, ReviewAddressPageForNavigatorOnly, UkAddressInUserAnswers}
import utils.LoggerUtil.*
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.ReviewAddressView

import scala.concurrent.{ExecutionContext, Future}

class ReviewAddressController @Inject() (
    override val messagesApi: MessagesApi,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    submissionLock: SubmissionLockAction,
    navigator: Navigator,
    sessionRepository: SessionRepository,
    val controllerComponents: MessagesControllerComponents,
    view: ReviewAddressView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify() andThen getData() andThen submissionLock andThen requireData) { implicit request =>

      val editAddressLink: String =
        routes.AddressController.onPageLoad(mode).url

      request.userAnswers.get(AddressPagePrePop) match {
        case Some(address) =>
          request.userAnswers.retrieveRcaspName match {
            case Some(name) => Ok(view(address, mode, editAddressLink, name))
            case None       =>
              logWarn(
                "[ReviewAddressController][onPageLoad] Could not retrieve IndividualNamePage and/or OverwritableOrganisationName"
              )
              Redirect(controllers.routes.InformationMissingController.onPageLoad())
          }
        case None          =>
          logWarn("[ReviewAddressController][onPageLoad] No address found in user answers")
          Redirect(routes.JourneyRecoveryController.onPageLoad())
      }
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify() andThen getData() andThen submissionLock andThen requireData).async { implicit request =>
      request.userAnswers.get(AddressPagePrePop) match {
        case Some(address) =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(UkAddressInUserAnswers, address))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(ReviewAddressPageForNavigatorOnly, mode, updatedAnswers))
        case None          =>
          logError("[ReviewAddressController][onSubmit] No address found in user answers")
          Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
      }
    }
}
