package viewmodels.checkAnswers.sectionName

import controllers.sectionName.routes
import models.{ChangeMode, UserAnswers}
import pages.FindAddressPage
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object FindAddressSummary  {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(FindAddressPage).map {
      answer =>

        SummaryListRowViewModel(
          key     = "findAddress.checkYourAnswersLabel",
          value   = ValueViewModel(HtmlFormat.escape(answer).toString),
          actions = Seq(
            ActionItemViewModel("site.change", routes.FindAddressController.onPageLoad(ChangeMode).url)
              .withVisuallyHiddenText(messages("findAddress.change.hidden"))
          )
        )
    }
}
