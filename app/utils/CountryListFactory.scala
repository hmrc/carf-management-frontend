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

import config.FrontendAppConfig
import models.Country
import play.api.Environment
import play.api.libs.json.Json

import javax.inject.{Inject, Singleton}

@Singleton
class CountryListFactory @Inject() (environment: Environment, appConfig: FrontendAppConfig) {

  private def countryList: Option[Seq[Country]] =
    environment.resourceAsStream(appConfig.countryCodeJson).map(Json.parse).map {
      _.as[Seq[Country]]
    }

  def getDescriptionFromCode(code: String): Option[String] =
    countryList.flatMap {
      _.find(_.code == code).map(_.description)
    }
}
