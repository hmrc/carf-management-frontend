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

import connectors.RcaspConnector
import controllers.actions.*
import forms.GenericYesNoPageFormProvider
import models.Mode
import navigation.Navigator
import pages.combined.RemoveUserAccessPage
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.SessionRepository
import services.AccountService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.combined.RemoveUserAccessView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RemoveUserAccessController @Inject() (
    override val messagesApi: MessagesApi,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    sessionRepository: SessionRepository,
    formProvider: GenericYesNoPageFormProvider,
    navigator: Navigator,
    rcaspConnector: RcaspConnector,
    accountService: AccountService,
    val controllerComponents: MessagesControllerComponents,
    view: RemoveUserAccessView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  private val IndividualPartyType = "Individual"

  private val journeyRecovery: Result = Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())

  private case class RemoveUserAccessViewData(
      titleKey: String,
      headingKey: String,
      errorKey: String,
      rcaspName: String,
      userBusinessName: Option[String],
      form: Form[Boolean]
  )

  private def buildViewData(
      carfId: String,
      rcaspId: String
  )(implicit hc: HeaderCarrier): Future[Either[Result, RemoveUserAccessViewData]] =
    for {
      rcaspResult        <- rcaspConnector.viewRcasp(carfId).value
      userBusinessResult <- accountService.getUserBusinessName(carfId).value
    } yield (rcaspResult, userBusinessResult) match {
      case (Right(viewRcaspResponse), Right(userBusinessNameOpt)) =>
        val rcaspList        = viewRcaspResponse.ViewRCASP.ResponseDetails.RCASPList
        val selectedRcaspOpt = rcaspList.find(_.RCASPID == rcaspId)

        selectedRcaspOpt match {
          case Some(selectedRcasp) =>
            val rcaspName   = selectedRcasp.getName
            val isRcaspUser = selectedRcasp.IsRCASPUser
            val partyType   = selectedRcasp.PartyType

            val suffix =
              if (partyType == IndividualPartyType) "individual"
              else if (isRcaspUser) "rcaspIsUser"
              else "otherOrg"

            val titleKey   = s"removeUserAccess.title.$suffix"
            val headingKey = s"removeUserAccess.heading.$suffix"
            val errorKey   = s"removeUserAccess.error.required.$suffix"

            val userBusinessName: Option[String] =
              if (partyType == IndividualPartyType) None
              else if (isRcaspUser) Some(rcaspName)
              else userBusinessNameOpt

            if (userBusinessName.isEmpty && partyType != IndividualPartyType && !isRcaspUser) {
              logger.warn(
                s"[RemoveUserAccessController][buildViewData] User business name was missing for rcaspId=$rcaspId"
              )
              Left(journeyRecovery)
            } else {
              Right(
                RemoveUserAccessViewData(
                  titleKey = titleKey,
                  headingKey = headingKey,
                  errorKey = errorKey,
                  rcaspName = rcaspName,
                  userBusinessName = userBusinessName,
                  form = formProvider(errorKey)
                )
              )
            }

          case None =>
            logger.warn(
              s"[RemoveUserAccessController][buildViewData] Could not find selected RCASP for rcaspId=$rcaspId"
            )
            Left(journeyRecovery)
        }

      case _ =>
        logger.warn(
          "[RemoveUserAccessController][buildViewData] Failed to retrieve RCASP details or user business name"
        )
        Left(journeyRecovery)
    }

  def onPageLoad(mode: Mode, rcaspId: String): Action[AnyContent] =
    (identify() andThen getData() andThen requireData).async { implicit request =>
      buildViewData(request.carfId, rcaspId).map {
        case Right(data) =>
          val preparedForm =
            request.userAnswers.get(RemoveUserAccessPage).fold(data.form)(data.form.fill)

          Ok(
            view(
              preparedForm,
              mode,
              rcaspId,
              data.titleKey,
              data.headingKey,
              data.errorKey,
              data.rcaspName,
              data.userBusinessName
            )
          )

        case Left(recovery) =>
          recovery
      }
    }

  def onSubmit(mode: Mode, rcaspId: String): Action[AnyContent] =
    (identify() andThen getData() andThen requireData).async { implicit request =>
      buildViewData(request.carfId, rcaspId).flatMap {
        case Right(data) =>
          data.form
            .bindFromRequest()
            .fold(
              formWithErrors =>
                Future.successful(
                  BadRequest(
                    view(
                      formWithErrors,
                      mode,
                      rcaspId,
                      data.titleKey,
                      data.headingKey,
                      data.errorKey,
                      data.rcaspName,
                      data.userBusinessName
                    )
                  )
                ),
              value =>
                for {
                  updatedAnswers <- Future.fromTry(request.userAnswers.set(RemoveUserAccessPage, value))
                  _              <- sessionRepository.set(updatedAnswers)
                } yield Redirect(navigator.nextPage(RemoveUserAccessPage, mode, updatedAnswers))
            )

        case Left(recovery) =>
          Future.successful(recovery)
      }
    }
}
