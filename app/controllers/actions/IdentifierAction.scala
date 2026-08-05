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

package controllers.actions

import config.FrontendAppConfig
import controllers.routes
import models.IdentifierType
import models.requests.IdentifierRequest
import play.api.mvc.*
import play.api.mvc.Results.*
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{affinityGroup, allEnrolments, internalId}
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.{HeaderCarrier, UnauthorizedException}
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

trait IdentifierAction {
  def apply(
      redirect: Boolean = true
  ): ActionBuilder[IdentifierRequest, AnyContent] with ActionFunction[Request, IdentifierRequest]
}

class AuthenticatedIdentifierAction @Inject() (
    override val authConnector: AuthConnector,
    config: FrontendAppConfig,
    val parser: BodyParsers.Default
)(implicit val executionContext: ExecutionContext)
    extends IdentifierAction
    with AuthorisedFunctions {

  override def apply(
      redirect: Boolean = true
  ): ActionBuilder[IdentifierRequest, AnyContent] with ActionFunction[Request, IdentifierRequest] =
    new AuthenticatedIdentifierActionWithRegime(authConnector, config, parser, redirect)
}

class AuthenticatedIdentifierActionWithRegime @Inject() (
    override val authConnector: AuthConnector,
    config: FrontendAppConfig,
    val parser: BodyParsers.Default,
    val redirect: Boolean
)(implicit val executionContext: ExecutionContext)
    extends ActionBuilder[IdentifierRequest, AnyContent]
    with ActionFunction[Request, IdentifierRequest]
    with AuthorisedFunctions {

  private def enrolmentKey: String = config.enrolmentKey

  override def invokeBlock[A](request: Request[A], block: IdentifierRequest[A] => Future[Result]): Future[Result] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    authorised().retrieve(
      internalId and allEnrolments and affinityGroup
    ) {
      case _ ~ _ ~ Some(Agent)                                 =>
        Future.successful(Redirect(config.registrationUrl))
      case Some(internalID) ~ enrolments ~ Some(affinityGroup) =>
        handleEnrolmentCheck(request, block, internalID, enrolments, affinityGroup)
      case _                                                   =>
        throw new UnauthorizedException("Failed to retrieve valid auth data")
    } recover {
      case _: NoActiveSession        =>
        Redirect(config.loginUrl, Map("continue" -> Seq(config.loginContinueUrl)))
      case _: AuthorisationException =>
        Redirect(routes.UnauthorisedController.onPageLoad())
    }
  }

  private def handleEnrolmentCheck[A](
      request: Request[A],
      block: IdentifierRequest[A] => Future[Result],
      internalID: String,
      enrolments: Enrolments,
      affinityGroup: AffinityGroup
  ): Future[Result] =
    if (enrolments.enrolments.exists(_.key == enrolmentKey) && redirect) {
      getCarfId(enrolments) match {
        case Some(carfId) =>
          block(IdentifierRequest(request, internalID, affinityGroup, enrolments.enrolments, carfId))
        case None         => Future.successful(Redirect(config.registrationUrl))
      }
    } else {
      Future.successful(Redirect(config.registrationUrl))
    }

  private def getCarfId(
      enrolments: Enrolments
  ): Option[String] =
    for {
      enrolment <- enrolments.getEnrolment(config.enrolmentKey)
      id        <- enrolment.getIdentifier(IdentifierType.CARFID)
      carfId    <- if (id.value.nonEmpty) Some(id.value) else None
    } yield carfId

}
