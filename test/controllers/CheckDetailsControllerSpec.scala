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

package controllers

import base.SpecBase
import controllers.actions.*
import models.OrganisationOrIndividual.Individual
import models.errors.ApiError.InternalServerError
import models.responses.{SubmitRcaspResponse, SubmitResponseDetails, SubmitReturnParameters}
import models.{ChangeMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import pages.combined.OrganisationOrIndividualPage
import pages.individual.IndividualNamePage
import pages.{RcaspIdPage, SubmissionSucceededPage}
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Call, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import services.SubmitRcaspService
import types.ResultT
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Key, SummaryListRow}
import utils.CheckDetailsHelper
import viewmodels.Section
import viewmodels.govuk.all.{ActionItemViewModel, FluentActionItem, SummaryListRowViewModel, ValueViewModel}
import views.html.CheckDetailsView

import java.time.Clock
import scala.concurrent.Future

class CheckDetailsControllerSpec extends SpecBase {

  private val testRow: SummaryListRow =
    SummaryListRowViewModel(
      key = Key(Text("TEST Key")),
      value = ValueViewModel(Text("TEST Value")),
      actions = Seq(
        ActionItemViewModel(
          Text("TEST Action"),
          controllers.individual.routes.IndividualNameController.onPageLoad(ChangeMode).url
        )
          .withVisuallyHiddenText("TEST HIDDEN TEXT")
      )
    )

  private val testSection: Section          = Section("TEST SECTION NAME", Seq(testRow))
  private val individualCompleteUserAnswers = emptyUserAnswers
    .withPage(OrganisationOrIndividualPage, Individual)
    .withPage(IndividualNamePage, testIndividualName)

  lazy val cdOnLoadRoute: String   = routes.CheckDetailsController.onPageLoad.url
  lazy val cdOnSubmitRoute: String = routes.CheckDetailsController.onSubmit.url
  def onwardRoute                  = Call("GET", "/foo")

  private val stubSubmitRcaspResponse =
    SubmitRcaspResponse(
      ResponseDetails = SubmitResponseDetails(
        ReturnParameters = SubmitReturnParameters(Key = "RCASPID", Value = rcaspId)
      )
    )

