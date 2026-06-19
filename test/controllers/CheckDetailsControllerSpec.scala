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
import models.OrganisationOrIndividual.Individual
import models.{ChangeMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import pages.combined.OrganisationOrIndividualPage
import pages.individual.IndividualNamePage
import play.api.Application
import play.api.http.*
import play.api.inject.bind
import play.api.mvc.{Call, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
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

  lazy val cdRoute: String = routes.CheckDetailsController.onPageLoad.url
  def onwardRoute          = Call("GET", "/foo")

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

          val request                = FakeRequest(GET, cdRoute)
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

          val request                = FakeRequest(GET, cdRoute)
          val view: CheckDetailsView = application.injector.instanceOf[CheckDetailsView]
          val result: Future[Result] = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual
            controllers.routes.PlaceholderController.onPageLoad("[CARF-293] Some Information is missing page").url
        }

        "must redirect to information is missing page OK and for GET when OrganisationOrIndividualPage is missing" in new Setup(
          emptyUserAnswers
        ) {

          val request                = FakeRequest(GET, cdRoute)
          val view: CheckDetailsView = application.injector.instanceOf[CheckDetailsView]
          val result: Future[Result] = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual
            controllers.routes.PlaceholderController.onPageLoad("[CARF-293] Some Information is missing page").url
        }
      }

      /** TODO "When Organisation tests here" [CARF-295]
        */

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
      "should redirect to Placeholder page" in {
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
        running(application) {
          val request = FakeRequest(POST, cdRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          controllers.routes.PlaceholderController.onPageLoad("[CARF-296] RCASP added page - /rcasp-added").url
        }
      }

      "must redirect to Journey Recovery for a POST if no existing data is found" in {
        val application = applicationBuilder(userAnswers = None).build()
        running(application) {
          val request = FakeRequest(POST, cdRoute)

          val result = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
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
