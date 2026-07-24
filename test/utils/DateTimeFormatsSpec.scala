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

package utils

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.i18n.Lang
import utils.DateTimeFormats.dateTimeFormat

import java.time.{LocalDate, LocalTime}

class DateTimeFormatsSpec extends AnyFreeSpec with Matchers {

  ".dateTimeFormat" - {

    "must format dates in English" in {
      val formatter = dateTimeFormat()(Lang("en"))
      val result    = LocalDate.of(2023, 1, 1).format(formatter)
      result mustEqual "1 January 2023"
    }

    "must format dates in Welsh" in {
      val formatter = dateTimeFormat()(Lang("cy"))
      val result    = LocalDate.of(2023, 1, 1).format(formatter)
      result mustEqual "1 Ionawr 2023"
    }

    "must default to English format" in {
      val formatter = dateTimeFormat()(Lang("de"))
      val result    = LocalDate.of(2023, 1, 1).format(formatter)
      result mustEqual "1 January 2023"
    }
  }

  ".formatDate" - {

    "must format a date with no leading zero on the day" in {
      DateTimeFormats.formatDate(LocalDate.of(2027, 3, 1)) mustEqual "1 March 2027"
    }

    "must format a date with a two-digit day unchanged" in {
      DateTimeFormats.formatDate(LocalDate.of(2027, 3, 15)) mustEqual "15 March 2027"
    }

    "must format the month as a full word" in {
      DateTimeFormats.formatDate(LocalDate.of(2027, 9, 9)) mustEqual "9 September 2027"
    }

    "must format the year in full" in {
      DateTimeFormats.formatDate(LocalDate.of(2024, 12, 25)) mustEqual "25 December 2024"
    }
  }

  ".formatTime" - {

    "must display 'midnight' with no minutes when time is 00:00" in {
      DateTimeFormats.formatTime(LocalTime.of(0, 0)) mustEqual "midnight"
    }

    "must display 'midday' with no minutes when time is 12:00" in {
      DateTimeFormats.formatTime(LocalTime.of(12, 0)) mustEqual "midday"
    }

    "must format an ordinary afternoon time correctly (matches ticket example)" in {
      DateTimeFormats.formatTime(LocalTime.of(13, 11)) mustEqual "1:11pm"
    }

    "must format a morning time with a leading-zero minute and no leading-zero hour" in {
      DateTimeFormats.formatTime(LocalTime.of(9, 7)) mustEqual "9:07am"
    }

    "must format a time just before midnight as pm with hour converted from 24hr format" in {
      DateTimeFormats.formatTime(LocalTime.of(23, 59)) mustEqual "11:59pm"
    }

    "must format a time just after midday as pm" in {
      DateTimeFormats.formatTime(LocalTime.of(12, 1)) mustEqual "12:01pm"
    }

    "must format a time just after midnight as am" in {
      DateTimeFormats.formatTime(LocalTime.of(0, 1)) mustEqual "12:01am"
    }

    "must have no space between the minutes and am/pm" in {
      DateTimeFormats.formatTime(LocalTime.of(10, 30)) must not include " "
    }

    "must use a colon as the separator between hours and minutes" in {
      DateTimeFormats.formatTime(LocalTime.of(10, 30)) must include(":")
    }

    "must display 'midnight' when time has non-zero seconds but is still within the midnight minute" in {
      DateTimeFormats.formatTime(LocalTime.of(0, 0, 30)) mustEqual "midnight"
    }

    "must display 'midday' when time has non-zero seconds but is still within the midday minute" in {
      DateTimeFormats.formatTime(LocalTime.of(12, 0, 45)) mustEqual "midday"
    }

    "must display 'midnight' when time has non-zero nanoseconds" in {
      DateTimeFormats.formatTime(LocalTime.of(0, 0, 0, 500)) mustEqual "midnight"
    }
  }
}
