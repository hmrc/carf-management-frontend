package viewmodels.checkAnswers.$section$

import java.time.format.DateTimeFormatter

import controllers.$section$.routes
import models.{ChangeMode, UserAnswers}
import pages.$section$.$className$Page
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object $className$Summary  {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get($className$Page).map {
      answer =>

        val dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")

        SummaryListRowViewModel(
          key     = "$className;format="decap"$.checkYourAnswersLabel",
          value   = ValueViewModel(answer.format(dateFormatter)),
          actions = Seq(
            ActionItemViewModel(
              content = HtmlContent(s"""<span aria-hidden='true'>\${messages("site.change")}</span>"""),
              href = routes.$className$Controller.onPageLoad(ChangeMode).url
            ).withVisuallyHiddenText(messages("$className;format="decap"$.change.hidden"))
          )
        )
    }
}
