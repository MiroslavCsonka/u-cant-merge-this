package models

// you need this import to have combinators
import play.api.libs.functional.syntax._
import play.api.libs.json._

case class PullRequest(id: Double, owner: String, projectName: String) {
  implicit val commentsReader: Reads[Comment] = (
    (__ \ "body").read[String] and
      (__ \ "user" \ "login").read[String]
    ) (Comment)

  def getComments: Option[List[Comment]] = {
    val url = s"https://api.github.com/repos/$owner/$projectName/issues/$id/comments"
    val body = scala.io.Source.fromURL(url).mkString
    val jsComments = Json.parse(body)
    jsComments.validate[List[Comment]].asOpt
  }

  def enoughReviews(config: Option[ApprovalsConfig]): Boolean = true
}

case class ApprovalsConfig(regular_expressions: List[String], minimum: Integer)

case class Comment(body: String, user: String)

class DecisionEngine {
  def enoughReviews(comments: List[Comment], maybeConfig: ApprovalsConfig): Boolean = {
    val positiveReviews = if (maybeConfig.regular_expressions.isEmpty) {
      comments
    } else {
      comments.filter { c: Comment =>
        maybeConfig.regular_expressions.exists { r => c.body.matches(r) }
      }
    }
    positiveReviews.length >= maybeConfig.minimum
  }
}