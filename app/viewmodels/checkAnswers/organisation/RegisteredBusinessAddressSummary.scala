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

package viewmodels.checkAnswers.organisation

import controllers.organisation.routes
import models.UserAnswers
import pages.organisation.CachedBusinessDetailsPage
import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object RegisteredBusinessAddressSummary {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(CachedBusinessDetailsPage).map { businessDetails =>
      val address      = businessDetails.address
      val addressLines = Seq(
        Some(address.addressLine1),
        address.addressLine2,
        address.addressLine3,
        address.addressLine4,
        address.postalCode
        // TODO: Add country name here - once CARF-197 is merged
      ).flatten

      val addressHtml = Html(
        addressLines.zipWithIndex
          .map { case (line, index) =>
            if (index < addressLines.length - 1)
              s"""<span class="govuk-!-margin-bottom-0">$line</span><br/>"""
            else
              line
          }
          .mkString("\n")
      )

      SummaryListRowViewModel(
        key = "registeredBusiness.address.checkYourAnswersLabel",
        value = ValueViewModel(HtmlContent(addressHtml)),
        actions = Seq(
          ActionItemViewModel(
            "site.change",
            controllers.routes.PlaceholderController
              .onPageLoad(
                "Should nav to /registered-business/is-the-address-correct (CARF-197)" +
                  "routes.RegisteredBusinessIsTheAddressCorrectController.onPageLoad(ChangeMode).url"
              )
              .url
          )
            .withVisuallyHiddenText(messages("registeredBusiness.address.change.hidden"))
        )
      )
    }
}
