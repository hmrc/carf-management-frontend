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

import base.SpecBase
import models.viewAndUpdateRcasp.RcaspDetails
import models.{Mode, NormalMode}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}
import uk.gov.hmrc.hmrcfrontend.views.viewmodels.listwithactions.{ListWithActions, ListWithActionsAction}

class YourRcaspsListWithActionsHelperSpec extends SpecBase {

  implicit val messages: Messages = messages(app)

  "YourRcaspsListWithActionsHelper" - {
    ".getYourRcaspsRows" - {
      "given an empty list" in {
        val listWithActions = YourRcaspsListWithActionsHelper.getYourRcaspsRows(List.empty)
        listWithActions mustBe ListWithActions(items = List.empty)
      }

      "given a list of 2 RcaspDetails (of which neither is a registered business)" in {
        val rcaspDetailsList: List[RcaspDetails] = List(
          individualRcaspDetailsResponse.copy(RCASPID = "RCASP1", IsRCASPUser = false),
          organisationRcaspDetailsResponse.copy(RCASPID = "RCASP2", IsRCASPUser = false)
        )

        val listWithActions = YourRcaspsListWithActionsHelper.getYourRcaspsRows(rcaspDetailsList)

        val expectedValueContent: Seq[HtmlContent] = Seq(
          HtmlContent(
            """<span class="govuk-!-margin-right-2" style="line-height:1.4">Timmy Jimmison</span>"""
          ),
          HtmlContent(
            """<span class="govuk-!-margin-right-2" style="line-height:1.4">Timmy Ltd</span>"""
          )
        )

        val expectedActionItems: Seq[Seq[ListWithActionsAction]] = Seq(
          Seq(
            ListWithActionsAction(
              href = controllers.changeDetails.routes.ChangeDetailsRoutingController.onPageLoad("RCASP1").url,
              content = Text("Change"),
              visuallyHiddenText = Some("Change the details for Timmy Jimmison")
            ),
            ListWithActionsAction(
              href = controllers.combined.routes.RemoveUserAccessController
                .onPageLoad(NormalMode: Mode, "RCASP2")
                .url,
              content = Text("Remove"),
              visuallyHiddenText = Some("Remove Timmy Jimmison from the list")
            )
          ),
          Seq(
            ListWithActionsAction(
              href = controllers.changeDetails.routes.ChangeDetailsRoutingController.onPageLoad("RCASP2").url,
              content = Text("Change"),
              visuallyHiddenText = Some("Change the details for Timmy Ltd")
            ),
            ListWithActionsAction(
              href = controllers.combined.routes.RemoveUserAccessController
                .onPageLoad(NormalMode: Mode, "RCASP1")
                .url,
              content = Text("Remove"),
              visuallyHiddenText = Some("Remove Timmy Ltd from the list")
            )
          )
        )

        listWithActions.items.map(_.name)    mustBe expectedValueContent
        listWithActions.items.map(_.actions) mustBe expectedActionItems
      }

      "given a list of 4 RcaspDetails (of which 1 is a registered business)" in {
        val rcaspDetailsList: List[RcaspDetails] = List(
          organisationRcaspDetailsResponse.copy(RCASPID = "RCASP1", IsRCASPUser = false, RCASPName = "org1"),
          individualRcaspDetailsResponse
            .copy(RCASPID = "RCASP2", IsRCASPUser = false, FirstName = "John", LastName = "Doe"),
          organisationRcaspDetailsResponse.copy(RCASPID = "RCASP3", IsRCASPUser = true, RCASPName = "ORG2"),
          individualRcaspDetailsResponse
            .copy(RCASPID = "RCASP4", IsRCASPUser = false, FirstName = "Ann", LastName = "Smith")
        )

        val listWithActions = YourRcaspsListWithActionsHelper.getYourRcaspsRows(rcaspDetailsList)

        val expectedValueContent: Seq[HtmlContent] = Seq(
          HtmlContent("""
            |<span class="govuk-!-margin-right-2" style="line-height:1.4">ORG2</span>
            |<strong class="govuk-tag" style="max-width: 230px !important;">Registered&nbsp;business</strong>
          """.stripMargin.trim),
          HtmlContent(
            """<span class="govuk-!-margin-right-2" style="line-height:1.4">Ann Smith</span>"""
          ),
          HtmlContent(
            """<span class="govuk-!-margin-right-2" style="line-height:1.4">John Doe</span>"""
          ),
          HtmlContent("""<span class="govuk-!-margin-right-2" style="line-height:1.4">org1</span>""")
        )

        val expectedActionItems: Seq[Seq[ListWithActionsAction]] = Seq(
          Seq(
            ListWithActionsAction(
              href = controllers.changeDetails.routes.ChangeDetailsRoutingController.onPageLoad("RCASP3").url,
              content = Text("Change"),
              visuallyHiddenText = Some("Change the details for ORG2")
            ),
            ListWithActionsAction(
              href = controllers.combined.routes.RemoveUserAccessController
                .onPageLoad(NormalMode: Mode, "RCASP3")
                .url,
              content = Text("Remove"),
              visuallyHiddenText = Some("Remove ORG2 from the list")
            )
          ),
          Seq(
            ListWithActionsAction(
              href = controllers.changeDetails.routes.ChangeDetailsRoutingController.onPageLoad("RCASP4").url,
              content = Text("Change"),
              visuallyHiddenText = Some("Change the details for Ann Smith")
            ),
            ListWithActionsAction(
              href = controllers.combined.routes.RemoveUserAccessController
                .onPageLoad(NormalMode: Mode, "RCASP4")
                .url,
              content = Text("Remove"),
              visuallyHiddenText = Some("Remove Ann Smith from the list")
            )
          ),
          Seq(
            ListWithActionsAction(
              href = controllers.changeDetails.routes.ChangeDetailsRoutingController.onPageLoad("RCASP2").url,
              content = Text("Change"),
              visuallyHiddenText = Some("Change the details for John Doe")
            ),
            ListWithActionsAction(
              href = controllers.combined.routes.RemoveUserAccessController
                .onPageLoad(NormalMode: Mode, "RCASP2")
                .url,
              content = Text("Remove"),
              visuallyHiddenText = Some("Remove John Doe from the list")
            )
          ),
          Seq(
            ListWithActionsAction(
              href = controllers.changeDetails.routes.ChangeDetailsRoutingController.onPageLoad("RCASP1").url,
              content = Text("Change"),
              visuallyHiddenText = Some("Change the details for org1")
            ),
            ListWithActionsAction(
              href = controllers.combined.routes.RemoveUserAccessController
                .onPageLoad(NormalMode: Mode, "RCASP1")
                .url,
              content = Text("Remove"),
              visuallyHiddenText = Some("Remove org1 from the list")
            )
          )
        )

        listWithActions.items.map(_.name)    mustBe expectedValueContent
        listWithActions.items.map(_.actions) mustBe expectedActionItems
      }
    }
  }
}
