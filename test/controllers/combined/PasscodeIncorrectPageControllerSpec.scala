package controllers.combined

import base.SpecBase
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.combined.PasscodeIncorrectPageView

class PasscodeIncorrectPageControllerSpec extends SpecBase {

  "PasscodeIncorrectPage Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, controllers.combined.routes.PasscodeIncorrectPageController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[PasscodeIncorrectPageView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view()(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, controllers.combined.routes.PasscodeIncorrectPageController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
