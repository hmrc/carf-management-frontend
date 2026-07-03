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

package models

import base.SpecBase
import models.OrganisationOrIndividual.{Individual, Organisation}
import pages.combined.OrganisationOrIndividualPage
import pages.individual.IndividualNamePage
import pages.organisation.{CachedBusinessDetailsPage, OverwritableOrganisationName, RegisteredBusinessIsThisYourBusinessNamePage}

class UserAnswersSpec extends SpecBase {

  "UserAnswers" - {
    "getRegisteredBusinessOrganisationNameMaybe method" - {
      "must return the cached business name when the user said it was correct" in {
        val ua = emptyUserAnswers
          .withPage(CachedBusinessDetailsPage, cachedBusinessDetails)
          .withPage(RegisteredBusinessIsThisYourBusinessNamePage, true)

        ua.getRegisteredBusinessOrganisationNameMaybe mustBe Some("Test Business Ltd")
      }
      "must return the declared business name when the user said the cached one was incorrect" in {
        val ua = emptyUserAnswers
          .withPage(RegisteredBusinessIsThisYourBusinessNamePage, false)
          .withPage(OverwritableOrganisationName, testOrgName)

        ua.getRegisteredBusinessOrganisationNameMaybe mustBe Some(testOrgName)
      }
      "must return the declared business name when the user did not answer if the cached one was correct" in {
        val ua = emptyUserAnswers
          .withPage(OverwritableOrganisationName, testOrgName)

        ua.getRegisteredBusinessOrganisationNameMaybe mustBe Some(testOrgName)
      }
      "must return None when no cached details exist but the user said it was correct" in {
        val ua = emptyUserAnswers
          .withPage(RegisteredBusinessIsThisYourBusinessNamePage, true)

        ua.getRegisteredBusinessOrganisationNameMaybe mustBe None
      }
      "must return None when no other business name exists but the user said the cached version was incorrect" in {
        val ua = emptyUserAnswers
          .withPage(RegisteredBusinessIsThisYourBusinessNamePage, false)

        ua.getRegisteredBusinessOrganisationNameMaybe mustBe None
      }
    }

    "retrieveRcaspName method" - {
      "must return the name inside IndividualNamePage has said they are an individual" in {
        val ua = emptyUserAnswers
          .withPage(OrganisationOrIndividualPage, Individual)
          .withPage(IndividualNamePage, testIndividualName)

        ua.retrieveRcaspName mustBe Some("Timmy Jimmison")
      }
      "must return OverwritableOrganisationName when the user has said they are an organisation" in {
        val ua = emptyUserAnswers
          .withPage(OrganisationOrIndividualPage, Organisation)
          .withPage(OverwritableOrganisationName, testOrgName)

        ua.retrieveRcaspName mustBe Some(testOrgName)
      }
      "must return OverwritableOrganisationName when the user has not answered OrganisationOrIndividualPage" in {
        val ua = emptyUserAnswers.withPage(OverwritableOrganisationName, testOrgName)

        ua.retrieveRcaspName mustBe Some(testOrgName)
      }
      "must return None when the user has said they are an individual but no name exists" in {
        val ua = emptyUserAnswers.withPage(OrganisationOrIndividualPage, Individual)

        ua.retrieveRcaspName mustBe None
      }
      "must return None when the user has said they are an organisation but no name exists" in {
        val ua = emptyUserAnswers.withPage(OrganisationOrIndividualPage, Organisation)

        ua.retrieveRcaspName mustBe None
      }
      "must return None when the user has not answered OrganisationOrIndividualPage and no name exists" in {
        emptyUserAnswers.retrieveRcaspName mustBe None
      }

    }

  }
}
