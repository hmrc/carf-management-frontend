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

package controllers.home

import config.Constants.ZERO
import config.FrontendAppConfig
import connectors.SubmissionDetailsConnector
import controllers.actions.{CtUtrRetrievalAction, DataRetrievalAction, IdentifierAction}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.AccountService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.LoggerUtil.*
import viewmodels.HomePageViewModel
import views.html.HomePageView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class HomePageController @Inject() (
    val controllerComponents: MessagesControllerComponents,
    identify: IdentifierAction,
    ctUtrRetrievalAction: CtUtrRetrievalAction,
    accountService: AccountService,
    submissionDetailsConnector: SubmissionDetailsConnector,
    appConfig: FrontendAppConfig,
    getData: DataRetrievalAction,
    view: HomePageView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (identify andThen ctUtrRetrievalAction() andThen getData()).async {
    implicit request =>
      val carfId = request.carfId

      val viewModelFuture = for {
        numberOfRcaspsCurrentlyAdded <- accountService.getNumberOfRcaspsCurrentlyAdded(carfId)
        subscriptionData             <- accountService.getUserBusinessSubscriptionData(carfId)
        hasSentFiles                 <- submissionDetailsConnector.checkForSentFiles(carfId)
      } yield HomePageViewModel(
        isBusiness = subscriptionData.hasOrganisationContactDetails,
        hasZeroRcaspsAdded = numberOfRcaspsCurrentlyAdded == ZERO,
        hasSentFiles = hasSentFiles,
        organisationName = subscriptionData.organisationName,
        ctUtr = request.utr,
        carfId = carfId
      )

      viewModelFuture.value.map {
        case Left(error) =>
          logWarn(s"[HomePageController][onPageLoad] Error generating view model: $error")
          Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())

        case Right(viewModel) =>
          val aeoiEmail: String               = appConfig.aeoiEmailAddress
          val changeContactDetailsUrl: String = appConfig.changeContactDetailsIndexUrl
          val uploadFileUrl: String           = appConfig.carfReportingFrontendUploadFileUrl
          val uploadResultsUrl: String        = appConfig.carfReportingFrontendResultsPageUrl
          Ok(view(viewModel, aeoiEmail, changeContactDetailsUrl, uploadFileUrl, uploadResultsUrl))
      }
  }
}
