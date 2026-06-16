package controllers

import controllers.actions.*
import forms.FindAddressFormProvider
import models.Mode
import navigation.Navigator
import pages.FindAddressPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.sectionName.FindAddressView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FindAddressController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       sessionRepository: SessionRepository,
                                       navigator: Navigator,
                                       identify: IdentifierAction,
                                       getData: DataRetrievalAction,
                                       requireData: DataRequiredAction,
                                       formProvider: FindAddressFormProvider,
                                       val controllerComponents: MessagesControllerComponents,
                                       view: FindAddressView
                                    )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form: Form[String] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify() andThen getData() andThen requireData) {
    implicit request =>

      val preparedForm = request.userAnswers.get(FindAddressPage).fold(form)(form.fill)

      Ok(view(preparedForm, mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify() andThen getData() andThen requireData).async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, mode))),

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(FindAddressPage, value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(FindAddressPage, mode, updatedAnswers))
      )
  }
}
