package controllers

import base.SpecBase
import models.NormalMode
import org.apache.pekko.http.scaladsl.model.HttpHeader.ParsingResult.Ok
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.InformationMissingView

class InformationMissingControllerSpec extends SpecBase {
  
  "InformationMissing Controller" - {
    
    "must return OK and correct view for GET" in {
      
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
      
      running(application) {
        val request = FakeRequest(GET, routes.InformationMissingController.onPageLoad().url)
        
        val result = route(application, request).value
        
        val view = application.injector.instanceOf[InformationMissingView]
        
        status(result) mustEqual OK
        contentAsString(result) mustEqual view(routes.RoutingController.onPageLoad(NormalMode).url)(
          request,
          messages(application)
        ).toString
      }
    }
  }

}
