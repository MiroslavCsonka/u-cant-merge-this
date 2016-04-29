package controllers

import models._
import play.api.Logger
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.mvc._
import scalaj.http._
import scalaj.http.Http._

object Application extends Controller {

  def index = Action {
    val owner = "uCantMergeThis"
    val repo = "uCantMergeThisTestingRepo"
    val head = "a7f4cd021aaadeac5cd6dcc21d7326c5ddffb1dd"

    val maybeStatuses = getPullRequestStatuses(owner, repo, head)

    maybeStatuses match {
      case JsSuccess(statuses, _) => Ok(statuses.map { s => (s.context, s.state) }.mkString(", "))
      case JsError(e) => Ok(e.toString)
    }
  }

  def getPullRequestStatuses(owner: String, repo: String, commitSha: String) = {
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

    Json.parse(x.body).validate[List[PullRequestState]]
  }


  def getPullRequest(json: JsValue): Option[PullRequest] = {
    val prReads: Reads[PullRequest] = (
      (__ \ "issue" \ "number").read[Double] and
        (__ \ "repository" \ "owner" \ "login").read[String] and
        (__ \ "repository" \ "name").read[String]
      ) (PullRequest.apply _)
    Logger.info(json.toString)
    val x = json.validate(prReads).asOpt
    Logger.info("Did parse PR " + x.isDefined.toString)
    x
  }

  def getComments(pr: PullRequest): Option[List[Comment]] = {
    val url = s"https://api.github.com/repos/${pr.owner}/${pr.projectName}/issues/${pr.number}/comments"
    implicit val commentsReader: Reads[Comment] = (
      (__ \ "body").read[String] and
        (__ \ "user" \ "login").read[String]
      ) (Comment.apply _)
    val body = scala.io.Source.fromURL(url).mkString
    val jsComments = Json.parse(body)
    jsComments.validate[List[Comment]].asOpt
  }

  def getFile(owner: String, projectName: String): Option[ApprovalsConfig] = {
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
    val configResult: JsResult[ApprovalsConfig] = jsonConfig.validate[ApprovalsConfig](configReads)
    configResult.asOpt
  }

  def postPullRequestState(state: PullRequestState): (Int, String) = {
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

  def tick = Action {
    request =>
      Logger.info(request.body.toString)

      val decision: Option[Boolean] = for {
        pr <- getPullRequest(request.body.asJson.get)
        comments <- getComments(pr)
        config <- getFile(pr.owner, pr.projectName)
      } yield DecisionEngine.enoughReviews(comments, config)

      val state = if (decision.exists(identity)) PullRequestState.SUCCESS else PullRequestState.FAILURE

      val payload = postPullRequestState(state)

      Ok(payload.toString)
  }
}

