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

package controllers.organisation

import config.Constants.ZERO
import models.{NormalMode, UniqueTaxpayerReference, UserAnswers}
import pages.organisation.{OrganisationNamePage, ReportForRegisteredBusinessPage, TradingNamePage}
import play.api.mvc.Call

trait RcaspHelper {

  def rcaspDisplayName(userAnswers: UserAnswers): Option[String] =
    userAnswers.get(TradingNamePage) match {
      case Some(tradingName) => Some(tradingName)
      case None              => userAnswers.get(OrganisationNamePage)
    }

  private def rcaspIsUser(
      rcaspCount: Int,
      ctUtr: Option[UniqueTaxpayerReference],
      userAnswers: UserAnswers
  ): Boolean = {
    val answeredYes = userAnswers.get(ReportForRegisteredBusinessPage).contains(true)

    rcaspCount == ZERO &&
    ctUtr.nonEmpty &&
    answeredYes
  }

  def rcaspIsUserRedirect(
      rcaspCount: Int,
      ctUtr: Option[UniqueTaxpayerReference],
      userAnswers: UserAnswers
  ): Call =
    if (rcaspIsUser(rcaspCount, ctUtr, userAnswers)) {
      controllers.organisation.routes.RegisteredBusinessIsTheAddressCorrectController.onPageLoad(NormalMode)
    } else {
      controllers.organisation.routes.UtrController.onPageLoad(NormalMode)
    }
}
