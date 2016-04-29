package models

// you need this import to have combinators
import play.api.libs.functional.syntax._
import play.api.libs.json._

case class PullRequest(number: Double, owner: String, projectName: String)

case class ApprovalsConfig(regular_expressions: List[String], minimum: Integer)

case class Comment(body: String, user: String)

case class PullRequestState(context: String, state: String)

object PullRequestState {
  val SUCCESS = PullRequestState("uCantMergeThis", "success")
  val FAILURE = PullRequestState("uCantMergeThis", "failure")
}

object DecisionEngine {
  def enoughReviews(comments: List[Comment], maybeConfig: ApprovalsConfig): Boolean = {
    val positiveReviews = if (maybeConfig.regular_expressions.isEmpty) {
      comments
    } else {
      comments.filter { c: Comment =>
        maybeConfig.regular_expressions.exists { r => c.body.trim.matches(r) }
      }
    }
    positiveReviews.length >= maybeConfig.minimum
  }
}