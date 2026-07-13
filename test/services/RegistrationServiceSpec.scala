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

import base.SpecBase
import connectors.RegistrationConnector
import models.BusinessDetails
import models.errors.ApiError.InternalServerError
import models.requests.RegisterWithIdRequest
import models.responses.RegisterWithIdResponse
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{reset, times, verify, when}
import types.ResultT

class RegistrationServiceSpec extends SpecBase {

  val mockRegistrationConnector: RegistrationConnector = mock[RegistrationConnector]

  val registrationService: RegistrationService = new RegistrationService(mockRegistrationConnector)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockRegistrationConnector)
  }

  val testRegisterWithIdResponse: RegisterWithIdResponse =
    RegisterWithIdResponse(testOrgName, testAddressRegistrationResponse)

  "RegistrationService" - {
    ".getBusinessWithCtUtr" - {
      "must return BusinessDetails when the connector call succeeds" in {
        when(mockRegistrationConnector.registerOrganisationWithUtrCtAutoMatch(any())(any()))
          .thenReturn(ResultT.fromValue(testRegisterWithIdResponse))

        val result: ResultT[BusinessDetails] = registrationService.getBusinessWithCtUtr(testUtr.uniqueTaxPayerReference)
        val expectedRequest                  =
          RegisterWithIdRequest(requiresNameMatch = false, IDNumber = testUtr.uniqueTaxPayerReference, IDType = "UTR")

        result.value.futureValue mustBe Right(BusinessDetails(testOrgName, testAddressRegistrationResponse))
        verify(mockRegistrationConnector, times(1)).registerOrganisationWithUtrCtAutoMatch(eqTo(expectedRequest))(any())
      }

      "must return a Left when the connector returns an error" in {
        when(mockRegistrationConnector.registerOrganisationWithUtrCtAutoMatch(any())(any()))
          .thenReturn(ResultT.fromError(InternalServerError))

        val result: ResultT[BusinessDetails] = registrationService.getBusinessWithCtUtr(testCarfId)

        result.value.futureValue mustBe Left(InternalServerError)

        verify(mockRegistrationConnector, times(1)).registerOrganisationWithUtrCtAutoMatch(any())(any())
      }
    }
  }
}
