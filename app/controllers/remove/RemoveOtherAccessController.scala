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
import models.Mode
import pages.remove.RemoveOtherAccessPage
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.remove.RemoveOtherAccessView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RemoveOtherAccessController @Inject() (
    override val messagesApi: MessagesApi,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    sessionRepository: SessionRepository,
    formProvider: GenericYesNoPageFormProvider,
    rcaspConnector: RcaspConnector,
    val controllerComponents: MessagesControllerComponents,
    view: RemoveOtherAccessView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  private val IndividualPartyType = "Individual"

  private val journeyRecovery: Result =
    Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())

  private case class RemoveOtherAccessViewData(
      titleKey: String,
      headingKey: String,
      errorKey: String,
      rcaspName: String,
      form: Form[Boolean]
  )

  private def buildViewData(
      carfId: String,
      rcaspId: String
  )(implicit hc: HeaderCarrier): Future[Either[Result, RemoveOtherAccessViewData]] =
    for {
      rcaspResult <- rcaspConnector.viewRcasp(carfId).value
    } yield rcaspResult match {
      case Right(viewRcaspResponse) =>
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

            val titleKey   = s"removeOtherAccess.title.$suffix"
            val headingKey = s"removeOtherAccess.heading.$suffix"
            val errorKey   = s"removeOtherAccess.error.required.$suffix"

            Right(
              RemoveOtherAccessViewData(
                titleKey = titleKey,
                headingKey = headingKey,
                errorKey = errorKey,
                rcaspName = rcaspName,
                form = formProvider(errorKey)
              )
            )

          case None =>
            logger.warn(
              s"[RemoveOtherAccessController][buildViewData] Could not find selected RCASP for rcaspId=$rcaspId"
            )
            Left(journeyRecovery)
        }

      case Left(_) =>
        logger.warn("[RemoveOtherAccessController][buildViewData] Failed to retrieve RCASP details")
        Left(journeyRecovery)
    }

  def onPageLoad(mode: Mode, rcaspId: String): Action[AnyContent] =
    (identify() andThen getData() andThen requireData).async { implicit request =>
      buildViewData(request.carfId, rcaspId).map {
        case Right(data) =>
          val preparedForm =
            request.userAnswers.get(RemoveOtherAccessPage).fold(data.form)(data.form.fill)

          Ok(
            view(
              preparedForm,
              mode,
              rcaspId,
              data.titleKey,
              data.headingKey,
              data.errorKey,
              data.rcaspName
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
                      data.rcaspName
                    )
                  )
                ),
              value =>
                for {
                  updatedAnswers <- Future.fromTry(request.userAnswers.set(RemoveOtherAccessPage, value))
                  _              <- sessionRepository.set(updatedAnswers)
                } yield Redirect(
                  controllers.routes.PlaceholderController.onPageLoad("Should nav to /remove/remove-rcasp (CARF-549)")
                )
            )

        case Left(recovery) =>
          Future.successful(recovery)
      }
    }
}
