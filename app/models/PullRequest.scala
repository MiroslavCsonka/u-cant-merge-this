package models

import play.api.data.validation.ValidationError
import play.api.libs.functional.syntax._
import play.api.libs.json._

import scalaz._
import scalaz.Scalaz._

case class PullRequest(number: Double, owner: String, projectName: String)

object PullRequest {
  def buildFrom(json: JsValue): Seq[(JsPath, Seq[ValidationError])] \/ PullRequest = {
    val prReads: Reads[PullRequest] = (
      (__ \ "issue" \ "number").read[Double] and
        (__ \ "repository" \ "owner" \ "login").read[String] and
        (__ \ "repository" \ "name").read[String]
      ) (PullRequest.apply _)
    json.validate(prReads).asEither.disjunction
  }
}

