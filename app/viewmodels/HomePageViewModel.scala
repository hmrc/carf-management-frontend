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

import models.{NormalMode, UniqueTaxpayerReference}
import play.api.i18n.Messages

case class HomePageViewModel(
    isBusiness: Boolean,
    hasZeroRcaspsAdded: Boolean,
    hasSentFilesInLast28Days: Boolean,
    organisationName: Option[String],
    ctUtr: Option[UniqueTaxpayerReference],
    carfId: String
) {

  def getNoRcaspsSectionText(implicit messages: Messages): Option[(String, String)] =
    if (hasZeroRcaspsAdded) {
      Some((messages("homePage.noRcasps.p1"), messages("homePage.noRcasps.p1.link")))
    } else {
      None
    }

  lazy val getNoRcaspsSectionLink: String = controllers.routes.RoutingController.onPageLoad(NormalMode).url

  lazy val getUploadXmlLink: String =
    controllers.routes.PlaceholderController
      .onPageLoad("Should redirect to /report/upload-file (part of file upload journey)")
      .url

  lazy val getViewResultsLink: String =
    controllers.routes.PlaceholderController
      .onPageLoad("Should redirect to /result-of-automatic-checks (part of file upload journey)")
      .url

  lazy val getAddRcaspLink: String =
    controllers.routes.RoutingController.onPageLoad(NormalMode).url

  lazy val getManageRcaspsLink: String =
    controllers.routes.PlaceholderController
      .onPageLoad("Should redirect to /rcasp/your-rcasps (part of management change journey)")
      .url

  def getContactDetailsSectionHeading(implicit messages: Messages): String =
    if (isBusiness) {
      messages("homePage.contactDetails.org.subheading")
    } else {
      messages("homePage.contactDetails.ind.subheading")
    }

  def getContactDetailsSectionLinkText(implicit messages: Messages): String =
    if (isBusiness) {
      messages(
        "homePage.contactDetails.org.linkText",
        organisationName.getOrElse(messages("homePage.contactDetails.org.fallbackBusinessName"))
      )
    } else {
      messages("homePage.contactDetails.ind.linkText")
    }
}
