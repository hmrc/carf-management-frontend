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

package config

import models.countries.*

object Constants {

  inline val ZERO = 0

  inline val standardTextInputRegex               = """^[a-zA-Z0-9 &'\\`^\-]*$"""
  inline val standardTextInputWithoutNumbersRegex = """^[a-zA-Z &'\\`^\-]*$"""

  inline final val ninoFormatRegex = """^[A-Z]{2}[0-9]{6}[A-Z]{1}$"""
  inline final val realNinoRegex   =
    "^([ACEHJLMOPRSWXY][A-CEGHJ-NPR-TW-Z]|B[A-CEHJ-NPR-TW-Z]|G[ACEGHJ-NPR-TW-Z]|[KT][A-CEGHJ-MPR-TW-Z]|N[A-CEGHJL-NPR-SW-Z]|Z[A-CEGHJ-NPR-TW-Y])[0-9]{6}[A-D ]$"

  final val regexPostcode        = """^[A-Za-z]{1,2}\d[A-Za-z0-9]?\s?\d[A-Za-z]{2}$"""
  final val postCodeAllowedChars = """^[A-Za-z0-9 ]*$"""

  inline final val maxNiNumberLength = 9

  inline final val maxEmailLength = 132
  inline final val maxPhoneLength = 24

  inline final val notReal0808PhoneNumber = "+448081570192"

  val cdPostcodeRegex: Map[String, String] = Map(
    Guernsey.code  -> "^GY([1-9]|10) ?[0-9][A-Z]{2}$",
    Jersey.code    -> "^JE[1-4] ?[0-9][A-Z]{2}$",
    IsleOfMan.code -> "^IM([1-9]|99) ?[0-9][A-Z]{2}$"
  )
}
