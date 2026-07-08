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
import connectors.{RcaspConnector, SubscriptionConnector}
import models.HomePageSubscriptionData
import models.errors.ApiError.{InternalServerError, NotFoundError}
import models.responses.{RcaspResponseDetails, ViewRcasp, ViewRcaspResponse}
import models.viewAndUpdateRcasp.RcaspDetails
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{reset, times, verify, when}
import types.ResultT

class AccountServiceSpec extends SpecBase {

  val mockRcaspConnector: RcaspConnector               = mock[RcaspConnector]
  val mockSubscriptionConnector: SubscriptionConnector = mock[SubscriptionConnector]

  val accountService: AccountService = new AccountService(mockRcaspConnector, mockSubscriptionConnector)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockRcaspConnector, mockSubscriptionConnector)
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

    ".getRcaspDetails" - {
      "must return a Right with RcaspDetails" in {
        val viewRcaspResponse = ViewRcaspResponse(
          ViewRCASP = ViewRcasp(
            ResponseCommon = rcaspResponseCommon,
            ResponseDetails = RcaspResponseDetails(
              RCASPList = List(
                individualRcaspDetailsResponse.copy(RCASPID = "RCASP1"),
                organisationRcaspDetailsResponse.copy(RCASPID = "RCASP2"),
                individualRcaspDetailsResponse,
                organisationRcaspDetailsResponse.copy(RCASPID = "RCASP3")
              )
            )
          )
        )

        when(mockRcaspConnector.viewRcasp(any())(any(), any())).thenReturn(ResultT.fromValue(viewRcaspResponse))

        val result: ResultT[RcaspDetails] = accountService.getRcaspDetails(testCarfId, rcaspId)

        result.value.futureValue mustBe Right(individualRcaspDetailsResponse)

        verify(mockRcaspConnector, times(1)).viewRcasp(eqTo(testCarfId))(any(), any())
      }

      "must return a Right with RcaspDetails when an RCASPID matches after converting to uppercase" in {
        val viewRcaspResponse = ViewRcaspResponse(
          ViewRCASP = ViewRcasp(
            ResponseCommon = rcaspResponseCommon,
            ResponseDetails = RcaspResponseDetails(
              RCASPList = List(
                individualRcaspDetailsResponse.copy(RCASPID = "RCASP1"),
                organisationRcaspDetailsResponse.copy(RCASPID = "RCASP2"),
                individualRcaspDetailsResponse.copy(RCASPID = "ZMcar0123456789"),
                organisationRcaspDetailsResponse.copy(RCASPID = "RCASP3")
              )
            )
          )
        )

        when(mockRcaspConnector.viewRcasp(any())(any(), any())).thenReturn(ResultT.fromValue(viewRcaspResponse))

        val result: ResultT[RcaspDetails] = accountService.getRcaspDetails(testCarfId, "zmCAR0123456789")

        result.value.futureValue mustBe Right(individualRcaspDetailsResponse.copy(RCASPID = "ZMcar0123456789"))

        verify(mockRcaspConnector, times(1)).viewRcasp(eqTo(testCarfId))(any(), any())
      }

      "must return a Left when the connector returns an error" in {
        when(mockRcaspConnector.viewRcasp(any())(any(), any())).thenReturn(ResultT.fromError(InternalServerError))

        val result: ResultT[RcaspDetails] = accountService.getRcaspDetails(testCarfId, rcaspId)

        result.value.futureValue mustBe Left(InternalServerError)

        verify(mockRcaspConnector, times(1)).viewRcasp(eqTo(testCarfId))(any(), any())
      }

      "must return a Left with NotFoundError when the connector response does not contain details for the RCASPID" in {
        val viewRcaspResponse = ViewRcaspResponse(
          ViewRCASP = ViewRcasp(
            ResponseCommon = rcaspResponseCommon,
            ResponseDetails = RcaspResponseDetails(
              RCASPList = List(
                individualRcaspDetailsResponse.copy(RCASPID = "RCASP1"),
                organisationRcaspDetailsResponse.copy(RCASPID = "RCASP2")
              )
            )
          )
        )

        when(mockRcaspConnector.viewRcasp(any())(any(), any())).thenReturn(ResultT.fromValue(viewRcaspResponse))

        val result: ResultT[RcaspDetails] = accountService.getRcaspDetails(testCarfId, rcaspId)

        result.value.futureValue mustBe Left(NotFoundError)

        verify(mockRcaspConnector, times(1)).viewRcasp(eqTo(testCarfId))(any(), any())
      }
    }

    ".getHomePageSubscriptionData" - {
      "must return a Right when individual contact details are present" in {
        when(mockSubscriptionConnector.displaySubscription(any())(any(), any()))
          .thenReturn(ResultT.fromValue(testIndividualDisplaySubscriptionResponse))

        val result: ResultT[HomePageSubscriptionData] = accountService.getHomePageSubscriptionData(testCarfId)

        result.value.futureValue mustBe Right(
          HomePageSubscriptionData(hasOrganisationContactDetails = false, organisationName = None)
        )

        verify(mockSubscriptionConnector, times(1)).displaySubscription(eqTo(testCarfId))(any(), any())
      }

      "must return a Right when organisation contact details are present with trading name" in {
        when(mockSubscriptionConnector.displaySubscription(any())(any(), any()))
          .thenReturn(ResultT.fromValue(testOrganisationDisplaySubscriptionResponse(Some(testTradingName))))

        val result: ResultT[HomePageSubscriptionData] = accountService.getHomePageSubscriptionData(testCarfId)

        result.value.futureValue mustBe Right(
          HomePageSubscriptionData(hasOrganisationContactDetails = true, organisationName = Some(testTradingName))
        )

        verify(mockSubscriptionConnector, times(1)).displaySubscription(eqTo(testCarfId))(any(), any())
      }

      "must return a Right when organisation contact details are present without trading name" in {
        when(mockSubscriptionConnector.displaySubscription(any())(any(), any()))
          .thenReturn(ResultT.fromValue(testOrganisationDisplaySubscriptionResponse(None)))

        val result: ResultT[HomePageSubscriptionData] = accountService.getHomePageSubscriptionData(testCarfId)

        result.value.futureValue mustBe Right(
          HomePageSubscriptionData(hasOrganisationContactDetails = true, organisationName = None)
        )

        verify(mockSubscriptionConnector, times(1)).displaySubscription(eqTo(testCarfId))(any(), any())
      }

      "must return a Left when both individual and organisation contact details are present" in {
        when(mockSubscriptionConnector.displaySubscription(any())(any(), any()))
          .thenReturn(ResultT.fromValue(testInvalidSubscriptionResponseBoth))

        val result: ResultT[HomePageSubscriptionData] = accountService.getHomePageSubscriptionData(testCarfId)

        result.value.futureValue mustBe Left(InternalServerError)

        verify(mockSubscriptionConnector, times(1)).displaySubscription(eqTo(testCarfId))(any(), any())
      }

      "must return a Left when neither individual nor organisation contact details are present" in {
        when(mockSubscriptionConnector.displaySubscription(any())(any(), any()))
          .thenReturn(ResultT.fromValue(testInvalidSubscriptionResponseNeither))

        val result: ResultT[HomePageSubscriptionData] = accountService.getHomePageSubscriptionData(testCarfId)

        result.value.futureValue mustBe Left(InternalServerError)

        verify(mockSubscriptionConnector, times(1)).displaySubscription(eqTo(testCarfId))(any(), any())
      }

      "must return a Left when the connector returns an error" in {
        when(mockSubscriptionConnector.displaySubscription(any())(any(), any()))
          .thenReturn(ResultT.fromError(InternalServerError))

        val result: ResultT[HomePageSubscriptionData] = accountService.getHomePageSubscriptionData(testCarfId)

        result.value.futureValue mustBe Left(InternalServerError)

        verify(mockSubscriptionConnector, times(1)).displaySubscription(eqTo(testCarfId))(any(), any())
      }
    }
    ".getUserBusinessName" - {

      "must return a Right with Some trading name when organisation contact details are present with trading name" in {
        when(mockSubscriptionConnector.displaySubscription(any())(any(), any()))
          .thenReturn(ResultT.fromValue(testOrganisationDisplaySubscriptionResponse(Some(testTradingName))))

        val result: ResultT[Option[String]] = accountService.getUserBusinessName(testCarfId)

        result.value.futureValue mustBe Right(Some(testTradingName))

        verify(mockSubscriptionConnector, times(1)).displaySubscription(eqTo(testCarfId))(any(), any())
      }

      "must return a Right with None when organisation contact details are present without trading name" in {
        when(mockSubscriptionConnector.displaySubscription(any())(any(), any()))
          .thenReturn(ResultT.fromValue(testOrganisationDisplaySubscriptionResponse(None)))

        val result: ResultT[Option[String]] = accountService.getUserBusinessName(testCarfId)

        result.value.futureValue mustBe Right(None)

        verify(mockSubscriptionConnector, times(1)).displaySubscription(eqTo(testCarfId))(any(), any())
      }

      "must return a Right with Some full name when individual contact details are present" in {
        when(mockSubscriptionConnector.displaySubscription(any())(any(), any()))
          .thenReturn(ResultT.fromValue(testIndividualDisplaySubscriptionResponse))

        val result: ResultT[Option[String]] = accountService.getUserBusinessName(testCarfId)

        result.value.futureValue mustBe Right(Some("Joe Smith"))

        verify(mockSubscriptionConnector, times(1)).displaySubscription(eqTo(testCarfId))(any(), any())
      }

      "must return a Left when both individual and organisation contact details are present" in {
        when(mockSubscriptionConnector.displaySubscription(any())(any(), any()))
          .thenReturn(ResultT.fromValue(testInvalidSubscriptionResponseBoth))

        val result: ResultT[Option[String]] = accountService.getUserBusinessName(testCarfId)

        result.value.futureValue mustBe Left(InternalServerError)

        verify(mockSubscriptionConnector, times(1)).displaySubscription(eqTo(testCarfId))(any(), any())
      }

      "must return a Left when neither individual nor organisation contact details are present" in {
        when(mockSubscriptionConnector.displaySubscription(any())(any(), any()))
          .thenReturn(ResultT.fromValue(testInvalidSubscriptionResponseNeither))

        val result: ResultT[Option[String]] = accountService.getUserBusinessName(testCarfId)

        result.value.futureValue mustBe Left(InternalServerError)

        verify(mockSubscriptionConnector, times(1)).displaySubscription(eqTo(testCarfId))(any(), any())
      }

      "must return a Left when the connector returns an error" in {
        when(mockSubscriptionConnector.displaySubscription(any())(any(), any()))
          .thenReturn(ResultT.fromError(InternalServerError))

        val result: ResultT[Option[String]] = accountService.getUserBusinessName(testCarfId)

        result.value.futureValue mustBe Left(InternalServerError)

        verify(mockSubscriptionConnector, times(1)).displaySubscription(eqTo(testCarfId))(any(), any())
      }
    }
  }
}
