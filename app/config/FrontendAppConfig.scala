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

import com.google.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.i18n.Lang
import play.api.mvc.RequestHeader
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

@Singleton
class FrontendAppConfig @Inject() (configuration: Configuration, servicesConfig: ServicesConfig) {

  val host: String    = configuration.get[String]("host")
  val appName: String = configuration.get[String]("appName")

  private val carfManagementHost: String = servicesConfig.baseUrl("carf-management")

  val carfManagementBaseUrl: String = s"$carfManagementHost/carf-management"

  private val addressLookupHost: String = servicesConfig.baseUrl("address-lookup")
  lazy val addressLookupBaseUrl: String = s"$addressLookupHost/address-lookup"

  private val contactHost                  = configuration.get[String]("contact-frontend.host")
  private val contactFormServiceIdentifier = "carf-management-frontend"

  def feedbackUrl(implicit request: RequestHeader): String =
    s"$contactHost/contact/beta-feedback?service=$contactFormServiceIdentifier&backUrl=${host + request.uri}"

  val loginUrl: String                     = configuration.get[String]("urls.login")
  val loginContinueUrl: String             = configuration.get[String]("urls.loginContinue")
  val signOutUrl: String                   = configuration.get[String]("urls.signOut")
  val registrationUrl: String              = configuration.get[String]("urls.registration")
  val changeContactDetailsIndexUrl: String = configuration.get[String]("urls.changeContactDetails")

  val aeoiEmailAddress: String = configuration.get("email.aeoi")

  private val exitSurveyBaseUrl: String = configuration.get[Service]("microservice.services.feedback-frontend").baseUrl
  val exitSurveyUrl: String             = s"$exitSurveyBaseUrl/feedback/carf-management-frontend"

  val languageTranslationEnabled: Boolean =
    configuration.get[Boolean]("features.welsh-translation")

  def languageMap: Map[String, Lang] = Map(
    "en" -> Lang("en"),
    "cy" -> Lang("cy")
  )

  val timeout: Int   = configuration.get[Int]("timeout-dialog.timeout")
  val countdown: Int = configuration.get[Int]("timeout-dialog.countdown")

  val cacheTtl: Long = configuration.get[Int]("mongodb.timeToLiveInSeconds")

  val enrolmentKey: String   = configuration.get[String]("keys.enrolmentKey.carf")
  val ctEnrolmentKey: String = configuration.get[String]("keys.enrolmentKey.ct")

  val countryCodeJson: String = configuration.get[String]("countryCodeJson")
}
