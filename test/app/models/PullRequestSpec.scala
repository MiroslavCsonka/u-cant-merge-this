package app.models

import models.PullRequest
import org.scalatestplus.play._
import play.api.libs.json.{Json, JsValue, JsPath}
import play.api.libs.json._

import scalaz.\/-

import play.api.libs.json._

class PullRequestSpec extends PlaySpec {

  "PullRequest" must {
    "buildFrom" when {
      "inputs are valid" in {

        val json: JsValue = Json.parse("""
{
  "issue": {
    "number": 12
  },
  "repository": {
    "owner":{
      "login": "Mirek"
    },
    "name": "Wohoo"
  }
}""")

       PullRequest.buildFrom(json) mustBe \/-(PullRequest(12, "Mirek", "Wohoo"))
      }
    }

  }
}