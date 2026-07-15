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
import models.{CachedBusinessDetails, RcaspContactDetails, UserAnswers}
import pages.UkAddressInUserAnswers
import pages.changeDetails.ChangeRcaspCachedDetails
import pages.combined.OrganisationOrIndividualPage
import pages.individual.*
import pages.organisation.*
import play.api.Logging
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import repositories.SessionRepository
import utils.CountryListFactory

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

class PopulateUserAnswersHelper @Inject() (
    sessionRepository: SessionRepository,
    countryListFactory: CountryListFactory
)(implicit ec: ExecutionContext)
    extends Logging {

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
          f              <- Future.fromTry(e.set(IndividualEmailPage, contactDetails.EmailAddress))
          g              <- Future.fromTry(f.set(IndividualHavePhonePage, contactDetails.PhoneNumber.nonEmpty))
          h              <- Future.fromTry(contactDetails.PhoneNumber.fold(Success(g))(g.set(IndividualPhonePage, _)))
          updatedAnswers <- Future.fromTry(h.set(ChangeRcaspCachedDetails, individualRcaspDetails))
          _              <- sessionRepository.set(updatedAnswers)
        } yield Redirect(
          controllers.changeDetails.routes.ChangeDetailsController.onPageLoad(individualRcaspDetails.RCASPID)
        )
      }
      .getOrElse {
        logger.warn(
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
          i              <- Future.fromTry(h.set(OrganisationFirstContactNamePage, firstContactDetails.ContactName))
          j              <- Future.fromTry(i.set(OrganisationFirstContactEmailPage, firstContactDetails.EmailAddress))
          k              <- Future.fromTry(j.set(OrganisationFirstContactHavePhonePage, firstContactDetails.PhoneNumber.nonEmpty))
          l              <- Future.fromTry(
                              firstContactDetails.PhoneNumber.fold(Success(k))(k.set(OrganisationFirstContactPhoneNumberPage, _))
                            )
          m              <- Future.fromTry(
                              l.set(OrganisationHaveSecondContactPage, organisationRcaspDetails.SecondaryContactDetails.nonEmpty)
                            )
          n              <- organisationRcaspDetails.SecondaryContactDetails.fold(Future.successful(m))(
                              addSecondContactDetailsToUserAnswers(_, m)
                            )
          updatedAnswers <- Future.fromTry(n.set(ChangeRcaspCachedDetails, organisationRcaspDetails))
          _              <- sessionRepository.set(updatedAnswers)
        } yield Redirect(
          controllers.changeDetails.routes.ChangeDetailsController.onPageLoad(organisationRcaspDetails.RCASPID)
        )
      }
      .getOrElse {
        logger.warn(
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
      organisationRcaspDetails: OrganisationRcaspDetails
  ): Future[Result] = {
    val emptyUserAnswers           = UserAnswers(id = userId, rcaspIsRegisteredBusiness = true)
    val haveTradingName            = organisationRcaspDetails.TradingName != organisationRcaspDetails.RCASPName
    val maybeUtr                   = organisationRcaspDetails.TINDetails.flatMap(_.headOption.map(_.TIN))
    val maybeCachedBusinessDetails =
      countryListFactory.getDescriptionFromCode(organisationRcaspDetails.AddressDetails.CountryCode).map { country =>
        CachedBusinessDetails(
          name = organisationRcaspDetails.RCASPName,
          address = organisationRcaspDetails.AddressDetails.toAddressRegistrationResponse,
          countryName = country
        )
      }

    (maybeUtr, maybeCachedBusinessDetails)
      .mapN { (utr, cachedBusinessDetails) =>
        for {
          a              <-
            Future.fromTry(emptyUserAnswers.set(ReportForRegisteredBusinessPage, true))
          b              <- Future.fromTry(a.set(OrganisationNamePage, organisationRcaspDetails.RCASPName))
          c              <- Future.fromTry(b.set(OverwritableOrganisationName, organisationRcaspDetails.RCASPName))
          d              <- Future.fromTry(c.set(RegisteredBusinessIsThisYourBusinessNamePage, true))
          e              <- Future.fromTry(d.set(HaveTradingNamePage, haveTradingName))
          f              <- Future.fromTry {
                              if haveTradingName then e.set(TradingNamePage, organisationRcaspDetails.TradingName) else Success(e)
                            }
          g              <- Future.fromTry(f.set(UtrPage, utr))
          h              <- Future.fromTry(g.set(RegisteredBusinessIsTheAddressCorrectPage, true))
          i              <- Future.fromTry(h.set(CachedBusinessDetailsPage, cachedBusinessDetails))
          updatedAnswers <- Future.fromTry(i.set(ChangeRcaspCachedDetails, organisationRcaspDetails))
          _              <- sessionRepository.set(updatedAnswers)
        } yield Redirect(
          controllers.changeDetails.routes.RegisteredBusinessChangeDetailsController
            .onPageLoad(organisationRcaspDetails.RCASPID)
        )
      }
      .getOrElse {
        logger.warn(
          "[PopulateUserAnswersHelper][populateUserAnswersForRegisteredBusiness] Unable to populate user answers from OrganisationRcaspDetails"
        )
        Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }
  }
}
