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
import models.OrganisationOrIndividual.Individual
import models.errors.ApiError.InternalServerError
import models.errors.MandatoryInformationMissingError
import models.requests.RcaspRequestCommon
import models.requests.deleteRcasp.{RcaspDetails as DeleteRcaspDetails, RcaspManagementRequest as DeleteRcaspManagementRequest, RcaspRequest as DeleteRcaspRequest}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{reset, times, verify, when}
import pages.combined.OrganisationOrIndividualPage
import pages.individual.IndividualNamePage
import types.ResultT
import utils.RcaspSubmissionHelper

class RcaspSubmissionServiceSpec extends SpecBase {

  val mockRcaspConnector: RcaspConnector               = mock[RcaspConnector]
  val mockRcaspSubmissionHelper: RcaspSubmissionHelper = mock[RcaspSubmissionHelper]
  val rcaspSubmissionService: RcaspSubmissionService   =
    new RcaspSubmissionService(mockRcaspConnector, mockRcaspSubmissionHelper)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockRcaspConnector)
    reset(mockRcaspSubmissionHelper)
  }

  private val testUserAnswers = emptyUserAnswers
    .withPage(OrganisationOrIndividualPage, Individual)
    .withPage(IndividualNamePage, testIndividualName)

  "RcaspSubmissionService" - {
    ".createRegisteredBusinessRcasp" - {
      "when the RcaspRequest is submitted successfully" in {
        when(mockRcaspSubmissionHelper.createRegisteredBusinessRcaspRequest(any(), any(), any())).thenReturn(
          Some(createRcaspRequestRegisteredBusiness)
        )

        when(mockRcaspConnector.createRcasp(eqTo(createRcaspRequestRegisteredBusiness))(any(), any())).thenReturn(
          ResultT.fromValue(createRcaspResponse)
        )

        val result =
          rcaspSubmissionService.createRegisteredBusinessRcasp(carfId, testUtr, testUserAnswers).value.futureValue

        result mustBe Right(createRcaspResponse)

        verify(mockRcaspSubmissionHelper, times(1))
          .createRegisteredBusinessRcaspRequest(eqTo(carfId), eqTo(testUtr), eqTo(testUserAnswers))
        verify(mockRcaspConnector, times(1)).createRcasp(eqTo(createRcaspRequestRegisteredBusiness))(any(), any())
      }

      "when the RcaspRequest is built but the connector returns an error" in {
        when(mockRcaspSubmissionHelper.createRegisteredBusinessRcaspRequest(any(), any(), any())).thenReturn(
          Some(createRcaspRequestRegisteredBusiness)
        )

        when(mockRcaspConnector.createRcasp(eqTo(createRcaspRequestRegisteredBusiness))(any(), any())).thenReturn(
          ResultT.fromError(InternalServerError)
        )

        val result =
          rcaspSubmissionService.createRegisteredBusinessRcasp(carfId, testUtr, testUserAnswers).value.futureValue

        result mustBe Left(InternalServerError)

        verify(mockRcaspSubmissionHelper, times(1))
          .createRegisteredBusinessRcaspRequest(eqTo(carfId), eqTo(testUtr), eqTo(testUserAnswers))
        verify(mockRcaspConnector, times(1)).createRcasp(eqTo(createRcaspRequestRegisteredBusiness))(any(), any())
      }

      "when the helper fails to build the RcaspRequest" in {
        when(mockRcaspSubmissionHelper.createRegisteredBusinessRcaspRequest(any(), any(), any())).thenReturn(None)

        val result =
          rcaspSubmissionService.createRegisteredBusinessRcasp(carfId, testUtr, testUserAnswers).value.futureValue

        result mustBe Left(
          MandatoryInformationMissingError("Error building the RcaspRequest from userAnswers")
        )

        verify(mockRcaspSubmissionHelper, times(1))
          .createRegisteredBusinessRcaspRequest(eqTo(carfId), eqTo(testUtr), eqTo(testUserAnswers))
        verify(mockRcaspConnector, times(0)).createRcasp(any())(any(), any())
      }
    }

    ".createRcasp" - {
      "when the RcaspRequest is submitted successfully" in {
        when(mockRcaspSubmissionHelper.createRcaspRequest(any(), any())).thenReturn(
          Some(createRcaspRequestIndividual)
        )

        when(mockRcaspConnector.createRcasp(eqTo(createRcaspRequestIndividual))(any(), any())).thenReturn(
          ResultT.fromValue(createRcaspResponse)
        )

        val result = rcaspSubmissionService.createRcasp(carfId, testUserAnswers).value.futureValue

        result mustBe Right(createRcaspResponse)

        verify(mockRcaspSubmissionHelper, times(1)).createRcaspRequest(eqTo(carfId), eqTo(testUserAnswers))
        verify(mockRcaspConnector, times(1)).createRcasp(eqTo(createRcaspRequestIndividual))(any(), any())
      }

      "when the RcaspRequest is built but the connector returns an error" in {
        when(mockRcaspSubmissionHelper.createRcaspRequest(any(), any())).thenReturn(
          Some(createRcaspRequestIndividual)
        )

        when(mockRcaspConnector.createRcasp(eqTo(createRcaspRequestIndividual))(any(), any())).thenReturn(
          ResultT.fromError(InternalServerError)
        )

        val result = rcaspSubmissionService.createRcasp(carfId, testUserAnswers).value.futureValue

        result mustBe Left(InternalServerError)

        verify(mockRcaspSubmissionHelper, times(1)).createRcaspRequest(eqTo(carfId), eqTo(testUserAnswers))
        verify(mockRcaspConnector, times(1)).createRcasp(eqTo(createRcaspRequestIndividual))(any(), any())
      }

      "when the helper fails to build the RcaspRequest" in {
        when(mockRcaspSubmissionHelper.createRcaspRequest(any(), any())).thenReturn(None)

        val result = rcaspSubmissionService.createRcasp(carfId, testUserAnswers).value.futureValue

        result mustBe Left(
          MandatoryInformationMissingError("Error building the RcaspRequest from userAnswers")
        )

        verify(mockRcaspSubmissionHelper, times(1)).createRcaspRequest(eqTo(carfId), eqTo(testUserAnswers))
        verify(mockRcaspConnector, times(0)).createRcasp(any())(any(), any())
      }
    }

    ".updateRegisteredBusinessRcasp" - {
      "when the RcaspRequest is submitted successfully" in {
        when(mockRcaspSubmissionHelper.updateRegisteredBusinessRcaspRequest(any(), any(), any())).thenReturn(
          Some(updateRcaspRequestRegisteredBusiness)
        )

        when(mockRcaspConnector.updateRcasp(eqTo(updateRcaspRequestRegisteredBusiness))(any(), any())).thenReturn(
          ResultT.fromValue(updateDeleteRcaspResponse)
        )

        val result =
          rcaspSubmissionService.updateRegisteredBusinessRcasp(carfId, testUtr, testUserAnswers).value.futureValue

        result mustBe Right(updateDeleteRcaspResponse)

        verify(mockRcaspSubmissionHelper, times(1))
          .updateRegisteredBusinessRcaspRequest(eqTo(carfId), eqTo(testUtr), eqTo(testUserAnswers))
        verify(mockRcaspConnector, times(1)).updateRcasp(eqTo(updateRcaspRequestRegisteredBusiness))(any(), any())
      }

      "when the RcaspRequest is built but the connector returns an error" in {
        when(mockRcaspSubmissionHelper.updateRegisteredBusinessRcaspRequest(any(), any(), any())).thenReturn(
          Some(updateRcaspRequestRegisteredBusiness)
        )

        when(mockRcaspConnector.updateRcasp(eqTo(updateRcaspRequestRegisteredBusiness))(any(), any())).thenReturn(
          ResultT.fromError(InternalServerError)
        )

        val result =
          rcaspSubmissionService.updateRegisteredBusinessRcasp(carfId, testUtr, testUserAnswers).value.futureValue

        result mustBe Left(InternalServerError)

        verify(mockRcaspSubmissionHelper, times(1))
          .updateRegisteredBusinessRcaspRequest(eqTo(carfId), eqTo(testUtr), eqTo(testUserAnswers))
        verify(mockRcaspConnector, times(1)).updateRcasp(eqTo(updateRcaspRequestRegisteredBusiness))(any(), any())
      }

      "when the helper fails to build the RcaspRequest" in {
        when(mockRcaspSubmissionHelper.updateRegisteredBusinessRcaspRequest(any(), any(), any())).thenReturn(None)

        val result =
          rcaspSubmissionService.updateRegisteredBusinessRcasp(carfId, testUtr, testUserAnswers).value.futureValue

        result mustBe Left(
          MandatoryInformationMissingError("Error building the RcaspRequest from userAnswers")
        )

        verify(mockRcaspSubmissionHelper, times(1))
          .updateRegisteredBusinessRcaspRequest(eqTo(carfId), eqTo(testUtr), eqTo(testUserAnswers))
        verify(mockRcaspConnector, times(0)).updateRcasp(any())(any(), any())
      }
    }

    ".updateRcasp" - {
      "when the RcaspRequest is submitted successfully" in {
        when(mockRcaspSubmissionHelper.updateRcaspRequest(any(), any())).thenReturn(
          Some(updateRcaspRequestIndividual)
        )

        when(mockRcaspConnector.updateRcasp(eqTo(updateRcaspRequestIndividual))(any(), any())).thenReturn(
          ResultT.fromValue(updateDeleteRcaspResponse)
        )

        val result = rcaspSubmissionService.updateRcasp(carfId, testUserAnswers).value.futureValue

        result mustBe Right(updateDeleteRcaspResponse)

        verify(mockRcaspSubmissionHelper, times(1)).updateRcaspRequest(eqTo(carfId), eqTo(testUserAnswers))
        verify(mockRcaspConnector, times(1)).updateRcasp(eqTo(updateRcaspRequestIndividual))(any(), any())
      }

      "when the RcaspRequest is built but the connector returns an error" in {
        when(mockRcaspSubmissionHelper.updateRcaspRequest(any(), any())).thenReturn(
          Some(updateRcaspRequestIndividual)
        )

        when(mockRcaspConnector.updateRcasp(eqTo(updateRcaspRequestIndividual))(any(), any())).thenReturn(
          ResultT.fromError(InternalServerError)
        )

        val result = rcaspSubmissionService.updateRcasp(carfId, testUserAnswers).value.futureValue

        result mustBe Left(InternalServerError)

        verify(mockRcaspSubmissionHelper, times(1)).updateRcaspRequest(eqTo(carfId), eqTo(testUserAnswers))
        verify(mockRcaspConnector, times(1)).updateRcasp(eqTo(updateRcaspRequestIndividual))(any(), any())
      }

      "when the helper fails to build the RcaspRequest" in {
        when(mockRcaspSubmissionHelper.updateRcaspRequest(any(), any())).thenReturn(None)

        val result = rcaspSubmissionService.updateRcasp(carfId, testUserAnswers).value.futureValue

        result mustBe Left(
          MandatoryInformationMissingError("Error building the RcaspRequest from userAnswers")
        )

        verify(mockRcaspSubmissionHelper, times(1)).updateRcaspRequest(eqTo(carfId), eqTo(testUserAnswers))
        verify(mockRcaspConnector, times(0)).createRcasp(any())(any(), any())
      }
    }

    ".removeRcasp" - {
      "must return a Right(()) when the connector call succeeds" in {
        when(mockRcaspConnector.deleteRcasp(any())(any(), any()))
          .thenReturn(ResultT.fromValue(updateDeleteRcaspResponse))

        val result: ResultT[Unit] = rcaspSubmissionService.removeRcasp(testCarfId, rcaspId)

        result.value.futureValue mustBe Right(())

        verify(mockRcaspConnector, times(1)).deleteRcasp(any())(any(), any())
      }

      "must build the DeleteRcaspRequest with the correct RequestCommon and RCASPID/SubscriptionID" in {
        when(mockRcaspConnector.deleteRcasp(any())(any(), any()))
          .thenReturn(ResultT.fromValue(updateDeleteRcaspResponse))

        rcaspSubmissionService.removeRcasp(testCarfId, rcaspId).value.futureValue

        val expectedRequest = DeleteRcaspRequest(
          RCASPManagement = DeleteRcaspManagementRequest(
            RequestCommon = RcaspRequestCommon(
              OriginatingSystem = "MDTP",
              TransmittingSystem = "EIS",
              RequestType = "DELETE",
              Regime = "CARF",
              RequestParameters = None
            ),
            RequestDetails = DeleteRcaspDetails(
              RCASPID = rcaspId,
              SubscriptionID = testCarfId
            )
          )
        )

        verify(mockRcaspConnector, times(1)).deleteRcasp(eqTo(expectedRequest))(any(), any())
      }

      "must return a Left when the connector returns an error" in {
        when(mockRcaspConnector.deleteRcasp(any())(any(), any()))
          .thenReturn(ResultT.fromError(InternalServerError))

        val result: ResultT[Unit] = rcaspSubmissionService.removeRcasp(testCarfId, rcaspId)

        result.value.futureValue mustBe Left(InternalServerError)

        verify(mockRcaspConnector, times(1)).deleteRcasp(any())(any(), any())
      }
    }
  }
}
