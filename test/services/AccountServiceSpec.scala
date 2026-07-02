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
import connectors.RcaspConnector
import models.errors.ApiError.InternalServerError
import models.responses.{RcaspResponseDetails, ViewRcasp, ViewRcaspResponse}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{reset, times, verify, when}
import types.ResultT

class AccountServiceSpec extends SpecBase {

  val mockRcaspConnector: RcaspConnector = mock[RcaspConnector]

  val accountService: AccountService = new AccountService(mockRcaspConnector)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockRcaspConnector)
  }

  "AccountService" - {
    ".getNumberOfRcaspsCurrentlyAdded" - {
      "must return a Right with the number of rcasps added when the view rcasp call succeeds" - {
        "when there are 0 rcasps" in {
          val viewRcaspResponse = ViewRcaspResponse(
            ViewRCASP = ViewRcasp(
              ResponseCommon = rcaspResponseCommon,
              ResponseDetails = RcaspResponseDetails(
                RCASPList = List.empty
              )
            )
          )

          when(mockRcaspConnector.viewRcasp(any())(any(), any())).thenReturn(ResultT.fromValue(viewRcaspResponse))

          val result: ResultT[Int] = accountService.getNumberOfRcaspsCurrentlyAdded(testCarfId)

          result.value.futureValue mustBe Right(0)

          verify(mockRcaspConnector, times(1)).viewRcasp(eqTo(testCarfId))(any(), any())
        }

        "when there is 1 rcasp" in {
          when(mockRcaspConnector.viewRcasp(any())(any(), any())).thenReturn(ResultT.fromValue(testViewRcaspResponse))

          val result: ResultT[Int] = accountService.getNumberOfRcaspsCurrentlyAdded(testCarfId)

          result.value.futureValue mustBe Right(1)

          verify(mockRcaspConnector, times(1)).viewRcasp(eqTo(testCarfId))(any(), any())
        }

        "when there are 4 rcasps" in {
          val viewRcaspResponse = ViewRcaspResponse(
            ViewRCASP = ViewRcasp(
              ResponseCommon = rcaspResponseCommon,
              ResponseDetails = RcaspResponseDetails(
                RCASPList = List(
                  individualRcaspDetailsResponse,
                  organisationRcaspDetailsResponse,
                  individualRcaspDetailsResponse,
                  organisationRcaspDetailsResponse
                )
              )
            )
          )

          when(mockRcaspConnector.viewRcasp(any())(any(), any())).thenReturn(ResultT.fromValue(viewRcaspResponse))

          val result: ResultT[Int] = accountService.getNumberOfRcaspsCurrentlyAdded(testCarfId)

          result.value.futureValue mustBe Right(4)

          verify(mockRcaspConnector, times(1)).viewRcasp(eqTo(testCarfId))(any(), any())
        }
      }

      "must return a Left when the connector returns an error" in {
        when(mockRcaspConnector.viewRcasp(any())(any(), any())).thenReturn(ResultT.fromError(InternalServerError))

        val result: ResultT[Int] = accountService.getNumberOfRcaspsCurrentlyAdded(testCarfId)

        result.value.futureValue mustBe Left(InternalServerError)

        verify(mockRcaspConnector, times(1)).viewRcasp(eqTo(testCarfId))(any(), any())
      }
    }
  }
}
