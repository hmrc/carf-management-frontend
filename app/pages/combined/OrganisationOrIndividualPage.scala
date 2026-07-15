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

package pages.combined

import models.{OrganisationOrIndividual, UserAnswers}
import play.api.libs.json.JsPath
import pages.QuestionPage
import pages.individual.*
import pages.organisation.*
import models.OrganisationOrIndividual.{Individual, Organisation}

import scala.util.{Success, Try}

case object OrganisationOrIndividualPage extends QuestionPage[OrganisationOrIndividual] {
  override def path: JsPath     = JsPath \ toString
  override def toString: String = "organisationOrIndividual"

  override def cleanup(
      newValue: OrganisationOrIndividual,
      userAnswers: UserAnswers,
      hasChanged: Boolean
  ): Try[UserAnswers] =
    if (hasChanged) {
      val pagesToRemove = newValue match {
        case Organisation =>
          List(
            IndividualNamePage,
            NiNumberPage,
            IndividualEmailPage,
            IndividualHavePhonePage,
            IndividualPhonePage
          )
        case Individual   =>
          List(
            OrganisationNamePage,
            OverwritableOrganisationName,
            HaveTradingNamePage,
            TradingNamePage,
            UtrPage,
            OrganisationFirstContactNamePage,
            OrganisationFirstContactEmailPage,
            OrganisationFirstContactHavePhonePage,
            OrganisationFirstContactPhoneNumberPage,
            OrganisationHaveSecondContactPage,
            OrganisationSecondContactNamePage,
            OrganisationSecondContactEmailPage,
            OrganisationSecondContactHavePhonePage,
            OrganisationSecondContactPhoneNumberPage
          )
      }
      userAnswers.remove(pagesToRemove)
    } else {
      Success(userAnswers)
    }
}
