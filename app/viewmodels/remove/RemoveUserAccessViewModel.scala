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
import models.viewAndUpdateRcasp.RcaspDetails
import play.api.data.Form

case class RemoveUserAccessViewModel(
    titleKey: String,
    headingKey: String,
    rcaspName: String,
    userBusinessName: Option[String],
    form: Form[Boolean]
)

object RemoveUserAccessViewModel {

  private val individualPartyType = "Individual"

  def from(
      details: RcaspDetails,
      userBusinessName: String,
      formProvider: GenericYesNoPageFormProvider
  ): RemoveUserAccessViewModel = {

    val rcaspName   = details.getName
    val isRcaspUser = details.IsRCASPUser
    val partyType   = details.PartyType

    val suffix =
      if (partyType == individualPartyType) "individual"
      else if (isRcaspUser) "rcaspIsUser"
      else "otherOrg"

    val titleKey   = s"removeUserAccess.title.$suffix"
    val headingKey = s"removeUserAccess.heading.$suffix"
    val errorKey   = s"removeUserAccess.error.required.$suffix"

    val maybeUserBusinessName: Option[String] =
      if (partyType != individualPartyType && !isRcaspUser) Some(userBusinessName)
      else None

    RemoveUserAccessViewModel(
      titleKey = titleKey,
      headingKey = headingKey,
      rcaspName = rcaspName,
      userBusinessName = maybeUserBusinessName,
      form = formProvider(errorKey)
    )
  }
}
