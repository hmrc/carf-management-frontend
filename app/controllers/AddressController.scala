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
import forms.AddressFormProvider
import models.{AddressUk, Mode, UniqueTaxpayerReference, UserAnswers}
import navigation.Navigator
import pages.{AddressPageForNavigatorOnly, AddressPagePrePop, AddressUPRNUserAnswers, UkAddressInUserAnswers}
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import repositories.SessionRepository
import services.AccountService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.RcaspHelper
import views.html.AddressView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AddressController @Inject() (
    override val messagesApi: MessagesApi,
    sessionRepository: SessionRepository,
    navigator: Navigator,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    submissionLock: SubmissionLockAction,
    requireData: DataRequiredAction,
    formProvider: AddressFormProvider,
    accountService: AccountService,
    val controllerComponents: MessagesControllerComponents,
    view: AddressView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  val form: Form[AddressUk] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify() andThen getData() andThen submissionLock andThen requireData).async { implicit request =>

      lazy val preparedForm = request.userAnswers.get(AddressPagePrePop).fold(form)(form.fill)

      RcaspHelper.retrieveRcaspName(request.userAnswers) match {
        case Some(name) =>
          Future.successful(Ok(view(preparedForm, mode, name)))
        case None       =>
          logger.warn(
            "[AddressController] Could not retrieve IndividualNamePage and/or OverwritableOrganisationName onPageLoad"
          )
          Future.successful(Redirect(controllers.routes.InformationMissingController.onPageLoad()))
      }
    }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify() andThen getData() andThen requireData).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            RcaspHelper
              .retrieveRcaspName(request.userAnswers)
              .fold {
                logger.warn(
                  "[AddressController] Could not retrieve IndividualNamePage and/or OverwritableOrganisationName onSubmit"
                )
                Future.successful(Redirect(controllers.routes.InformationMissingController.onPageLoad()))
              }(name => Future.successful(BadRequest(view(formWithErrors, mode, name)))),
          value =>
            for {
              a      <- Future.fromTry(request.userAnswers.set(UkAddressInUserAnswers, value))
              b      <- Future.fromTry(a.set(AddressPagePrePop, value))
              c      <- Future.fromTry(b.remove(AddressUPRNUserAnswers))
              _      <- sessionRepository.set(c)
              result <-
                accountService
                  .getNumberOfRcaspsCurrentlyAdded(request.carfId)
                  .map(count => Redirect(isRcaspUserRedirect(count, request.utr, c, mode)))
                  .leftMap { error =>
                    logger.warn(s"[AddressController] Error retrieving RCASP count: $error")
                    Redirect(routes.JourneyRecoveryController.onPageLoad())
                  }
                  .merge
            } yield result
        )
  }

  private def isRcaspUserRedirect(
      rcaspCount: Int,
      ctUtr: Option[UniqueTaxpayerReference],
      userAnswers: UserAnswers,
      mode: Mode
  ): Call =
    if (RcaspHelper.isRcaspUser(rcaspCount, ctUtr, userAnswers)) {
      routes.PlaceholderController.onPageLoad("Should nav to /registered-business/check-answers (CARF-294)")
    } else {
      navigator.nextPage(AddressPageForNavigatorOnly, mode, userAnswers)
    }

}
