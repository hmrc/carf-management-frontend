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
import play.api.Logging
import play.api.i18n.Messages
import viewmodels.Section
import viewmodels.checkAnswers.organisation.*

import javax.inject.Inject

class CheckDetailsRegisteredBusinessHelper @Inject() extends Logging {

  def getRegisteredBusinessSection(userAnswers: UserAnswers)(implicit messages: Messages): Option[Section] =
    (for {
      reportForRegisteredBusinessAnswer <- userAnswers.get(ReportForRegisteredBusinessPage)
      if reportForRegisteredBusinessAnswer
      reportForRegisteredBusiness       <- ReportForRegisteredBusinessSummary.row(userAnswers, changeJourney = false)
      organisationName                  <- OverwritableOrganisationNameSummary.row(userAnswers, true)
      haveTradingRow                    <- HaveTradingNameSummary.row(userAnswers)
      haveTrading                       <- userAnswers.get(HaveTradingNamePage)
      address                           <- RegisteredBusinessAddressSummary.row(userAnswers)
    } yield {
      val topBaseRows = Seq(
        reportForRegisteredBusiness,
        organisationName,
        haveTradingRow
      )
      if (haveTrading) {
        TradingNameSummary.row(userAnswers).map { tradingRow =>
          topBaseRows :+ tradingRow :+ address
        }
      } else {
        Some(topBaseRows :+ address)
      }
    }).flatten.map(Section("", _))
}
