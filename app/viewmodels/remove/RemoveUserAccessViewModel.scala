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

package viewmodels.remove

import forms.GenericYesNoPageFormProvider
import models.UserBusinessSubscriptionData
import models.viewAndUpdateRcasp.RcaspDetails
import play.api.data.Form
import play.api.i18n.Messages

case class RemoveUserAccessViewModel(
    titleKey: String,
    headingKey: String,
    rcaspName: String,
    userBusinessNameOpt: Option[String],
    form: Form[Boolean]
)

object RemoveUserAccessViewModel {

  def from(
      details: RcaspDetails,
      userInfo: UserBusinessSubscriptionData,
      formProvider: GenericYesNoPageFormProvider
  )(implicit messages: Messages): RemoveUserAccessViewModel = {

    val rcaspName   = details.getName
    val isRcaspUser = details.IsRCASPUser

    val suffix =
      if (!userInfo.hasOrganisationContactDetails) "individual"
      else if (isRcaspUser) "rcaspIsUser"
      else "otherOrg"

    val titleKey   = s"removeUserAccess.title.$suffix"
    val headingKey = s"removeUserAccess.heading.$suffix"
    val errorKey   = s"removeUserAccess.error.required.$suffix"

    val maybeUserBusinessName: Option[String] =
      if (userInfo.hasOrganisationContactDetails && !isRcaspUser)
        userInfo.organisationName.orElse(Some(messages("homePage.contactDetails.org.fallbackBusinessName")))
      else None

    RemoveUserAccessViewModel(
      titleKey = titleKey,
      headingKey = headingKey,
      rcaspName = rcaspName,
      userBusinessNameOpt = maybeUserBusinessName,
      form = formProvider(errorKey)
    )
  }
}
