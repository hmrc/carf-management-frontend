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
import models.responses.SubmitRcaspResponse
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
        when(mockRcaspSubmissionHelper.createRcaspRequestForRegisteredBusiness(any(), any(), any())).thenReturn(
          Some(createRcaspRequestRegisteredBusiness)
        )

        when(mockRcaspConnector.createRcasp(eqTo(createRcaspRequestRegisteredBusiness))(any(), any())).thenReturn(
          ResultT.fromValue(submitRcaspResponse)
        )

        val result =
          rcaspSubmissionService.createRegisteredBusinessRcasp(carfId, testUtr, testUserAnswers).value.futureValue

        result mustBe Right(submitRcaspResponse)

        verify(mockRcaspSubmissionHelper, times(1))
          .createRcaspRequestForRegisteredBusiness(eqTo(carfId), eqTo(testUtr), eqTo(testUserAnswers))
        verify(mockRcaspConnector, times(1)).createRcasp(eqTo(createRcaspRequestRegisteredBusiness))(any(), any())
      }

      "when the RcaspRequest is built but the connector returns an error" in {
        when(mockRcaspSubmissionHelper.createRcaspRequestForRegisteredBusiness(any(), any(), any())).thenReturn(
          Some(createRcaspRequestRegisteredBusiness)
        )

        when(mockRcaspConnector.createRcasp(eqTo(createRcaspRequestRegisteredBusiness))(any(), any())).thenReturn(
          ResultT.fromError(InternalServerError)
        )

        val result =
          rcaspSubmissionService.createRegisteredBusinessRcasp(carfId, testUtr, testUserAnswers).value.futureValue

        result mustBe Left(InternalServerError)

        verify(mockRcaspSubmissionHelper, times(1))
          .createRcaspRequestForRegisteredBusiness(eqTo(carfId), eqTo(testUtr), eqTo(testUserAnswers))
        verify(mockRcaspConnector, times(1)).createRcasp(eqTo(createRcaspRequestRegisteredBusiness))(any(), any())
      }

      "when the helper fails to build the RcaspRequest" in {
        when(mockRcaspSubmissionHelper.createRcaspRequestForRegisteredBusiness(any(), any(), any())).thenReturn(None)

        val result =
          rcaspSubmissionService.createRegisteredBusinessRcasp(carfId, testUtr, testUserAnswers).value.futureValue

        result mustBe Left(
          MandatoryInformationMissingError("Error building the RcaspRequest from userAnswers")
        )

        verify(mockRcaspSubmissionHelper, times(1))
          .createRcaspRequestForRegisteredBusiness(eqTo(carfId), eqTo(testUtr), eqTo(testUserAnswers))
        verify(mockRcaspConnector, times(0)).createRcasp(any())(any(), any())
      }
    }

    ".createRcasp" - {
      "when the RcaspRequest is submitted successfully" in {
        when(mockRcaspSubmissionHelper.createRcaspRequest(any(), any())).thenReturn(
          Some(createRcaspRequestIndividual)
        )

        when(mockRcaspConnector.createRcasp(eqTo(createRcaspRequestIndividual))(any(), any())).thenReturn(
          ResultT.fromValue(submitRcaspResponse)
        )

        val result = rcaspSubmissionService.createRcasp(carfId, testUserAnswers).value.futureValue

        result mustBe Right(submitRcaspResponse)

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
        when(mockRcaspSubmissionHelper.updateRcaspRequestForRegisteredBusiness(any(), any(), any())).thenReturn(
          Some(updateRcaspRequestRegisteredBusiness)
        )

        when(mockRcaspConnector.updateRcasp(eqTo(updateRcaspRequestRegisteredBusiness))(any(), any())).thenReturn(
          ResultT.fromValue(submitRcaspResponse)
        )

        val result =
          rcaspSubmissionService.updateRegisteredBusinessRcasp(carfId, testUtr, testUserAnswers).value.futureValue

        result mustBe Right(submitRcaspResponse)

        verify(mockRcaspSubmissionHelper, times(1))
          .updateRcaspRequestForRegisteredBusiness(eqTo(carfId), eqTo(testUtr), eqTo(testUserAnswers))
        verify(mockRcaspConnector, times(1)).updateRcasp(eqTo(updateRcaspRequestRegisteredBusiness))(any(), any())
      }

      "when the RcaspRequest is built but the connector returns an error" in {
        when(mockRcaspSubmissionHelper.updateRcaspRequestForRegisteredBusiness(any(), any(), any())).thenReturn(
          Some(updateRcaspRequestRegisteredBusiness)
        )

        when(mockRcaspConnector.updateRcasp(eqTo(updateRcaspRequestRegisteredBusiness))(any(), any())).thenReturn(
          ResultT.fromError(InternalServerError)
        )

        val result =
          rcaspSubmissionService.updateRegisteredBusinessRcasp(carfId, testUtr, testUserAnswers).value.futureValue

        result mustBe Left(InternalServerError)

        verify(mockRcaspSubmissionHelper, times(1))
          .updateRcaspRequestForRegisteredBusiness(eqTo(carfId), eqTo(testUtr), eqTo(testUserAnswers))
        verify(mockRcaspConnector, times(1)).updateRcasp(eqTo(updateRcaspRequestRegisteredBusiness))(any(), any())
      }

      "when the helper fails to build the RcaspRequest" in {
        when(mockRcaspSubmissionHelper.updateRcaspRequestForRegisteredBusiness(any(), any(), any())).thenReturn(None)

        val result =
          rcaspSubmissionService.updateRegisteredBusinessRcasp(carfId, testUtr, testUserAnswers).value.futureValue

        result mustBe Left(
          MandatoryInformationMissingError("Error building the RcaspRequest from userAnswers")
        )

        verify(mockRcaspSubmissionHelper, times(1))
          .updateRcaspRequestForRegisteredBusiness(eqTo(carfId), eqTo(testUtr), eqTo(testUserAnswers))
        verify(mockRcaspConnector, times(0)).updateRcasp(any())(any(), any())
      }
    }

    ".updateRcasp" - {
      "when the RcaspRequest is submitted successfully" in {
        when(mockRcaspSubmissionHelper.updateRcaspRequest(any(), any())).thenReturn(
          Some(updateRcaspRequestIndividual)
        )

        when(mockRcaspConnector.updateRcasp(eqTo(updateRcaspRequestIndividual))(any(), any())).thenReturn(
          ResultT.fromValue(submitRcaspResponse)
        )

        val result = rcaspSubmissionService.updateRcasp(carfId, testUserAnswers).value.futureValue

        result mustBe Right(submitRcaspResponse)

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
  }
}
