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

import models.viewAndUpdateRcasp.RcaspDetails

import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneId}
import java.util.Locale

case class RcaspRemovedViewModel(
    rcaspName: String,
    rcaspId: String,
    formattedDateTime: String
)

object RcaspRemovedViewModel {

  private val ukZone        = ZoneId.of("Europe/London")
  private val dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.UK)

  def from(details: RcaspDetails, removedAt: Instant): RcaspRemovedViewModel =
    RcaspRemovedViewModel(
      rcaspName = details.getName,
      rcaspId = details.RCASPID,
      formattedDateTime = formatDateTime(removedAt)
    )

  private def formatDateTime(instant: Instant): String = {
    val zdt = instant.atZone(ukZone)

    val datePart = zdt.format(dateFormatter)

    val hour   = zdt.getHour
    val minute = zdt.getMinute

    val timePart = (hour, minute) match {
      case (0, 0)  => "midnight"
      case (12, 0) => "midday"
      case _       =>
        val period     = if (hour < 12) "am" else "pm"
        val hour12     = if (hour % 12 == 0) 12 else hour % 12
        val minutePart = f"$minute%02d"
        s"$hour12:$minutePart$period"
    }

    s"$datePart at $timePart"
  }
}
