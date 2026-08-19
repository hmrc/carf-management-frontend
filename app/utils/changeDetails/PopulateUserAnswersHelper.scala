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

package utils.changeDetails

import cats.syntax.all.*
import models.OrganisationOrIndividual.{Individual, Organisation}
import models.individual.IndividualName
import models.viewAndUpdateRcasp.{IndividualRcaspDetails, OrganisationRcaspDetails}
import models.{CachedBusinessDetails, RcaspContactDetails, UniqueTaxpayerReference, UserAnswers}
import pages.changeDetails.ChangeRcaspCachedDetails
import pages.combined.OrganisationOrIndividualPage
import pages.individual.*
import pages.organisation.*
import pages.{AddressPagePrePop, UkAddressInUserAnswers}
import utils.LoggerUtil.*
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import repositories.SessionRepository
import services.RegistrationService
import uk.gov.hmrc.http.HeaderCarrier
import utils.CountryListFactory

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

class PopulateUserAnswersHelper @Inject() (
    sessionRepository: SessionRepository,
    registrationService: RegistrationService,
    countryListFactory: CountryListFactory
)(implicit ec: ExecutionContext) {

  def populateUserAnswersForIndividual(
      userId: String,
      individualRcaspDetails: IndividualRcaspDetails
  ): Future[Result] = {
    val emptyUserAnswers    = UserAnswers(id = userId, rcaspIsRegisteredBusiness = false)
    val individualName      = IndividualName(individualRcaspDetails.FirstName, individualRcaspDetails.LastName)
    val maybeNino           = individualRcaspDetails.TINDetails.flatMap(_.headOption.map(_.TIN))
    val maybeAddressUk      = individualRcaspDetails.AddressDetails.toAddressUk
    val maybeContactDetails = individualRcaspDetails.PrimaryContactDetails

    (maybeNino, maybeAddressUk, maybeContactDetails)
      .mapN { (nino, address, contactDetails) =>
        for {
          a              <- Future.fromTry(emptyUserAnswers.set(ReportForRegisteredBusinessPage, false))
          b              <- Future.fromTry(a.set(OrganisationOrIndividualPage, Individual))
          c              <- Future.fromTry(b.set(IndividualNamePage, individualName))
          d              <- Future.fromTry(c.set(NiNumberPage, nino))
          e              <- Future.fromTry(d.set(UkAddressInUserAnswers, address))
          f              <- Future.fromTry(e.set(AddressPagePrePop, address))
          g              <- Future.fromTry(f.set(IndividualEmailPage, contactDetails.EmailAddress))
          h              <- Future.fromTry(g.set(IndividualHavePhonePage, contactDetails.PhoneNumber.nonEmpty))
          i              <- Future.fromTry(contactDetails.PhoneNumber.fold(Success(h))(h.set(IndividualPhonePage, _)))
          updatedAnswers <- Future.fromTry(i.set(ChangeRcaspCachedDetails, individualRcaspDetails))
          _              <- sessionRepository.set(updatedAnswers)
        } yield Redirect(
          controllers.changeDetails.routes.ChangeDetailsController.onPageLoad(individualRcaspDetails.RCASPID)
        )
      }
      .getOrElse {
        logWarn(
          "[PopulateUserAnswersHelper][populateUserAnswersForIndividual] Unable to populate user answers from IndividualRcaspDetails"
        )
        Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }
  }

  def populateUserAnswersForOrganisation(
      userId: String,
      organisationRcaspDetails: OrganisationRcaspDetails
  ): Future[Result] = {
    val emptyUserAnswers         = UserAnswers(id = userId, rcaspIsRegisteredBusiness = false)
    val haveTradingName          = organisationRcaspDetails.TradingName != organisationRcaspDetails.RCASPName
    val maybeUtr                 = organisationRcaspDetails.TINDetails.flatMap(_.headOption.map(_.TIN))
    val maybeAddressUk           = organisationRcaspDetails.AddressDetails.toAddressUk
    val maybeFirstContactDetails = organisationRcaspDetails.PrimaryContactDetails

    (maybeUtr, maybeAddressUk, maybeFirstContactDetails)
      .mapN { (utr, address, firstContactDetails) =>
        for {
          a              <- Future.fromTry(emptyUserAnswers.set(ReportForRegisteredBusinessPage, false))
          b              <- Future.fromTry(a.set(OrganisationOrIndividualPage, Organisation))
          c              <- Future.fromTry(b.set(OrganisationNamePage, organisationRcaspDetails.RCASPName))
          d              <- Future.fromTry(c.set(OverwritableOrganisationName, organisationRcaspDetails.RCASPName))
          e              <- Future.fromTry(d.set(HaveTradingNamePage, haveTradingName))
          f              <- Future.fromTry {
                              if haveTradingName then e.set(TradingNamePage, organisationRcaspDetails.TradingName) else Success(e)
                            }
          g              <- Future.fromTry(f.set(UtrPage, utr))
          h              <- Future.fromTry(g.set(UkAddressInUserAnswers, address))
          i              <- Future.fromTry(h.set(AddressPagePrePop, address))
          j              <- Future.fromTry(i.set(OrganisationFirstContactNamePage, firstContactDetails.ContactName))
          k              <- Future.fromTry(j.set(OrganisationFirstContactEmailPage, firstContactDetails.EmailAddress))
          l              <- Future.fromTry(k.set(OrganisationFirstContactHavePhonePage, firstContactDetails.PhoneNumber.nonEmpty))
          m              <- Future.fromTry(
                              firstContactDetails.PhoneNumber.fold(Success(l))(l.set(OrganisationFirstContactPhoneNumberPage, _))
                            )
          n              <- Future.fromTry(
                              m.set(OrganisationHaveSecondContactPage, organisationRcaspDetails.SecondaryContactDetails.nonEmpty)
                            )
          o              <- organisationRcaspDetails.SecondaryContactDetails.fold(Future.successful(n))(
                              addSecondContactDetailsToUserAnswers(_, n)
                            )
          updatedAnswers <- Future.fromTry(o.set(ChangeRcaspCachedDetails, organisationRcaspDetails))
          _              <- sessionRepository.set(updatedAnswers)
        } yield Redirect(
          controllers.changeDetails.routes.ChangeDetailsController.onPageLoad(organisationRcaspDetails.RCASPID)
        )
      }
      .getOrElse {
        logWarn(
          "[PopulateUserAnswersHelper][populateUserAnswersForOrganisation] Unable to populate user answers from OrganisationRcaspDetails"
        )
        Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }
  }

  private def addSecondContactDetailsToUserAnswers(
      secondContactDetails: RcaspContactDetails,
      userAnswers: UserAnswers
  ): Future[UserAnswers] =
    for {
      a              <- Future.fromTry(userAnswers.set(OrganisationSecondContactNamePage, secondContactDetails.ContactName))
      b              <- Future.fromTry(a.set(OrganisationSecondContactEmailPage, secondContactDetails.EmailAddress))
      c              <- Future.fromTry(b.set(OrganisationSecondContactHavePhonePage, secondContactDetails.PhoneNumber.nonEmpty))
      updatedAnswers <-
        Future.fromTry(
          secondContactDetails.PhoneNumber.fold(Success(c))(c.set(OrganisationSecondContactPhoneNumberPage, _))
        )
    } yield updatedAnswers

  def populateUserAnswersForRegisteredBusiness(
      userId: String,
      requestUtr: UniqueTaxpayerReference,
      organisationRcaspDetails: OrganisationRcaspDetails
  )(implicit hc: HeaderCarrier): Future[Result] = {
    val emptyUserAnswers = UserAnswers(id = userId, rcaspIsRegisteredBusiness = true)
    val haveTradingName  = organisationRcaspDetails.TradingName != organisationRcaspDetails.RCASPName
    val maybeCadxUtr     = organisationRcaspDetails.TINDetails.flatMap(_.headOption.map(_.TIN))
    val maybeAddressUk   = organisationRcaspDetails.AddressDetails.toAddressUk

    (maybeCadxUtr, maybeAddressUk)
      .mapN { (_, address) =>
        registrationService.getBusinessWithCtUtr(requestUtr.uniqueTaxPayerReference).value.flatMap {
          case Right(businessDetails) =>
            countryListFactory.getDescriptionFromCode(businessDetails.address.countryCode) match {
              case None =>
                logWarn(
                  s"[PopulateUserAnswersHelper][populateUserAnswersForRegisteredBusiness] Country with code ${businessDetails.address.countryCode} not found in list of countries"
                )
                Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))

              case Some(countryName) =>
                val cachedBusinessDetails = CachedBusinessDetails(
                  name = businessDetails.name,
                  address = businessDetails.address,
                  countryName = countryName
                )
                for {
                  a              <- Future.fromTry(emptyUserAnswers.set(ReportForRegisteredBusinessPage, true))
                  b              <- Future.fromTry(a.set(OrganisationNamePage, organisationRcaspDetails.RCASPName))
                  c              <- Future.fromTry(b.set(OverwritableOrganisationName, organisationRcaspDetails.RCASPName))
                  d              <- Future.fromTry(c.set(HaveTradingNamePage, haveTradingName))
                  e              <- Future.fromTry {
                                      if haveTradingName then d.set(TradingNamePage, organisationRcaspDetails.TradingName)
                                      else Success(d)
                                    }
                  f              <- Future.fromTry(e.set(UkAddressInUserAnswers, address))
                  g              <- Future.fromTry(f.set(AddressPagePrePop, address))
                  h              <- Future.fromTry(g.set(CachedBusinessDetailsPage, cachedBusinessDetails))
                  updatedAnswers <- Future.fromTry(h.set(ChangeRcaspCachedDetails, organisationRcaspDetails))
                  _              <- sessionRepository.set(updatedAnswers)
                } yield Redirect(
                  controllers.changeDetails.routes.RegisteredBusinessChangeDetailsController
                    .onPageLoad(organisationRcaspDetails.RCASPID)
                )
            }
          case Left(error)            =>
            logWarn(
              s"[PopulateUserAnswersHelper][populateUserAnswersForRegisteredBusiness] Failed to get business details: $error"
            )
            Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
        }
      }
      .getOrElse {
        logWarn(
          "[PopulateUserAnswersHelper][populateUserAnswersForRegisteredBusiness] Unable to populate user answers from OrganisationRcaspDetails"
        )
        Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }
  }
}
