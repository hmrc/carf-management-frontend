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
import models.requests.DataRequest
import models.viewAndUpdateRcasp.RcaspDetails
import models.{CachedBusinessDetails, ChangeMode, Mode, NormalMode, UserAnswers}
import navigation.Navigator
import pages.changeDetails.ChangeRcaspCachedDetails
import pages.organisation.{CachedBusinessDetailsPage, ReportForRegisteredBusinessPage}
import utils.LoggerUtil.*
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents, Result}
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
    with I18nSupport {

  lazy val form: Boolean => Form[Boolean] = isChange =>
    formProvider(
      if (isChange) {
        "reportForRegisteredBusiness.changeDetails.error.required"
      } else {
        "reportForRegisteredBusiness.error.required"
      }
    )

  private lazy val recovery: Call = controllers.routes.JourneyRecoveryController.onPageLoad()

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify() andThen ctUtrRetrievalAction() andThen getData() andThen submissionLock andThen requireData).async {
      implicit request =>
        request.userAnswers.get(ChangeRcaspCachedDetails).fold(onPageLoadAddVersion(mode)) { details =>
          Future.successful(onPageLoadChangeDetailsVersion(mode, details.IsRCASPUser))
        }
    }

  private def onPageLoadAddVersion(mode: Mode)(implicit request: DataRequest[AnyContent]): Future[Result] =
    request.utr match {
      case Some(utr) =>
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
                  updatedAnswers <- Future.fromTry(
                                      request.userAnswers.set(CachedBusinessDetailsPage, cached)
                                    )
                  _              <- sessionRepository.set(updatedAnswers)
                } yield {
                  val preparedForm =
                    updatedAnswers.get(ReportForRegisteredBusinessPage).fold(form(false))(form(false).fill)

                  Ok(view(preparedForm, mode, Some(cached.name), false))
                }

              case None =>
                logError(
                  "[ReportForRegisteredBusinessController][onPageLoad] " +
                    s"Country with code ${businessDetails.address.countryCode} not found in list of countries"
                )
                Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
            }
          case Left(error)            =>
            logWarn(s"[ReportForRegisteredBusinessController][onPageLoad][Add] Failed to get business details: $error")
            Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
        }

      case None =>
        logWarn("[ReportForRegisteredBusinessController][onPageLoad][Add] CT UTR not found in request")
        Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
    }

  private def onPageLoadChangeDetailsVersion(mode: Mode, isRcaspUser: Boolean)(implicit
      request: DataRequest[AnyContent]
  ): Result =
    request.utr match {
      case Some(utr) =>
        val isChangeJourney = true
        if (isRcaspUser) {
          request.userAnswers
            .get(ReportForRegisteredBusinessPage)
            .fold(Redirect(recovery))(value => Ok(view(form(isChangeJourney).fill(value), mode, None, isChangeJourney)))
        } else {
          logWarn(
            "[ReportForRegisteredBusinessController][onPageLoad][Change] Failed verifications as user is not a " +
              "registered business"
          )
          Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
        }
      case None      =>
        logWarn("[ReportForRegisteredBusinessController][onPageLoad][Change] CT UTR not found in request")
        Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify() andThen ctUtrRetrievalAction() andThen getData() andThen submissionLock andThen requireData).async {
      implicit request =>
        val userAnswers                              = request.userAnswers
        lazy val hasValueChanged: Boolean => Boolean =
          newValue => !userAnswers.get(ReportForRegisteredBusinessPage).contains(newValue)

        val isCachedDetailsPresent: Boolean = userAnswers.get(ChangeRcaspCachedDetails).nonEmpty

        form(isCachedDetailsPresent)
          .bindFromRequest()
          .fold(
            formWithErrors =>
              userAnswers.get(CachedBusinessDetailsPage) match {
                case Some(cached) =>
                  Future.successful(
                    BadRequest(view(formWithErrors, mode, Some(cached.name), true))
                  )
                case None         =>
                  Future.successful(BadRequest(view(formWithErrors, mode, None, false)))
              },
            value =>
              userAnswers.get(ChangeRcaspCachedDetails).fold(onSubmitAddVersion(mode, value)) { details =>
                onSubmitChangeVersion(mode, value, details, userAnswers, hasValueChanged(value))
              }
          )
    }

  private def onSubmitAddVersion(mode: Mode, value: Boolean)(implicit request: DataRequest[AnyContent]) =
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
        logError(
          s"[ReportForRegisteredBusinessController][onSubmit] Error getting how many Rcasps user has: $error"
        )
        Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
    }

  private def onSubmitChangeVersion(
      mode: Mode,
      value: Boolean,
      details: RcaspDetails,
      userAnswers: UserAnswers,
      hasValueChanged: Boolean
  ): Future[Result] =
    if (hasValueChanged) {
      ifChanged(value, details, userAnswers)
    } else Future.successful(ifUnchanged(value, details, mode))

  private def setRcaspIsRegisteredBusinessFlag(
      userAnswers: UserAnswers,
      pageAnswer: Boolean,
      carfId: String,
      ctUtr: Option[String]
  )(implicit hc: HeaderCarrier): ResultT[UserAnswers] =
    accountService.getNumberOfRcaspsCurrentlyAdded(carfId = carfId).map { numberOfRcasps =>
      if (numberOfRcasps == ZERO && pageAnswer && ctUtr.nonEmpty) {
        userAnswers.copy(rcaspIsRegisteredBusiness = true)
      } else {
        userAnswers.copy(rcaspIsRegisteredBusiness = false)
      }
    }

  private def ifChanged(newValue: Boolean, details: RcaspDetails, userAnswers: UserAnswers): Future[Result] = {

    def saveUserAnswersAndRedirect(call: Call) =
      for {
        a <- Future.fromTry(userAnswers.set(ReportForRegisteredBusinessPage, newValue))
        b  = a.copy(rcaspIsRegisteredBusiness = newValue)
        _ <- sessionRepository.set(b)
      } yield Redirect(call)

    if (newValue) {
      if (details.IsRCASPUser) {
        saveUserAnswersAndRedirect(
          controllers.organisation.routes.RegisteredBusinessIsThisYourBusinessNameController.onPageLoad(NormalMode)
        )
      } else {
        logWarn(
          "[ReportForRegisteredBusinessController][Change Journey] Cannot change RCASP to isRCASPUser to" +
            "true if previously false in API"
        )
        Future.successful(Redirect(recovery))
      }
    } else {
      saveUserAnswersAndRedirect(
        controllers.combined.routes.OrganisationOrIndividualController.onPageLoad(NormalMode)
      )
    }
  }

  private def ifUnchanged(newValue: Boolean, details: RcaspDetails, mode: Mode): Result =
    Redirect {
      mode match {
        case NormalMode =>
          if (newValue) {
            controllers.organisation.routes.RegisteredBusinessIsThisYourBusinessNameController.onPageLoad(NormalMode)
          } else {
            controllers.combined.routes.OrganisationOrIndividualController.onPageLoad(NormalMode)
          }
        case ChangeMode =>
          if (newValue) {
            controllers.changeDetails.routes.RegisteredBusinessChangeDetailsController.onPageLoad(details.RCASPID)
          } else {
            controllers.changeDetails.routes.ChangeDetailsController.onPageLoad(details.RCASPID)
          }
      }
    }
}
