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

package controllers.organisation

import config.Constants.ZERO
import controllers.actions.*
import forms.GenericYesNoPageFormProvider
import models.{CachedBusinessDetails, Mode, UserAnswers}
import navigation.Navigator
import pages.changeDetails.ChangeRcaspCachedDetails
import pages.organisation.{CachedBusinessDetailsPage, ReportForRegisteredBusinessPage}
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.{AccountService, RegistrationService}
import types.ResultT
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.CountryListFactory
import views.html.organisation.ReportForRegisteredBusinessView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ReportForRegisteredBusinessController @Inject() (
    override val messagesApi: MessagesApi,
    sessionRepository: SessionRepository,
    navigator: Navigator,
    identify: IdentifierAction,
    ctUtrRetrievalAction: CtUtrRetrievalAction,
    getData: DataRetrievalAction,
    submissionLock: SubmissionLockAction,
    requireData: DataRequiredAction,
    formProvider: GenericYesNoPageFormProvider,
    registrationService: RegistrationService,
    accountService: AccountService,
    countryListFactory: CountryListFactory,
    val controllerComponents: MessagesControllerComponents,
    view: ReportForRegisteredBusinessView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  val form: Form[Boolean] = formProvider("reportForRegisteredBusiness.error.required")

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify() andThen ctUtrRetrievalAction() andThen getData() andThen submissionLock andThen requireData).async {
      implicit request =>
        lazy val preparedForm = request.userAnswers.get(ReportForRegisteredBusinessPage).fold(form)(form.fill)

        (request.utr, request.userAnswers.get(CachedBusinessDetailsPage)) match {
          case (Some(_), Some(cached)) =>
            Future.successful(Ok(view(preparedForm, mode, cached.name)))
          case (Some(utr), None)       =>
            registrationService.getBusinessWithCtUtr(utr.uniqueTaxPayerReference).value.flatMap {
              case Right(businessDetails) =>
                countryListFactory.getDescriptionFromCode(businessDetails.address.countryCode) match {
                  case Some(countryName) =>
                    val cached = CachedBusinessDetails(
                      name = businessDetails.name,
                      address = businessDetails.address,
                      countryName = countryName
                    )

                    for {
                      updatedAnswers <- Future.fromTry(request.userAnswers.set(CachedBusinessDetailsPage, cached))
                      _              <- sessionRepository.set(updatedAnswers)
                    } yield Ok(view(preparedForm, mode, cached.name))

                  case None =>
                    logger.error(
                      s"[ReportForRegisteredBusinessController][onPageLoad] " +
                        s"Country with code ${businessDetails.address.countryCode} not found in list of countries"
                    )
                    Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
                }

              case Left(error) =>
                logger
                  .warn(s"[ReportForRegisteredBusinessController][onPageLoad] Failed to get business details: $error")
                Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
            }

          case (None, _) =>
            logger.warn("[ReportForRegisteredBusinessController][onPageLoad] CT UTR not found in request")
            Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
        }
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify() andThen ctUtrRetrievalAction() andThen getData() andThen submissionLock andThen requireData).async {
      implicit request =>
        form
          .bindFromRequest()
          .fold(
            formWithErrors =>
              request.userAnswers.get(CachedBusinessDetailsPage) match {
                case Some(cached) => Future.successful(BadRequest(view(formWithErrors, mode, cached.name)))
                case None         =>
                  logger.warn("[ReportForRegisteredBusinessController][onSubmit] No cached business details found")
                  Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
              },
            value =>
              setRcaspIsRegisteredBusinessFlag(
                userAnswers = request.userAnswers,
                pageAnswer = value,
                carfId = request.carfId,
                ctUtr = request.utr.map(_.uniqueTaxPayerReference)
              ).value.flatMap {
                case Right(userAnswers) =>
                  for {
                    updatedAnswers <- Future.fromTry(userAnswers.set(ReportForRegisteredBusinessPage, value))
                    _              <- sessionRepository.set(updatedAnswers)
                  } yield Redirect(
                    navigator.nextPage(ReportForRegisteredBusinessPage, mode, updatedAnswers)
                  )
                case Left(error)        =>
                  logger.error(
                    s"[ReportForRegisteredBusinessController][onSubmit] Error getting how many Rcasps user has: $error"
                  )
                  Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
              }
          )
    }

  private def setRcaspIsRegisteredBusinessFlag(
      userAnswers: UserAnswers,
      pageAnswer: Boolean,
      carfId: String,
      ctUtr: Option[String]
  )(implicit hc: HeaderCarrier): ResultT[UserAnswers] =
    userAnswers
      .get(ChangeRcaspCachedDetails)
      .fold {
        accountService.getNumberOfRcaspsCurrentlyAdded(carfId = carfId).map { numberOfRcasps =>
          if (numberOfRcasps == ZERO && pageAnswer && ctUtr.nonEmpty) {
            userAnswers.copy(rcaspIsRegisteredBusiness = true)
          } else {
            userAnswers.copy(rcaspIsRegisteredBusiness = false)
          }
        }
      } { details =>
        if (details.IsRCASPUser && pageAnswer) ResultT.fromValue(userAnswers.copy(rcaspIsRegisteredBusiness = true))
        else ResultT.fromValue(userAnswers.copy(rcaspIsRegisteredBusiness = false))
      }
}
