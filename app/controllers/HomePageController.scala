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

import cats.data.EitherT
import config.Constants.ZERO
import config.FrontendAppConfig
import controllers.actions.{CtUtrRetrievalAction, DataRetrievalAction, IdentifierAction}
import models.errors.CarfError
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{AccountService, UploadInformationService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.HomePageViewModel
import views.html.HomePageView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class HomePageController @Inject() (
    val controllerComponents: MessagesControllerComponents,
    identify: IdentifierAction,
    ctUtrRetrievalAction: CtUtrRetrievalAction,
    accountService: AccountService,
    uploadInformationService: UploadInformationService,
    appConfig: FrontendAppConfig,
    getData: DataRetrievalAction,
    view: HomePageView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(): Action[AnyContent] = (identify() andThen ctUtrRetrievalAction() andThen getData()).async {
    implicit request =>
      val carfId = request.carfId

      val viewModelFuture = for {
        numberOfRcaspsCurrentlyAdded     <- accountService.getNumberOfRcaspsCurrentlyAdded(carfId)
        hasOrganisationContactDetails    <- accountService.hasOrganisationContactDetails(carfId)
        organisationName                 <-
          if (hasOrganisationContactDetails) {
            accountService.getOrganisationName(carfId)
          } else {
            EitherT.rightT[Future, CarfError](None: Option[String])
          }
        hasUserUploadedFilesInLast28Days <- uploadInformationService.hasUserUploadedFilesInLast28Days(carfId)
      } yield HomePageViewModel(
        isBusiness = hasOrganisationContactDetails,
        hasZeroRcaspsAdded = numberOfRcaspsCurrentlyAdded == ZERO,
        hasSentFilesInLast28Days = hasUserUploadedFilesInLast28Days,
        organisationName = organisationName,
        ctUtr = request.utr,
        carfId = carfId
      )

      viewModelFuture.value.map {
        case Left(error)      =>
          logger.warn("[HomePageController] Error generating view model!")
          Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
        case Right(viewModel) =>
          val aeoiEmail: String               = appConfig.aeoiEmailAddress
          val changeContactDetailsUrl: String = appConfig.changeContactDetailsIndexUrl
          Ok(view(viewModel, aeoiEmail, changeContactDetailsUrl))
      }
  }
}
