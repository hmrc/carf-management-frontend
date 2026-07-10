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
import models.responses.{RcaspDetails, ViewRcaspResponse}
import play.api.data.Form

case class RemoveUserAccessViewModel(
    titleKey: String,
    headingKey: String,
    errorKey: String,
    rcaspName: String,
    userBusinessName: Option[String],
    form: Form[Boolean]
)

object RemoveUserAccessViewModel {

  private val IndividualPartyType = "Individual"

  def from(
      rcaspId: String,
      viewRcaspResponse: ViewRcaspResponse,
      userBusinessNameOpt: Option[String],
      formProvider: GenericYesNoPageFormProvider
  ): Either[String, RemoveUserAccessViewModel] = {
    val rcaspList        = viewRcaspResponse.ViewRCASP.ResponseDetails.RCASPList
    val selectedRcaspOpt = rcaspList.find(_.RCASPID == rcaspId)

    selectedRcaspOpt match {
      case None =>
        Left(s"Could not find selected RCASP for rcaspId=$rcaspId")

      case Some(selectedRcasp) =>
        val rcaspName   = selectedRcasp.getName
        val isRcaspUser = selectedRcasp.IsRCASPUser
        val partyType   = selectedRcasp.PartyType

        val suffix =
          if (partyType == IndividualPartyType) "individual"
          else if (isRcaspUser) "rcaspIsUser"
          else "otherOrg"

        val titleKey   = s"removeUserAccess.title.$suffix"
        val headingKey = s"removeUserAccess.heading.$suffix"
        val errorKey   = s"removeUserAccess.error.required.$suffix"

        val userBusinessName: Option[String] =
          if (partyType == IndividualPartyType) None
          else if (isRcaspUser) Some(rcaspName)
          else userBusinessNameOpt

        if (userBusinessName.isEmpty && partyType != IndividualPartyType && !isRcaspUser) {
          Left(s"User business name was missing for rcaspId=$rcaspId")
        } else {
          Right(
            RemoveUserAccessViewModel(
              titleKey = titleKey,
              headingKey = headingKey,
              errorKey = errorKey,
              rcaspName = rcaspName,
              userBusinessName = userBusinessName,
              form = formProvider(errorKey)
            )
          )
        }
    }
  }
}
