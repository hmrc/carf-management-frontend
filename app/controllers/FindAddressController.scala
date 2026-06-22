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
import models.countries.CountryUk
import models.requests.DataRequest
import models.{AddressAndUPRN, AddressUk, FindAddress, Mode, UserAnswers}
import navigation.Navigator
import pages.individual.IndividualNamePage
import pages.organisation.OverwritableOrganisationName
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
    mode => controllers.routes.PlaceholderController.onPageLoad("Should nav to /address (CARF-203)").url

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify() andThen getData() andThen requireData) {
    implicit request =>

      lazy val preparedForm = request.userAnswers.get(FindAddressPage).fold(form)(form.fill)

      retrieveRcaspName(request.userAnswers) match {
        case Some(name) => Ok(view(preparedForm, mode, name, manualLink(mode)))
        case None       =>
          logger.warn(
            "[FindAddressController] Could not retrieve IndividualNamePage and/or OverwritableOrganisationName onPageLoad"
          )
          Redirect(controllers.routes.InformationMissingController.onPageLoad())
      }

  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify() andThen getData() andThen requireData).async {
    implicit request =>
      val formReturned = form.bindFromRequest()
      formReturned
        .fold(
          formWithErrors =>
            retrieveRcaspName(request.userAnswers) match {
              case Some(name) =>
                Future.successful(BadRequest(view(formWithErrors, mode, name, manualLink(mode))))
              case None       =>
                logger.warn(
                  "[FindAddressController] Could not retrieve IndividualNamePage and/or OverwritableOrganisationName onSubmit"
                )
                Future.successful(Redirect(controllers.routes.InformationMissingController.onPageLoad()))
            },
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
                  retrieveRcaspName(request.userAnswers) match {
                    case Some(name) =>
                      Future.successful(BadRequest(view(formError, mode, name, manualLink(mode))))
                    case None       =>
                      logger.warn(
                        "[FindAddressController] Could not retrieve IndividualNamePage and/or OverwritableOrganisationName onSubmit"
                      )
                      Future.successful(Redirect(controllers.routes.InformationMissingController.onPageLoad()))
                  }
                case Right((addressesAndUPRNs, additionalCallMade)) =>
                  for {
                    updatedAnswersWithFlag <- save(value, addressesAndUPRNs, additionalCallMade)
                  } yield Redirect(navigator.nextPage(FindAddressPage, mode, updatedAnswersWithFlag))
              }
        )
  }

  private def save(
      findAddress: FindAddress,
      addressesAndUPRNs: Seq[AddressAndUPRN],
      additionalCallMade: Boolean
  )(implicit
      request: DataRequest[AnyContent]
  ) =
    for {
      updatedAnswers            <- Future.fromTry(request.userAnswers.set(FindAddressPage, findAddress))
      (filledAddress, maybeUPRN) =
        addressesAndUPRNs.headOption.fold(
          (AddressUk("", None, None, "", findAddress.postcode, CountryUk("", "")), Option.empty[Long])
        )(addressAndUPRN => (addressAndUPRN.address, Some(addressAndUPRN.UPRN)))

      updatedAnswersWithPrePop <-
        Future.fromTry(updatedAnswers.set(AddressPagePrePop, filledAddress))

      updatedAnswersWithAddress <- Future.fromTry(
                                     updatedAnswersWithPrePop.set(
                                       AddressLookupPage,
                                       addressesAndUPRNs
                                     )
                                   )
      updatedAnswersWithFlag    <- Future.fromTry(
                                     updatedAnswersWithAddress.set(FindAddressAdditionalCallUa, additionalCallMade)
                                   )
      resultingUserAnswer       <- maybeUPRN.fold(Future.successful(updatedAnswersWithFlag)) { uprn =>
                                     Future.fromTry(updatedAnswersWithFlag.set(AddressUPRNUserAnswers, uprn))
                                   }
      _                         <- sessionRepository.set(resultingUserAnswer)
    } yield resultingUserAnswer

  private def retrieveRcaspName(userAnswers: UserAnswers): Option[String] =
    (userAnswers.get(IndividualNamePage), userAnswers.get(OverwritableOrganisationName)) match {
      case (Some(indNamePage), None) => Some(indNamePage.fullName)
      case (None, Some(orgNamePage)) => Some(orgNamePage)
      case _                         => None
    }
}
