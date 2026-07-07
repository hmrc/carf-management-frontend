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

package viewmodels.checkAnswers.combined

import controllers.combined.routes
import models.{ChangeMode, UserAnswers}
import pages.combined.RemoveOtherAccessPage
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object RemoveOtherAccessSummary  {

  def row(answers: UserAnswers, rcaspId: String)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(RemoveOtherAccessPage).map {
      answer =>

        val value = if (answer) "site.yes" else "site.no"

        SummaryListRowViewModel(
          key     = "removeOtherAccess.checkYourAnswersLabel",
          value   = ValueViewModel(value),
          actions = Seq(
            ActionItemViewModel(
              content = HtmlContent(s"""<span aria-hidden='true'>${messages("site.change")}</span>"""),
              href = routes.RemoveOtherAccessController.onPageLoad(ChangeMode, rcaspId).url
            ).withVisuallyHiddenText(messages("removeOtherAccess.change.hidden"))
          )
        )
    }
}
