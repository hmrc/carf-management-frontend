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

import models.{AddressUk, UserAnswers}
import pages.UkAddressInUserAnswers
import pages.organisation.{CachedBusinessDetailsPage, RegisteredBusinessIsTheAddressCorrectPage}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object RegisteredBusinessAddressSummary {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] = {

    val address: Option[String] = if (answers.get(RegisteredBusinessIsTheAddressCorrectPage).contains(true)) {
      import models.responses.renderHtml
      answers.get(CachedBusinessDetailsPage).map(_.address.renderHtml)
    } else {
      import models.renderHtml
      answers.get(UkAddressInUserAnswers).map(_.renderHtml)
    }

    address.map { address =>
      SummaryListRowViewModel(
        key = "registeredBusiness.address.checkYourAnswersLabel",
        value = ValueViewModel(HtmlContent(address)),
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
}
