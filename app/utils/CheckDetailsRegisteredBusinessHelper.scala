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
import pages.organisation.{HaveTradingNamePage, ReportForRegisteredBusinessPage}
import play.api.i18n.Messages
import viewmodels.Section
import viewmodels.changeDetails.RcaspIdSummary
import viewmodels.checkAnswers.organisation.*

class CheckDetailsRegisteredBusinessHelper {

  def getRegisteredBusinessSection(
      userAnswers: UserAnswers,
      changeJourney: Boolean
  )(implicit messages: Messages): Option[Section] =
    (for {
      reportForRegisteredBusinessAnswer <- userAnswers.get(ReportForRegisteredBusinessPage)
      if reportForRegisteredBusinessAnswer
      reportForRegisteredBusiness       <- ReportForRegisteredBusinessSummary.row(userAnswers, changeJourney = changeJourney)
      organisationName                  <- OverwritableOrganisationNameSummary.row(userAnswers, isRegisteredBusiness = true)
      haveTradingRow                    <- HaveTradingNameSummary.row(userAnswers)
      haveTrading                       <- userAnswers.get(HaveTradingNamePage)
      address                           <- RegisteredBusinessAddressSummary.row(userAnswers)
      maybeRcaspIdRow                   <- if changeJourney then RcaspIdSummary.row(userAnswers).map(Some(_)) else Some(None)
    } yield {
      val topBaseRows = maybeRcaspIdRow.fold {
        Seq(reportForRegisteredBusiness, organisationName, haveTradingRow)
      }(Seq(_, reportForRegisteredBusiness, organisationName, haveTradingRow))

      if (haveTrading) {
        TradingNameSummary.row(userAnswers).map { tradingRow =>
          topBaseRows :+ tradingRow :+ address
        }
      } else {
        Some(topBaseRows :+ address)
      }
    }).flatten.map(Section("", _))
}
