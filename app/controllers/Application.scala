package controllers

import models.{ApprovalsConfig, PullRequest}
import play.api.Logger
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.mvc._

//import play.api.Logger

// Combinator syntax


object Application extends Controller {

  // POST /repos/:owner/:repo/statuses/:sha

  def index = Action {
    val prId = 1
    val owner: String = "MiroslavCsonka"
    val projectName: String = "uCantMergeThisTestingRepo"
    PullRequest(prId, owner, projectName).getComments match {
      case Some(comments) => Ok(comments.map(x => (x.body, x.user)).mkString(", "))
      case _ => BadRequest("hups")
    }
  }

  def tick = Action {
    request =>
      //    val payload: String = request.body.asJson.mkString
      //    val json = Json.parse(payload)
      //    //
      //    //
      //    val prReads: Reads[PullRequest] = (
      //      (__ \ "pull_request" \ "id").read[Double] and
      //        (__ \ "repository" \ "owner" \ "login").read[String] and
      //        (__ \ "repository" \ "name").read[String]
      //      ) (PullRequest.apply _)
      //    //
      //    val prResult = json.validate(prReads)
      //
      //
      //    prResult match {
      //      case s: JsSuccess[PullRequest] => Logger.info("heyo " + s.get); Ok("Id: " + s.get)
      //      case e: JsError => BadRequest("Errors: " + e.errors.toString)
      //    }

      val prId = 1
      implicit val configReads: Reads[ApprovalsConfig] = new Reads[ApprovalsConfig] {
        def reads(json: JsValue): JsResult[ApprovalsConfig] = {
          for {
            regularExpressions <- (json \ "approvals" \ "regular_expressions").validate[List[String]]
            minimum <- (json \ "approvals" \ "minimum").validate[Double]
          } yield ApprovalsConfig(regularExpressions, minimum.toInt)
        }
      }
      val owner: String = "MiroslavCsonka"
      val projectName: String = "uCantMergeThisTestingRepo"
      val configFileName = "u-cant-merge-this.json"

      def getFile(owner: String, projectName: String, configFileName: String): Option[ApprovalsConfig] = {
        val configRaw = scala.io.Source.fromURL(s"https://raw.githubusercontent.com/$owner/$projectName/master/$configFileName").mkString
        val jsonConfig: JsValue = Json.parse(configRaw)
        val configResult: JsResult[ApprovalsConfig] = jsonConfig.validate[ApprovalsConfig](configReads)
        configResult.asOpt
      }




      //    def getComments(owner: String, projectName: String, prId: Integer): List[Comment] = {
      val url = s"https://api.github.com/repos/$owner/$projectName/issues/$prId/comments"
      val body = scala.io.Source.fromURL(url).mkString
      val jsComments = Json.parse(body)
      Logger.debug(jsComments.toString)
      //      val commentReads = (jsComments \ "body").as[String]

      //      maybeComments.get.asInstanceOf[List[Map[String, Any]]].map {
      //        x => Comment(x("body").asInstanceOf[String].trim)
      //      }
      //    }

      //    val comments = getComments(owner, projectName, prId)
      //
      //    var approvals = comments.filter {
      //      comment => regularExpressions.exists { r => comment.body.matches(r) }
      //    }
      //    if (approvals.length >= minimum) {
      //      "we made it"
      //    } else {
      //      "not yet"
      //    } tring, projectName: String, prId: Integer): List[Comment] = {
      //    val url = s"https://api.github.com/repos/$owner/$projectName/issues/$prId/comments"
      //    val body = scala.io.Source.fromURL(url).mkString
      //    val maybeComments = JSON.parseFull(body)
      //    maybeComments.get.asInstanceOf[List[Map[String, Any]]].map {
      //      x => Comment(x("body").asInstanceOf[String].trim)
      //    }
      //  }
      //
      //    val configJson = JSON.parseFull(configRaw)
      //    val config = configJson.get.asInstanceOf[Map[String, Any]]
      //    val approvalsConfig = config.get("approvals").get.asInstanceOf[Map[String, Any]]
      //    val regularExpressions = approvalsConfig.get("regular_expressions").get.asInstanceOf[List[String]]
      //    val minimum = approvalsConfig.getOrElse("minimum", 6.0).asInstanceOf[Double].toInt
      //    val comments = getComments(owner, projectName, prId)
      //    var approvals = comments.filter {
      //      comment => regularExpressions.exists { r => comment.body.matches(r) }
      //    }


      //    getFile(owner, projectName, configFileName) match {
      //      case s: Some[ApprovalsConfig] => Ok(s.get.minimum.toString)
      //      case None => BadRequest("hups")
      //    }
      Ok(body)

  }
}
