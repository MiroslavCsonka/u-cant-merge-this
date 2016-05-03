package models

import play.api.data.validation.ValidationError
import play.api.libs.functional.syntax._
import play.api.libs.json._

import scalaj.http.Http
import scalaz._
import scalaz.Scalaz._

case class ApprovalsConfig(regular_expressions: List[String], minimum: Integer)

case class Comment(body: String, user: String)

case class PullRequestState(context: String, state: String)

object PullRequestState {
  val SUCCESS = PullRequestState("uCantMergeThis", "success")
  val FAILURE = PullRequestState("uCantMergeThis", "failure")

  def postState(state: PullRequestState): (Int, String) = {
    implicit val statusWrites: Writes[PullRequestState] = (
      (JsPath \ "context").write[String] and
        (JsPath \ "state").write[String]
      ) (unlift(PullRequestState.unapply))

    val statusesUrl: String = "https://api.github.com/repos/uCantMergeThis/uCantMergeThisTestingRepo/statuses/a7f4cd021aaadeac5cd6dcc21d7326c5ddffb1dd"
    val payload = Json.toJson(state).toString

    val code = Http(statusesUrl)
      .header("content-type", "application/json")
      .header("Authorization", "token 645e6219572ed6ffeb2d8d4e674fed583b978eb5")
      .postData(payload)
      .asString
      .code

    (code, payload.toString)
  }

  def getBy(owner: String, repo: String, commitSha: String): Seq[(JsPath, Seq[ValidationError])] \/ List[PullRequestState] = {
    val url = s"https://api.github.com/repos/$owner/$repo/commits/$commitSha/statuses"

    val x = Http(url)
      .header("content-type", "application/json")
      .header("Authorization", "token 645e6219572ed6ffeb2d8d4e674fed583b978eb5")
      .asString

    implicit val configReads: Reads[PullRequestState] = new Reads[PullRequestState] {
      def reads(json: JsValue): JsResult[PullRequestState] = {
        for {
          context <- (json \ "context").validate[String]
          status <- (json \ "state").validate[String]
        } yield PullRequestState(context, status)
      }
    }

    Json.parse(x.body).validate[List[PullRequestState]].asEither.disjunction
  }
}

object Comment {
  def getBy(pullRequest: PullRequest): Seq[(JsPath, Seq[ValidationError])] \/ List[Comment] = {
    val url = s"https://api.github.com/repos/${pullRequest.owner}/${pullRequest.projectName}/issues/${pullRequest.number}/comments"
    implicit val commentsReader: Reads[Comment] = (
      (__ \ "body").read[String] and
        (__ \ "user" \ "login").read[String]
      ) (Comment.apply _)
    val body = scala.io.Source.fromURL(url).mkString
    val jsComments = Json.parse(body)
    jsComments.validate[List[Comment]].asEither.disjunction
  }
}

object DecisionEngine {
  private val defaultFilter = (comment: Comment) => true

  def enoughReviews(comments: List[Comment], config: ApprovalsConfig): Boolean = {
    val regularExpressionFilter = (c: Comment) => config.regular_expressions.exists { r => c.body.trim.matches(r) }
    val filterFunction = if (config.regular_expressions.isEmpty) defaultFilter else regularExpressionFilter
    val positiveReviews = comments filter filterFunction
    positiveReviews.length >= config.minimum
  }
}


object PullRequestConfiguration {
  def getBy(owner: String, projectName: String): Seq[(JsPath, Seq[ValidationError])] \/ ApprovalsConfig = {
    implicit val configReads: Reads[ApprovalsConfig] = new Reads[ApprovalsConfig] {
      def reads(json: JsValue): JsResult[ApprovalsConfig] = {
        for {
          regularExpressions <- (json \ "approvals" \ "regular_expressions").validate[List[String]]
          minimum <- (json \ "approvals" \ "minimum").validate[Double]
        } yield ApprovalsConfig(regularExpressions, minimum.toInt)
      }
    }

    val configRaw = scala.io.Source.fromURL(s"https://raw.githubusercontent.com/$owner/$projectName/master/u-cant-merge-this.json").mkString
    val jsonConfig: JsValue = Json.parse(configRaw)
    jsonConfig.validate[ApprovalsConfig].asEither.disjunction
  }
}