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

package models

import cats.syntax.all.*
import models.OrganisationOrIndividual.Individual
import models.changeDetails.{IndividualRcaspDetailsForComparison, OrganisationRcaspDetailsForComparison, RcaspDetailsForComparison}
import models.crypto.CryptoType.CryptoT
import models.crypto.SensitiveJsObject
import pages.UkAddressInUserAnswers
import pages.combined.OrganisationOrIndividualPage
import pages.individual.*
import pages.organisation.*
import play.api.libs.json.*
import queries.{Gettable, Settable}
import uk.gov.hmrc.crypto.json.JsonEncryption
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant
import scala.util.{Failure, Success, Try}

final case class UserAnswers(
    id: String,
    rcaspIsRegisteredBusiness: Boolean,
    data: JsObject = Json.obj(),
    lastUpdated: Instant = Instant.now
) {

  def get[A](page: Gettable[A])(implicit rds: Reads[A]): Option[A] =
    Reads.optionNoError(Reads.at(page.path)).reads(data).getOrElse(None)

  def set[A](page: Settable[A] & Gettable[A], newValue: A)(implicit
      writes: Writes[A],
      rds: Reads[A]
  ): Try[UserAnswers] = {

    lazy val hasValueChanged = !get(page).contains(newValue)

    val updatedData = data.setObject(page.path, Json.toJson(newValue)) match {
      case JsSuccess(jsValue, _) =>
        Success(jsValue)
      case JsError(errors)       =>
        Failure(JsResultException(errors))
    }

    updatedData.flatMap { d =>
      val updatedAnswers = copy(data = d)
      page.cleanup(newValue, updatedAnswers, hasValueChanged)
    }
  }

  def remove[A](page: Settable[A]): Try[UserAnswers] = {

    val updatedData = data.removeObject(page.path) match {
      case JsSuccess(jsValue, _) =>
        Success(jsValue)
      case JsError(_)            =>
        Success(data)
    }

    updatedData.flatMap { d =>
      val updatedAnswers = copy(data = d)
      Success(updatedAnswers)
    }
  }

  def remove(pages: List[Settable[_]]): Try[UserAnswers] =
    pages.foldLeft(Try(this)) { (oldAnswerList, page) =>
      oldAnswerList.flatMap(_.remove(page))
    }

  def getRegisteredBusinessOrganisationNameMaybe: Option[String] =
    if (this.get(RegisteredBusinessIsThisYourBusinessNamePage).contains(true)) {
      this.get(CachedBusinessDetailsPage).map(_.name)
    } else {
      this.get(OverwritableOrganisationName)
    }

  def retrieveRcaspName: Option[String] =
    this.get(OrganisationOrIndividualPage) match {
      case Some(Individual) => this.get(IndividualNamePage).map(_.fullName)
      case _                => this.get(OverwritableOrganisationName)
    }

  def getRcaspDetailsForComparison: Option[RcaspDetailsForComparison] =
    if (this.get(OrganisationOrIndividualPage).contains(Individual)) {
      for {
        isRcaspUser    <- this.get(ReportForRegisteredBusinessPage)
        individualName <- this.get(IndividualNamePage)
        nino           <- this.get(NiNumberPage)
        address        <- this.get(UkAddressInUserAnswers).map(_.toRcaspAddress)
        email          <- this.get(IndividualEmailPage)
        phone           = this.get(IndividualPhonePage)
      } yield IndividualRcaspDetailsForComparison(
        isRcaspUser,
        individualName.firstName,
        individualName.lastName,
        nino,
        address,
        email,
        phone
      )
    } else {
      for {
        isRcaspUser       <- this.get(ReportForRegisteredBusinessPage)
        rcaspName         <- this.get(OverwritableOrganisationName)
        tradingName        = this.get(TradingNamePage).getOrElse(rcaspName)
        utr               <- if isRcaspUser then Some(None) else this.get(UtrPage).map(Some(_))
        address           <- this.get(UkAddressInUserAnswers).map(_.toRcaspAddress)
        firstContactName   = this.get(OrganisationFirstContactNamePage)
        firstContactEmail  = this.get(OrganisationFirstContactEmailPage)
        firstContactPhone  = this.get(OrganisationFirstContactPhoneNumberPage)
        secondContactName  = this.get(OrganisationSecondContactNamePage)
        secondContactEmail = this.get(OrganisationSecondContactEmailPage)
        secondContactPhone = this.get(OrganisationSecondContactPhoneNumberPage)
      } yield OrganisationRcaspDetailsForComparison(
        isRcaspUser,
        rcaspName,
        tradingName,
        utr,
        address,
        primaryContactDetails = (firstContactName, firstContactEmail).mapN { (name, email) =>
          RcaspContactDetails(name, email, firstContactPhone)
        },
        secondaryContactDetails = (secondContactName, secondContactEmail).mapN { (name, email) =>
          RcaspContactDetails(name, email, secondContactPhone)
        }
      )
    }
}

object UserAnswers {

  import play.api.libs.functional.syntax.*

  def mongoFormat(encryptionEnabled: Boolean)(implicit crypto: CryptoT): OFormat[UserAnswers] = {
    implicit val sensitiveJsObjectFormat: Format[SensitiveJsObject] =
      if (encryptionEnabled) {
        JsonEncryption.sensitiveEncrypterDecrypter(SensitiveJsObject.apply)
      } else {
        Json.format[SensitiveJsObject]
      }

    (
      (__ \ "_id").format[String] and
        (__ \ "rcaspIsRegisteredBusiness").format[Boolean] and
        (__ \ "data").format[SensitiveJsObject] and
        (__ \ "lastUpdated").format(MongoJavatimeFormats.instantFormat)
    )(
      (id, rcaspIsRegisteredBusiness, data, lastUpdated) =>
        UserAnswers(id, rcaspIsRegisteredBusiness, data.decryptedValue, lastUpdated),
      ua => (ua.id, ua.rcaspIsRegisteredBusiness, SensitiveJsObject(ua.data), ua.lastUpdated)
    )
  }

}
