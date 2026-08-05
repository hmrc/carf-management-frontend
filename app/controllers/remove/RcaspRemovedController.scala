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

import config.Constants.ukZoneId
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import pages.SubmissionSucceededPage
import pages.remove.{RcaspRemovedDateTimePage, RemoveRcaspCachedDetails}
import utils.LoggerUtil.*
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.DateTimeFormats
import views.html.remove.RcaspRemovedView

import javax.inject.Inject

class RcaspRemovedController @Inject() (
    override val messagesApi: MessagesApi,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    val controllerComponents: MessagesControllerComponents,
    view: RcaspRemovedView
) extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (identify() andThen getData() andThen requireData) { implicit request =>
    val submissionSucceeded = request.userAnswers.get(SubmissionSucceededPage).contains(true)

    val cachedDetails = request.userAnswers.get(RemoveRcaspCachedDetails)

    val rcaspRemovedInstant = request.userAnswers.get(RcaspRemovedDateTimePage)

    (cachedDetails, rcaspRemovedInstant) match {
      case (Some(details), Some(instant)) if submissionSucceeded =>
        val datetime = instant.atZone(ukZoneId)
        val date     = datetime.toLocalDate
        val time     = datetime.toLocalTime

        Ok(
          view(
            rcaspName = details.getName,
            rcaspId = details.RCASPID,
            formattedDate = DateTimeFormats.formatDate(date),
            formattedTime = DateTimeFormats.formatTime(time)
          )
        )

      case _ =>
        logWarn(
          "[RcaspRemovedController][onPageLoad] Missing cached RCASP details, removal datetime, or submission flag not set"
        )
        Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
    }
  }
}
