package controllers.combined

import base.SpecBase
import config.FrontendAppConfig
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.combined.PasscodeIncorrectPageView
import play.api.inject.bind

class PasscodeIncorrectPageControllerSpec extends SpecBase {

  val mockAppConfig: FrontendAppConfig = mock[FrontendAppConfig]

  "PasscodeIncorrectPage Controller" - {

    "must return OK and the correct view for a GET" in {
      when(mockAppConfig.carfDigitalEmailAddress).thenReturn(testEmail)
      when(mockAppConfig.feedbackUrl(any())).thenReturn("foo")

      val application = applicationBuilder(userAnswers = None)
        .overrides(bind[FrontendAppConfig].toInstance(mockAppConfig))
        .build()

      running(application) {
        val request = FakeRequest(GET, controllers.combined.routes.PasscodeIncorrectPageController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[PasscodeIncorrectPageView]

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(testEmail)(request, messages(application)).toString
      }
    }
  }
}
