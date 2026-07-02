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

import config.Constants.ZERO
import models.OrganisationOrIndividual.Individual
import models.{UniqueTaxpayerReference, UserAnswers}
import pages.combined.OrganisationOrIndividualPage
import pages.individual.IndividualNamePage
import pages.organisation.{OverwritableOrganisationName, ReportForRegisteredBusinessPage}

object RcaspHelper {
  def retrieveRcaspName(userAnswers: UserAnswers): Option[String] =
    userAnswers.get(OrganisationOrIndividualPage) match {
      case Some(Individual) => userAnswers.get(IndividualNamePage).map(_.fullName)
      case _                => userAnswers.get(OverwritableOrganisationName)
    }

  def isRcaspUser(
      rcaspCount: Int,
      ctUtr: Option[UniqueTaxpayerReference],
      userAnswers: UserAnswers
  ): Boolean = {
    val answeredYes = userAnswers.get(ReportForRegisteredBusinessPage).contains(true)
    rcaspCount == ZERO && ctUtr.nonEmpty && answeredYes
  }
}
