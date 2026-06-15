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

package models

import base.SpecBase
import play.api.i18n.Messages
import viewmodels.HomePageViewModel

class HomePageViewModelSpec extends SpecBase {

  val basicViewModel: HomePageViewModel = HomePageViewModel(
    isBusiness = true,
    hasZeroRcaspsAdded = true,
    hasSentFilesInLast28Days = true,
    organisationName = Some("Timmy Ltd"),
    ctUtr = Some(testUtr),
    carfId = testCarfId
  )

  implicit val implicitMessages: Messages = messages(app)

  "HomePageViewModel" - {

    "getNoRcaspsSectionText method" - {
      "must return None when there are more than one Rcasps added" in {
        val viewModel = basicViewModel.copy(hasZeroRcaspsAdded = false)
        val result    = viewModel.getNoRcaspsSectionText()

        result mustBe None
      }
      "must return a tuple of messages when there are zero Rcasps added" in {
        val viewModel = basicViewModel
        val result    = viewModel.getNoRcaspsSectionText()
        val message1  = "To send a report, you must"
        val message2  = "add a reporting cryptoasset service provider (RCASP)"

        result mustBe Some(message1, message2)
      }
    }

    "getNoRcaspsSectionLink val" - {
      "must return the url for the routing controller" in {
        val viewModel   = basicViewModel
        val result      = viewModel.getNoRcaspsSectionLink
        val expectedUrl = controllers.routes.RoutingController.onPageLoad(NormalMode).url

        result mustBe expectedUrl
      }
    }

    "getUploadXmlLink val" - {
      "must return the url for the upload xml link" in {
        val viewModel   = basicViewModel
        val result      = viewModel.getUploadXmlLink
        val expectedUrl = controllers.routes.PlaceholderController
          .onPageLoad("Should redirect to /report/upload-file (part of file upload journey)")
          .url

        result mustBe expectedUrl
      }
    }

    "getViewResultsLink val" - {
      "must return the url for the view results page" in {
        val viewModel   = basicViewModel
        val result      = viewModel.getViewResultsLink
        val expectedUrl = controllers.routes.PlaceholderController
          .onPageLoad("Should redirect to /result-of-automatic-checks (part of file upload journey)")
          .url

        result mustBe expectedUrl
      }
    }

    "getAddRcaspLink val" - {
      "must return the url for the routing controller" in {
        val viewModel   = basicViewModel
        val result      = viewModel.getAddRcaspLink
        val expectedUrl = controllers.routes.RoutingController.onPageLoad(NormalMode).url

        result mustBe expectedUrl
      }
    }

    "getManageRcaspsLink val" - {
      "must return the url for the manage rcasps page" in {
        val viewModel   = basicViewModel
        val result      = viewModel.getManageRcaspsLink
        val expectedUrl = controllers.routes.PlaceholderController
          .onPageLoad("Should redirect to /manage-your-rcasps/your-rcasps (part of management change journey)")
          .url

        result mustBe expectedUrl
      }
    }

    "getContactDetailsSectionHeading method" - {
      "must return the org version of the message when model is an org" in {
        val viewModel  = basicViewModel
        val result     = viewModel.getContactDetailsSectionHeading()
        val messageKey = "Contact details"

        result mustBe messageKey
      }
      "must return the ind version of the message when model is an ind" in {
        val viewModel  = basicViewModel.copy(isBusiness = false)
        val result     = viewModel.getContactDetailsSectionHeading()
        val messageKey = "Your contact details"

        result mustBe messageKey
      }
    }

    "getContactDetailsSectionLinkText method" - {
      "must return the org version of the link text when model is an org" in {
        val viewModel   = basicViewModel
        val result      = viewModel.getContactDetailsSectionLinkText()
        val linkTextKey = "Change the contact details for Timmy Ltd"

        result mustBe linkTextKey
      }
      "must use the default value when model is an org but no organisation name is present" in {
        val viewModel   = basicViewModel.copy(organisationName = None)
        val result      = viewModel.getContactDetailsSectionLinkText()
        val linkTextKey = "Change the contact details for your business"

        result mustBe linkTextKey
      }
      "must return the ind version of the link text when model is an ind" in {
        val viewModel   = basicViewModel.copy(isBusiness = false)
        val result      = viewModel.getContactDetailsSectionLinkText()
        val linkTextKey = "Change your contact details"

        result mustBe linkTextKey
      }
    }
  }
}
