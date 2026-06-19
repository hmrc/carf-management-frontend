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

import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import models.OrganisationOrIndividual.*
import pages.combined.OrganisationOrIndividualPage
import pages.individual.IndividualNamePage
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.CheckDetailsHelper
import views.html.CheckDetailsView

import javax.inject.Inject

class CheckDetailsController @Inject() (
    override val messagesApi: MessagesApi,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    view: CheckDetailsView,
    val controllerComponents: MessagesControllerComponents,
    helper: CheckDetailsHelper
) extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad: Action[AnyContent] = (identify() andThen getData() andThen requireData) { implicit request =>
    import cats.syntax.all.*
    val userAnswers          = request.userAnswers
    lazy val ifEmptyProtocol =
      Redirect(controllers.routes.PlaceholderController.onPageLoad("[CARF-293] Some Information is missing page"))

    userAnswers
      .get(OrganisationOrIndividualPage)
      .fold {
        logger.warn(
          s"[CheckYourAnswersController] Error! Could not load page OrganisationOrIndividualPage needed"
        )
        ifEmptyProtocol
      } {
        case Individual   =>
          (
            userAnswers.get(IndividualNamePage),
            helper.getIndividualSectionMaybe(userAnswers),
            helper.getContactDetails(userAnswers)
          )
            .mapN { (name, individualSection, contactDetailsSection) =>
              Ok(view(Seq(individualSection, contactDetailsSection), name.fullName))
            }
            .getOrElse {
              logger.warn(
                s"[CheckYourAnswersController] Error! Could not load page missing answers"
              )
              ifEmptyProtocol
            }
        case Organisation => Ok(view(Seq.empty, "Organisation Name")) // TODO [CARF-295] - Replace with real org name
      }
  }

  def onSubmit: Action[AnyContent] = (identify() andThen getData() andThen requireData) { implicit request =>
    Redirect(controllers.routes.PlaceholderController.onPageLoad("[CARF-296] RCASP added page - /rcasp-added"))
  }
}
