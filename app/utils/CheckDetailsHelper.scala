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
import play.api.Logging
import play.api.i18n.Messages
import viewmodels.Section
import viewmodels.checkAnswers.combined.UkAddressSummary
import viewmodels.checkAnswers.individual.*

import javax.inject.Inject

class CheckDetailsHelper @Inject() extends Logging {

  def getIndividualSectionMaybe(userAnswers: UserAnswers)(implicit messages: Messages): Option[Section] =
    for {
      rcaspName <- IndividualNameSummary.row(userAnswers)
      ni        <- NiNumberSummary.row(userAnswers)
      address   <- UkAddressSummary.row(userAnswers)
    } yield Section("", Seq(rcaspName, ni, address))

  def getContactDetails(userAnswers: UserAnswers)(implicit messages: Messages): Option[Section] =
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
    }).flatten.map(Section(messages("checkDetailsAnswers.summaryListTitle"), _))
}
