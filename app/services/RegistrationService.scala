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

package services

import models.BusinessDetails
import models.errors.InternalServerError
import models.responses.AddressRegistrationResponse
import types.ResultT

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class RegistrationService @Inject() ()(implicit ec: ExecutionContext) {

  // TODO  link up register-with-id - CARF-519
  def getBusinessWithUtr(utr: String): ResultT[BusinessDetails] =
    if (utr.startsWith("9")) {
      ResultT.fromError(InternalServerError)
    } else {
      ResultT.fromValue(
        BusinessDetails(
          name = "Test Business Ltd",
          address = AddressRegistrationResponse(
            addressLine1 = "1 Test Street",
            addressLine2 = Some("Testville"),
            addressLine3 = None,
            addressLine4 = None,
            postalCode = Some("TE1 1ST"),
            countryCode = "GB"
          )
        )
      )
    }
}
