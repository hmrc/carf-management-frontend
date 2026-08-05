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

import config.Constants.noneOfTheseValue
import controllers.actions.*
import forms.ChooseAddressFormProvider
import models.requests.DataRequest
import models.{format, AddressAndUPRN, AddressUk, FindAddress, Mode, UserAnswers}
import navigation.Navigator
import pages.*
import utils.LoggerUtil.*
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.*
import repositories.SessionRepository
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.ChooseAddressView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class ChooseAddressController @Inject() (
    override val messagesApi: MessagesApi,
    sessionRepository: SessionRepository,
    navigator: Navigator,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    submissionLock: SubmissionLockAction,
    formProvider: ChooseAddressFormProvider,
    val controllerComponents: MessagesControllerComponents,
    view: ChooseAddressView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private case class WithRadiosResult(result: Result, addresses: Seq[AddressAndUPRN])

  val form: Form[String] = formProvider()

  private lazy val addressControllerRedirect: Mode => Result = mode =>
    Redirect(controllers.routes.AddressController.onPageLoad(mode))

  private def additionalLine(property: String, postcode: String)(implicit request: DataRequest[AnyContent]): String = {
    val messages: Messages = implicitly[Messages]
    messages("chooseAddress.showing.results", property, postcode)
  }

  private def generateHtml(
      maybeFindAddress: Option[FindAddress]
  )(implicit request: DataRequest[AnyContent]): Option[String] =
    maybeFindAddress.map { findAddress =>
      s"""${additionalLine(
          findAddress.propertyNameOrNumber.getOrElse(""),
          findAddress.postcode
        )}"""
    }

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify() andThen getData() andThen submissionLock andThen requireData) { implicit request =>
      lazy val preparedForm: Form[String] = request.userAnswers.get(ChooseAddressPage).fold(form)(form.fill)

      val WithRadiosResult(result, _) = resultWithRadios(mode) { (radios, maybeFindAddress) =>
        request.userAnswers.retrieveRcaspName match {
          case Some(name) => Ok(view(preparedForm, mode, radios, generateHtml(maybeFindAddress), name))
          case None       =>
            logWarn(
              "[ChooseAddressController][onPageLoad] Could not retrieve IndividualNamePage and/or OverwritableOrganisationName"
            )
            Redirect(controllers.routes.InformationMissingController.onPageLoad())
        }
      }

      result
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify() andThen getData() andThen submissionLock andThen requireData).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            val WithRadiosResult(result, _) = resultWithRadios(mode) { (radios, maybeFindAddress) =>
              request.userAnswers.retrieveRcaspName match {
                case Some(name) => BadRequest(view(formWithErrors, mode, radios, generateHtml(maybeFindAddress), name))
                case None       =>
                  logWarn(
                    "[ChooseAddressController][onSubmit] Could not retrieve IndividualNamePage and/or OverwritableOrganisationName"
                  )
                  Redirect(controllers.routes.InformationMissingController.onPageLoad())
              }
            }
            Future.successful(result)
          },
          value =>
            for {
              updatedAnswers                <- Future.fromTry(request.userAnswers.set(ChooseAddressPage, value))
              addressToStoreMaybe           <- findAddressToStore(mode, value)
              updatedAnswersAsAddressMaybe  <-
                addressToStoreMaybe.fold(Future.successful(updatedAnswers)) { addressToStore =>
                  storeAddress(addressToStore, updatedAnswers)
                }
              updatedAnswersWithEmptyPrePop <-
                Future.fromTry(updatedAnswersAsAddressMaybe.remove(AddressPagePrePop))
              _                             <- sessionRepository.set(updatedAnswersWithEmptyPrePop)
            } yield Redirect(navigator.nextPage(ChooseAddressPage, mode, updatedAnswersWithEmptyPrePop))
        )
    }

  private def storeAddress(addressToStore: AddressAndUPRN, userAnswer: UserAnswers): Future[UserAnswers] =
    for {
      a                  <- Future.fromTry(userAnswer.set(SelectedChooseAddressPage, addressToStore.address))
      b                  <- Future.fromTry(a.set(UkAddressInUserAnswers, addressToStore.address))
      updatedUserAnswers <- Future.fromTry(b.set(AddressUPRNUserAnswers, addressToStore.UPRN))
    } yield updatedUserAnswers

  private def findAddressToStore(mode: Mode, value: String)(implicit
      request: DataRequest[AnyContent]
  ): Future[Option[AddressAndUPRN]] = Future.fromTry {
    val WithRadiosResult(_, addresses) = resultWithRadios(mode) { (_, _) =>
      Redirect(call = controllers.routes.JourneyRecoveryController.onPageLoad())
    }

    val exception = new Exception("Failed to find address")
    addresses
      .find(_.address.format == value)
      .fold {
        if (value == noneOfTheseValue) {
          Success(None)
        } else {
          Failure[Option[AddressAndUPRN]](exception)
        }
      }(address => Success(Some(address)))
  }

  private def createAddressRadios(addresses: => Seq[AddressUk]): Seq[RadioItem] =
    addresses.map { address =>
      val addressFormatted = address.format
      RadioItem(content = Text(s"$addressFormatted"), value = Some(s"$addressFormatted"))
    }

  private def resultWithRadios(
      mode: Mode
  )(
      result: (Seq[RadioItem], Option[FindAddress]) => Result
  )(implicit request: DataRequest[AnyContent]): WithRadiosResult =
    request.userAnswers
      .get(AddressLookupResult)
      .fold {
        WithRadiosResult(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()), Seq.empty)
      } { addressesAndUPRN =>
        if (addressesAndUPRN.isEmpty) {
          WithRadiosResult(addressControllerRedirect(mode), Seq.empty)
        } else {
          val addresses                   = addressesAndUPRN.map(_.address)
          lazy val radios: Seq[RadioItem] = createAddressRadios(addresses)

          val maybeWithRadiosResult = for {
            findAddress        <- request.userAnswers.get(FindAddressPage)
            additionalCallMade <- request.userAnswers.get(FindAddressAdditionalCallUa)
          } yield WithRadiosResult(
            result = result(
              radios,
              if (additionalCallMade) Some(findAddress) else None
            ),
            addresses = addressesAndUPRN
          )

          maybeWithRadiosResult.fold(
            WithRadiosResult(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()), Seq.empty)
          )(identity)
        }
      }

}
