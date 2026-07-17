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

import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import pages.SubmissionSucceededPage
import pages.remove.RemoveRcaspCachedDetails
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.remove.RcaspRemovedView
import utils.DateTimeFormats

import java.time.{Clock, ZoneId, ZonedDateTime}
import javax.inject.Inject

class RcaspRemovedController @Inject() (
    override val messagesApi: MessagesApi,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    clock: Clock,
    val controllerComponents: MessagesControllerComponents,
    view: RcaspRemovedView
) extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(rcaspId: String): Action[AnyContent] = (identify() andThen getData() andThen requireData) {
    implicit request =>

      val submissionSucceeded = request.userAnswers.get(SubmissionSucceededPage).contains(true)

      val cachedDetails = request.userAnswers
        .get(RemoveRcaspCachedDetails)
        .filter(_.RCASPID.equalsIgnoreCase(rcaspId))

      cachedDetails match {
        case Some(details) if submissionSucceeded =>
          val datetime = ZonedDateTime.now(clock).withZoneSameInstant(ZoneId.of("Europe/London"))
          val date     = datetime.toLocalDate
          val time     = datetime.toLocalTime

          Ok(
            view(
              rcaspName = details.getName,
              rcaspId = rcaspId,
              formattedDate = DateTimeFormats.formatDate(date),
              formattedTime = DateTimeFormats.formatTime(time)
            )
          )

        case _ =>
          logger.warn(
            "[RcaspRemovedController][onPageLoad] Missing cached RCASP details, removal datetime, or submission flag not set"
          )
          Redirect(controllers.routes.PlaceholderController.onPageLoad("/problem/page-unavailable (CARF-536)"))
      }
  }
}
