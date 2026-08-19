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

package generators

import models.{AddressAndUPRN, AddressUk, FindAddress}
import models.individual.IndividualName
import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.Arbitrary.arbitrary

trait ModelGenerators {

  implicit lazy val arbitraryIndividualName: Arbitrary[IndividualName] =
    Arbitrary {
      for {
        firstName <- arbitrary[String]
        lastName  <- arbitrary[String]
      } yield IndividualName(firstName, lastName)
    }

  val addressStringGen: Gen[String] = Gen.alphaNumStr.suchThat(_.trim.nonEmpty).map(_.take(35))

  val postcodeStringGen: Gen[String] = Gen.alphaNumStr.suchThat(_.trim.nonEmpty).map(_.take(10))

  implicit lazy val arbitraryFindAddress: Arbitrary[FindAddress] =
    Arbitrary {
      for {
        postcode             <- arbitrary[String]
        propertyNameOrNumber <- arbitrary[String]
      } yield FindAddress(postcode, Some(propertyNameOrNumber))
    }

  implicit lazy val arbitraryAddressUk: Arbitrary[AddressUk] =
    Arbitrary {
      for {
        addressLine1 <- addressStringGen
        addressLine2 <- Gen.option(addressStringGen)
        addressLine3 <- Gen.option(addressStringGen)
        townOrCity   <- arbitrary[String]
        postcode     <- postcodeStringGen
      } yield AddressUk(
        addressLine1 = addressLine1,
        addressLine2 = addressLine2,
        addressLine3 = addressLine3,
        townOrCity = townOrCity,
        postCode = postcode
      )
    }

  implicit lazy val arbitraryAddressesAndUPRNSeq: Arbitrary[Seq[AddressAndUPRN]] =
    Arbitrary {
      for {
        addressUk <- arbitrary[AddressUk]
        uprn      <- Gen.long
      } yield Seq(
        AddressAndUPRN(addressUk, uprn),
        AddressAndUPRN(addressUk, uprn),
        AddressAndUPRN(addressUk, uprn)
      )
    }

}
