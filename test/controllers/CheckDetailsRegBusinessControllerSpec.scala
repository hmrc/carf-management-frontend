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
import models.UserAnswers
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import pages.organisation.OverwritableOrganisationName
import play.api.Application
import play.api.inject.bind
import play.api.mvc.{Call, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Key, SummaryListRow}
import utils.CheckDetailsRegBusinessHelper
import viewmodels.Section
import viewmodels.govuk.all.{ActionItemViewModel, FluentActionItem, SummaryListRowViewModel, ValueViewModel}
import views.html.organisation.CheckDetailsRegBusinessView
import models.errors.InternalServerError
import services.RegistrationService
import types.ResultT

import java.time.Clock
import scala.concurrent.Future

class CheckDetailsRegBusinessControllerSpec extends SpecBase {

  private val testRow: SummaryListRow =
    SummaryListRowViewModel(
      key = Key(Text("TEST Key")),
      value = ValueViewModel(Text("TEST Value")),
      actions = Seq(
        ActionItemViewModel(
          Text("TEST Action"),
          controllers.organisation.routes.RegisteredBusinessIsThisYourBusinessNameController
            .onPageLoad(models.ChangeMode)
            .url
        )
          .withVisuallyHiddenText("TEST HIDDEN TEXT")
      )
    )

  private val testSection: Section = Section("TEST SECTION NAME", Seq(testRow))

  lazy val cdRoute: String = controllers.routes.CheckDetailsRegBusinessController.onPageLoad.url
  def onwardRoute: Call    = Call("GET", "/foo")

  "CheckDetailsRegBusiness Controller" - {

    "onPageLoad" - {

      "must return OK and the correct view for a GET when all answers are present" in new Setup(
        emptyUserAnswers.withPage(OverwritableOrganisationName, "Test Business Ltd")
      ) {
        when(
          mockHelper.getRegisteredBusinessSection(
            eqTo(emptyUserAnswers.withPage(OverwritableOrganisationName, "Test Business Ltd"))
          )(any())
        )
          .thenReturn(Some(testSection))

        val request                = FakeRequest(GET, cdRoute)
        val view                   = application.injector.instanceOf[CheckDetailsRegBusinessView]
        val result: Future[Result] = route(application, request).value

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(Seq(testSection), "Test Business Ltd")(
          request,
          messages(application)
        ).toString
      }

      "must redirect to InformationMissing when section is None" in new Setup(
        emptyUserAnswers.withPage(OverwritableOrganisationName, "Test Business Ltd")
      ) {
        when(mockHelper.getRegisteredBusinessSection(any())(any()))
          .thenReturn(None)

        val request                = FakeRequest(GET, cdRoute)
        val result: Future[Result] = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.InformationMissingController.onPageLoad().url
      }

      "must redirect to InformationMissing when OverwritableOrganisationName is missing" in new Setup(
        emptyUserAnswers
      ) {
        val request                = FakeRequest(GET, cdRoute)
        val result: Future[Result] = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.InformationMissingController.onPageLoad().url
      }

      "must redirect to Journey Recovery for a GET if no existing data is found" in {
        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(GET, cdRoute)
          val result  = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    "onSubmit" - {

      "must redirect to Placeholder page on success" in new Setup(
        emptyUserAnswers
      ) {
        when(mockRegistrationService.registerRcasp(any()))
          .thenReturn(ResultT.fromValue(()))

        val request                = FakeRequest(POST, cdRoute)
        val result: Future[Result] = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          controllers.routes.PlaceholderController
            .onPageLoad("[CARF-296] RCASP added page - /rcasp-added")
            .url
      }

      "must redirect to Journey Recovery on failure" in new Setup(
        emptyUserAnswers
      ) {
        when(mockRegistrationService.registerRcasp(any()))
          .thenReturn(ResultT.fromError(InternalServerError))

        val request                = FakeRequest(POST, cdRoute)
        val result: Future[Result] = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }

      "must redirect to Journey Recovery for a POST if no existing data is found" in {
        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(POST, cdRoute)
          val result  = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }
  }

  class Setup(userAnswers: UserAnswers) {
    final val mockHelper: CheckDetailsRegBusinessHelper    = mock[CheckDetailsRegBusinessHelper]
    final val mockRegistrationService: RegistrationService = mock[RegistrationService]

    val application: Application =
      applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(
          bind[CheckDetailsRegBusinessHelper].toInstance(mockHelper),
          bind[RegistrationService].toInstance(mockRegistrationService),
          bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
          bind[Clock].toInstance(clock)
        )
        .build()
  }
}
