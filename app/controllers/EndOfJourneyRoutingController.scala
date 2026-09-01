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
import pages.changeDetails.ChangeRcaspCachedDetails
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject

class EndOfJourneyRoutingController @Inject() (
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    submissionLock: SubmissionLockAction,
    requireData: DataRequiredAction,
    val controllerComponents: MessagesControllerComponents
) extends FrontendBaseController {

  def onPageLoad(): Action[AnyContent] =
    (identify andThen getData() andThen submissionLock andThen requireData) { implicit request =>
      val userAnswers = request.userAnswers

      userAnswers
        .get(ChangeRcaspCachedDetails)
        .fold {
          if (userAnswers.rcaspIsRegisteredBusiness)
            Redirect(controllers.organisation.routes.RegisteredBusinessCheckDetailsController.onPageLoad)
          else Redirect(controllers.routes.CheckDetailsController.onPageLoad)
        } { cachedDetails =>
          Redirect(controllers.changeDetails.routes.ChangeDetailsRoutingController.onPageLoad(cachedDetails.RCASPID))
        }
    }
}
