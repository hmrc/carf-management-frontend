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

package utils

import models.UserAnswers
import pages.individual.IndividualHavePhonePage
import pages.organisation.{HaveTradingNamePage, OrganisationFirstContactHavePhonePage, OrganisationHaveSecondContactPage, OrganisationSecondContactHavePhonePage, ReportForRegisteredBusinessPage}
import play.api.Logging
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.Section
import viewmodels.checkAnswers.combined.{OrganisationOrIndividualSummary, UkAddressSummary}
import viewmodels.checkAnswers.individual.*
import viewmodels.checkAnswers.organisation.*

import javax.inject.Inject

class CheckDetailsHelper @Inject() extends Logging {

  private def getSharedQuestionRows(
      userAnswers: UserAnswers
  )(implicit messages: Messages): Option[Seq[SummaryListRow]] =
    userAnswers.get(ReportForRegisteredBusinessPage) match {
      case Some(true)  => None
      case Some(false) =>
        for {
          reportForRegisteredBusiness <- ReportForRegisteredBusinessSummary.row(userAnswers)
          organisationOrIndividual    <- OrganisationOrIndividualSummary.row(userAnswers, showAcronymOnly = true)
        } yield Seq(reportForRegisteredBusiness, organisationOrIndividual)
      case None        =>
        OrganisationOrIndividualSummary.row(userAnswers, showAcronymOnly = false).map(Seq(_))
    }

  def getIndividualSectionMaybe(userAnswers: UserAnswers)(implicit messages: Messages): Option[Section] =
    for {
      sharedRows <- getSharedQuestionRows(userAnswers)
      rcaspName  <- IndividualNameSummary.row(userAnswers)
      ni         <- NiNumberSummary.row(userAnswers)
      address    <- UkAddressSummary.row(userAnswers)
    } yield Section("", sharedRows ++ Seq(rcaspName, ni, address))

  def getIndividualContactDetailsMaybe(userAnswers: UserAnswers)(implicit messages: Messages): Option[Section] =
    (for {
      emailSummary     <- IndividualEmailSummary.row(userAnswers)
      havePhoneSummary <- IndividualHavePhoneSummary.row(userAnswers)
      havePhone        <- userAnswers.get(IndividualHavePhonePage)
    } yield {
      val baseSummaries = Seq(emailSummary, havePhoneSummary)
      if (havePhone) {
        IndividualPhoneSummary.row(userAnswers).map { phoneSummary =>
          baseSummaries :+ phoneSummary
        }
      } else {
        Some(baseSummaries)
      }
    }).flatten.map(Section(messages("checkDetails.summaryListTitle.individualDetails"), _))

  def getOrganisationSectionMaybe(userAnswers: UserAnswers)(implicit messages: Messages): Option[Section] = {
    for {
      sharedRows            <- getSharedQuestionRows(userAnswers)
      organisationName      <- OverwritableOrganisationNameSummary.row(userAnswers, false)
      haveTradingName       <- HaveTradingNameSummary.row(userAnswers)
      haveTradingNameAnswer <- userAnswers.get(HaveTradingNamePage)
      utr                   <- UtrSummary.row(userAnswers)
      address               <- UkAddressSummary.row(userAnswers)
    } yield
      if (haveTradingNameAnswer) {
        TradingNameSummary.row(userAnswers).map {
          sharedRows ++ Seq(organisationName, haveTradingName, _, utr, address)
        }
      } else {
        Some(sharedRows ++ Seq(organisationName, haveTradingName, utr, address))
      }
  }.flatten.map(Section("", _))

  def getOrganisationFirstContactDetailsMaybe(
      userAnswers: UserAnswers
  )(implicit messages: Messages): Option[Section] = {
    for {
      firstContactName            <- OrganisationFirstContactNameSummary.row(userAnswers)
      firstContactEmail           <- OrganisationFirstContactEmailSummary.row(userAnswers)
      firstContactHavePhone       <- OrganisationFirstContactHavePhoneSummary.row(userAnswers)
      firstContactHavePhoneAnswer <- userAnswers.get(OrganisationFirstContactHavePhonePage)
    } yield
      if (firstContactHavePhoneAnswer) {
        OrganisationFirstContactPhoneNumberSummary.row(userAnswers).map {
          Seq(firstContactName, firstContactEmail, firstContactHavePhone, _)
        }
      } else {
        Some(Seq(firstContactName, firstContactEmail, firstContactHavePhone))
      }
  }.flatten.map(Section(messages("checkDetails.summaryListTitle.firstContact"), _))

  def getOrganisationSecondContactDetailsMaybe(
      userAnswers: UserAnswers
  )(implicit messages: Messages): Option[Section] = {
    for {
      haveSecondContact    <- userAnswers.get(OrganisationHaveSecondContactPage)
      haveSecondContactRow <- OrganisationHaveSecondContactSummary.row(userAnswers)
    } yield {
      if (haveSecondContact) {
        for {
          secondContactName            <- OrganisationSecondContactNameSummary.row(userAnswers)
          secondContactEmail           <- OrganisationSecondContactEmailSummary.row(userAnswers)
          secondContactHavePhone       <- OrganisationSecondContactHavePhoneSummary.row(userAnswers)
          secondContactHavePhoneAnswer <- userAnswers.get(OrganisationSecondContactHavePhonePage)
        } yield
          if (secondContactHavePhoneAnswer) {
            OrganisationSecondContactPhoneNumberSummary.row(userAnswers).map {
              Seq(haveSecondContactRow, secondContactName, secondContactEmail, secondContactHavePhone, _)
            }
          } else {
            Some(Seq(haveSecondContactRow, secondContactName, secondContactEmail, secondContactHavePhone))
          }
      } else {
        Some(Some(Seq(haveSecondContactRow)))
      }
    }.flatten
  }.flatten.map(Section(messages("checkDetails.summaryListTitle.secondContact"), _))
}