  "Check Details Controller" - {
    "onPageLoad" - {
      "when Individual as RCASP" - {
        "must return OK and the correct view for a GET when all answers have been answered" in new Setup(
          individualCompleteUserAnswers
        ) {

          when(mockCDAHelper.getIndividualSectionMaybe(eqTo(individualCompleteUserAnswers))(any()))
            .thenReturn(Some(testSection))

          when(mockCDAHelper.getContactDetails(eqTo(individualCompleteUserAnswers))(any()))
            .thenReturn(Some(testSection))

          val request                = FakeRequest(GET, cdOnLoadRoute)
          val view: CheckDetailsView = application.injector.instanceOf[CheckDetailsView]
          val result: Future[Result] = route(application, request).value

          status(result)          mustEqual OK
          contentAsString(result) mustEqual view(Seq(testSection, testSection), testIndividualName.fullName)(
            request,
            messages(application)
          ).toString
        }

        "must redirect to information is missing page OK and for GET when a section is none (answers missing)" in new Setup(
          individualCompleteUserAnswers
        ) {

          when(mockCDAHelper.getIndividualSectionMaybe(eqTo(individualCompleteUserAnswers))(any()))
            .thenReturn(None)

          when(mockCDAHelper.getContactDetails(eqTo(individualCompleteUserAnswers))(any()))
            .thenReturn(Some(testSection))

          val request                = FakeRequest(GET, cdOnLoadRoute)
          val view: CheckDetailsView = application.injector.instanceOf[CheckDetailsView]
          val result: Future[Result] = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual
            controllers.routes.InformationMissingController.onPageLoad().url
        }

        "must redirect to information is missing page OK and for GET when OrganisationOrIndividualPage is missing" in new Setup(
          emptyUserAnswers
        ) {

          val request                = FakeRequest(GET, cdOnLoadRoute)
          val view: CheckDetailsView = application.injector.instanceOf[CheckDetailsView]
          val result: Future[Result] = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual
            controllers.routes.InformationMissingController.onPageLoad().url
        }
      }

      /** TODO "When Organisation tests here" [CARF-295]
        */

      "must redirect to Journey Recovery for a GET if no existing data is found" in {
        val application = applicationBuilder(userAnswers = None).build()
        running(application) {
          val request = FakeRequest(GET, cdOnLoadRoute)
          val result  = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to the page unavailable placeholder for a GET when submission has already succeeded" in {
        val userAnswers = emptyUserAnswers.withPage(SubmissionSucceededPage, true)

        val application = new GuiceApplicationBuilder()
          .overrides(
            bind[DataRequiredAction].to[DataRequiredActionImpl],
            bind[IdentifierAction]
              .toInstance(new FakeIdentifierAction(injectedParsers, AffinityGroup.Individual, None)),
            bind[DataRetrievalAction].toInstance(new FakeDataRetrievalActionProvider(Some(userAnswers))),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, cdOnLoadRoute)
          val result  = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.PlaceholderController
            .onPageLoad("Should nav to /problem/page-unavailable (CARF-308)")
            .url
        }
      }
    }

    "onSubmit" - {
      "must store the rcaspId and redirect to the confirmation page when submission succeeds" in {
        val mockSubmitRcaspService = mock[SubmitRcaspService]

        when(mockSubmitRcaspService.submitRcasp()).thenReturn(ResultT.fromValue(stubSubmitRcaspResponse))
        when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

        val application =
          applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(bind[SubmitRcaspService].toInstance(mockSubmitRcaspService))
            .build()

        running(application) {
          val request = FakeRequest(POST, cdOnSubmitRoute)
          val result  = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.RcaspAddedConfirmationController.onPageLoad().url

          verify(mockSessionRepository).set(
            org.mockito.ArgumentMatchers.argThat((answers: UserAnswers) => answers.get(RcaspIdPage).contains(rcaspId))
          )
        }
      }

      "must redirect to Journey Recovery when the submit RCASP call fails" in {
        val mockSubmitRcaspService = mock[SubmitRcaspService]

        when(mockSubmitRcaspService.submitRcasp()).thenReturn(ResultT.fromError(InternalServerError))

        val application =
          applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(bind[SubmitRcaspService].toInstance(mockSubmitRcaspService))
            .build()

        running(application) {
          val request = FakeRequest(POST, cdOnSubmitRoute)
          val result  = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery for a POST if no existing data is found" in {
        val application = applicationBuilder(userAnswers = None).build()
        running(application) {
          val request = FakeRequest(POST, cdOnSubmitRoute)

          val result = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to the page unavailable placeholder for a POST when submission has already succeeded" in {
        val userAnswers = emptyUserAnswers.withPage(SubmissionSucceededPage, true)

        val application = new GuiceApplicationBuilder()
          .overrides(
            bind[DataRequiredAction].to[DataRequiredActionImpl],
            bind[IdentifierAction]
              .toInstance(new FakeIdentifierAction(injectedParsers, AffinityGroup.Individual, None)),
            bind[DataRetrievalAction].toInstance(new FakeDataRetrievalActionProvider(Some(userAnswers))),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, cdOnSubmitRoute)
          val result  = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.PlaceholderController
            .onPageLoad("Should nav to /problem/page-unavailable (CARF-308)")
            .url
        }
      }
    }
  }

  class Setup(userAnswers: UserAnswers) {
    final val mockCDAHelper = mock[CheckDetailsHelper]

    val application: Application =
      applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(
          bind[CheckDetailsHelper].toInstance(mockCDAHelper),
          bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
          bind[Clock].toInstance(clock)
        )
        .build()
  }

}
