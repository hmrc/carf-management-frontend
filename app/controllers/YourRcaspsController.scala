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

import connectors.RcaspConnector
import controllers.actions.*
import forms.GenericYesNoPageFormProvider
import models.NormalMode
import utils.LoggerUtil.*
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.YourRcaspsListWithActionsHelper
import views.html.YourRcaspsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class YourRcaspsController @Inject() (
    override val messagesApi: MessagesApi,
    identify: IdentifierAction,
    formProvider: GenericYesNoPageFormProvider,
    val controllerComponents: MessagesControllerComponents,
    rcaspConnector: RcaspConnector,
    view: YourRcaspsView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form: Form[Boolean] = formProvider("yourRcasps.error.required")

  def onPageLoad(): Action[AnyContent] = identify.async { implicit request =>
    rcaspConnector.viewRcasp(request.carfId).value.map {
      case Left(error)      =>
        logWarn(s"[YourRcaspsController][onPageLoad] Error! Could not view rcasps: $error")
        Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      case Right(rcaspList) =>
        val listWithActions = YourRcaspsListWithActionsHelper.getYourRcaspsRows(rcaspList)
        Ok(view(form, listWithActions))
    }
  }

  def onSubmit(): Action[AnyContent] = identify.async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          rcaspConnector.viewRcasp(request.carfId).value.map {
            case Left(error)      =>
              logWarn(s"[YourRcaspsController][onSubmit] Error! Could not view rcasps: $error")
              Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
            case Right(rcaspList) =>
              val listWithActions = YourRcaspsListWithActionsHelper.getYourRcaspsRows(rcaspList)
              BadRequest(view(formWithErrors, listWithActions))
          },
        value =>
          val redirectCall: Call = if (value) {
            controllers.routes.RoutingController.onPageLoad(NormalMode)
          } else {
            controllers.home.routes.HomePageController.onPageLoad()
          }

          Future.successful(Redirect(redirectCall))
      )
  }
}
