package controllers

import models._
import play.api.mvc._

import scalaz.\/

class Application extends Controller {

  def index = Action {
    Ok(views.html.index(null))
  }

  def webhook = Action { request =>
    val decision: \/[_, Boolean] = for {
      pr <- PullRequest.buildFrom(request.body.asJson.get)
      comments <- Comment.getBy(pr)
      config <- PullRequestConfiguration.getBy(pr.owner, pr.projectName)
    } yield DecisionEngine.enoughReviews(comments, config)

    val state = if (decision.getOrElse(false)) PullRequestState.SUCCESS else PullRequestState.FAILURE

    val payload = PullRequestState.postState(state)

    Ok(payload.toString)
  }
}
