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
import forms.FindAddressFormProvider
import models.requests.DataRequest
import models.{AddressAndUPRN, AddressUk, FindAddress, Mode}
import navigation.Navigator
import pages.*
import play.api.Logging
import play.api.data.{Form, FormError}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.AddressLookupService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.FindAddressView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FindAddressController @Inject() (
    override val messagesApi: MessagesApi,
    sessionRepository: SessionRepository,
    navigator: Navigator,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    submissionLock: SubmissionLockAction,
    formProvider: FindAddressFormProvider,
    addressLookupService: AddressLookupService,
    val controllerComponents: MessagesControllerComponents,
    view: FindAddressView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  val form: Form[FindAddress] = formProvider()

  lazy val manualLink: Mode => String =
    mode => controllers.routes.AddressController.onPageLoad(mode).url

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify() andThen getData() andThen submissionLock andThen requireData) { implicit request =>

      lazy val preparedForm = request.userAnswers.get(FindAddressPage).fold(form)(form.fill)

      request.userAnswers.retrieveRcaspName match {
        case Some(name) => Ok(view(preparedForm, mode, name, manualLink(mode)))
        case None       =>
          logger.warn(
            "[FindAddressController] Could not retrieve IndividualNamePage and/or OverwritableOrganisationName onPageLoad"
          )
          Redirect(controllers.routes.InformationMissingController.onPageLoad())
      }

    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify() andThen getData() andThen submissionLock andThen requireData).async { implicit request =>
      val formReturned = form.bindFromRequest()
      formReturned
        .fold(
          formWithErrors =>
            request.userAnswers.retrieveRcaspName
              .fold {
                logger.warn(
                  "[FindAddressController][formWithErrors] Could not retrieve IndividualNamePage and/or OverwritableOrganisationName onSubmit"
                )
                Future.successful(Redirect(controllers.routes.InformationMissingController.onPageLoad()))
              }(name => Future.successful(BadRequest(view(formWithErrors, mode, name, manualLink(mode))))),
          value =>
            addressLookupService
              .postcodeSearch(value.postcode, value.propertyNameOrNumber)
              .value
              .flatMap {
                case Left(error)                                    =>
                  logger.error(s"Address lookup service failed: $error")
                  Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
                case Right((Nil, _))                                =>
                  val formError =
                    formReturned.withError(FormError("postcode", List("findAddress.postcode.error.notFound")))
                  request.userAnswers.retrieveRcaspName.fold {
                    logger.warn(
                      "[FindAddressController][form success] Could not retrieve IndividualNamePage and/or OverwritableOrganisationName onSubmit"
                    )
                    Future.successful(Redirect(controllers.routes.InformationMissingController.onPageLoad()))
                  }(name => Future.successful(BadRequest(view(formError, mode, name, manualLink(mode)))))
                case Right((addressesAndUPRNs, additionalCallMade)) =>
                  for {
                    updatedAnswers <-
                      if (addressesAndUPRNs.length == 1) { saveSingleAddress(value, addressesAndUPRNs.head) }
                      else { saveMultipleAddresses(value, addressesAndUPRNs, additionalCallMade) }
                  } yield Redirect(navigator.nextPage(FindAddressPage, mode, updatedAnswers))
              }
        )
    }

  private def saveMultipleAddresses(
      findAddress: FindAddress,
      addressesAndUPRNs: Seq[AddressAndUPRN],
      additionalCallMade: Boolean
  )(implicit request: DataRequest[AnyContent]) =
    for {
      uaWithSingleAddressDataCleared <-
        Future.fromTry(request.userAnswers.remove(List(AddressUPRNUserAnswers, AddressPagePrePop)))
      uaWithPageAnswer               <-
        Future.fromTry(uaWithSingleAddressDataCleared.set(FindAddressPage, findAddress))
      uaWithAddresses                <-
        Future.fromTry(uaWithPageAnswer.set(AddressLookupResult, addressesAndUPRNs))
      uaWithAdditionalCallFlag       <-
        Future.fromTry(uaWithAddresses.set(FindAddressAdditionalCallUa, additionalCallMade))
      _                              <- sessionRepository.set(uaWithAdditionalCallFlag)
    } yield uaWithAdditionalCallFlag

  private def saveSingleAddress(
      findAddress: FindAddress,
      addressAndUPRN: AddressAndUPRN
  )(implicit request: DataRequest[AnyContent]) =
    for {
      uaWithMultipleAddressDataCleared <-
        Future.fromTry(request.userAnswers.remove(List(FindAddressAdditionalCallUa, AddressLookupResult)))
      uaWithPageAnswer                 <-
        Future.fromTry(uaWithMultipleAddressDataCleared.set(FindAddressPage, findAddress))
      uaWithPrePop                     <-
        Future.fromTry(uaWithPageAnswer.set(AddressPagePrePop, addressAndUPRN.address))
      uaWithUprn                       <-
        Future.fromTry(uaWithPrePop.set(AddressUPRNUserAnswers, addressAndUPRN.UPRN))
      _                                <- sessionRepository.set(uaWithUprn)
    } yield uaWithUprn

}
