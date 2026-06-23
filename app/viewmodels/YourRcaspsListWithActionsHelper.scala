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

package viewmodels

import models.responses.RcaspDetails
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}
import uk.gov.hmrc.hmrcfrontend.views.viewmodels.listwithactions.{ListWithActions, ListWithActionsAction, ListWithActionsItem}

object YourRcaspsListWithActionsHelper {

  def getYourRcaspsRows(rcaspList: List[RcaspDetails])(implicit messages: Messages): ListWithActions = {
    val orderedRcasps: Seq[RcaspDetails] = orderRcasps(rcaspList)
    val items: Seq[ListWithActionsItem]  = orderedRcasps.map { rcasp =>
      ListWithActionsItem(
        name = getValueContent(rcasp.getName, rcasp.IsRCASPUser),
        actions = Seq(
          ListWithActionsAction(
            href = if (rcasp.IsRCASPUser) {
              controllers.routes.PlaceholderController
                .onPageLoad(s"Should nav to /registered-business/change-answers/${rcasp.RCASPID} (CARF-350)")
                .url
            } else {
              controllers.routes.PlaceholderController
                .onPageLoad(s"Should nav to /change-answers/${rcasp.RCASPID} (CARF-354)")
                .url
            },
            content = Text(messages("site.change")),
            visuallyHiddenText = Some(messages("yourRcasps.change.hidden", rcasp.getName))
          ),
          ListWithActionsAction(
            href = controllers.routes.PlaceholderController
              .onPageLoad(s"Should nav to /remove/user-access/${rcasp.RCASPID} (CARF-355)")
              .url,
            content = Text(messages("site.remove")),
            visuallyHiddenText = Some(messages("yourRcasps.remove.hidden", rcasp.getName))
          )
        )
      )
    }

    ListWithActions(items = items)
  }

  private def orderRcasps(rcaspList: Seq[RcaspDetails]): Seq[RcaspDetails] =
    rcaspList.sortBy(rcasp => (!rcasp.IsRCASPUser, rcasp.getName.toUpperCase))

  private def getValueContent(name: String, isRcaspUser: Boolean)(implicit messages: Messages): HtmlContent = {
    val registeredBusinessTag =
      if (isRcaspUser) {
        s"""<strong class="govuk-tag" style="max-width: 180px !important;">${messages(
            "yourRcasps.registeredBusinessTag"
          )}</strong>"""
      } else {
        ""
      }

    HtmlContent(s"""
         |<span class="govuk-!-margin-right-2" style="max-width: 180px">$name</span>
         |$registeredBusinessTag
    """.stripMargin.trim)
  }
}
