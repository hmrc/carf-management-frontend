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

package testOnly.controllers

import connectors.RcaspConnector
import models.requests.*
import models.responses.{SubmitRcaspResponse, ViewRcaspResponse}
import models.{RcaspAddress, RcaspContactDetails, TinDetails}
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import types.ResultT
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RcaspController @Inject() (
    connector: RcaspConnector,
    val controllerComponents: MessagesControllerComponents,
    implicit val executionContext: ExecutionContext
) extends FrontendBaseController
    with I18nSupport {

  def viewRcasp(carfId: String): Action[AnyContent] = Action.async { implicit request =>
    connector.viewRcasp(carfId).processResponse(data => Ok(Json.prettyPrint(Json.toJson(data))))
  }

  def createRcasp(email: String, isIndividual: String): Action[AnyContent] = Action.async { implicit request =>

    val fullAddress = RcaspAddress(
      AddressLine1 = "2 High Street",
      AddressLine2 = Some("Birmingham"),
      AddressLine3 = Some("Nowhereshire"),
      AddressLine4 = Some("Down the road"),
      PostalCode = "B23 2AZ",
      CountryCode = "GB"
    )

    val tinDetails = TinDetails(
      TINType = "UTR",
      TIN = "6893649",
      IssuedBy = "GB"
    )

    val createRcaspRequest: CreateRcaspRequest =
      CreateRcaspRequest(
        RCASPManagementRequest(
          RcaspCreateRequestCommon(
            OriginatingSystem = "MDTP",
            TransmittingSystem = "EIS",
            RequestType = "CREATE",
            Regime = "CARF",
            RequestParameters = None
          ),
          if (isIndividual.toBoolean) {
            IndividualRcaspDetails(
              SubscriptionID = "XCARF000000001",
              IsRCASPUser = true,
              PartyType = "Individual",
              FirstName = "Penny",
              LastName = "Cassiopeia",
              TINDetails = Some(
                List(
                  tinDetails
                )
              ),
              AddressDetails = fullAddress,
              PrimaryContactDetails = Some(
                RcaspContactDetails(
                  ContactName = "Penny Cassiopeia",
                  EmailAddress = email,
                  PhoneNumber = Some("07123412345")
                )
              )
            )
          } else {
            OrganisationRcaspDetails(
              SubscriptionID = "XCARF000000001",
              IsRCASPUser = true,
              PartyType = "Organisation",
              RCASPName = "Mesagoza",
              TradingName = "Uva Academy",
              TINDetails = Some(List(tinDetails)),
              AddressDetails = fullAddress,
              PrimaryContactDetails = Some(
                RcaspContactDetails(
                  ContactName = "Clavell",
                  EmailAddress = email,
                  PhoneNumber = Some("07123412344")
                )
              ),
              SecondaryContactDetails = Some(
                RcaspContactDetails(
                  ContactName = "Jacq",
                  EmailAddress = "jacq@uva.edu.org",
                  PhoneNumber = Some("07123412345")
                )
              )
            )
          }
        )
      )

    connector.createRcasp(createRcaspRequest).processResponse(data => Ok(Json.prettyPrint(Json.toJson(data))))
  }

  extension [T](result: ResultT[T]) {
    private def processResponse(f: T => Result): Future[Result] =
      result.value
        .map {
          case Right(data) => f(data)
          case Left(error) => Ok(error.toString)
        }
  }
}
