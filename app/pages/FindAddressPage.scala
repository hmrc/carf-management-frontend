package pages

import pages.QuestionPage
import play.api.libs.json.JsPath

case object FindAddressPage extends QuestionPage[String] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "findAddress"
}
