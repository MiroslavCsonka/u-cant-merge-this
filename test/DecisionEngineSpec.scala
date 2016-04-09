import models.{ApprovalsConfig, DecisionEngine, Comment}
import org.scalatestplus.play._

class DecisionEngineSpec extends PlaySpec {
  val decisionEngine = new DecisionEngine
  val noComments = List()
  val positiveComment = Comment("nice", "MiroslavCsonka")

  def decision(comments: List[Comment], config: ApprovalsConfig) = decisionEngine.enoughReviews(comments, config)

  "DecisionEngine" must {
    "approve" when {
      "minimum reviews are 0" in {
        decision(noComments, ApprovalsConfig(List(), 0)) mustBe true
      }

      "someone commented and" when {
        "no patterns are supplied" in {
          decision(List(positiveComment), ApprovalsConfig(List(), 1)) mustBe true
        }
      }
    }

    "decline" when {
      "no one commented" in {
        decision(noComments, ApprovalsConfig(List(), 1)) mustBe false
      }

      "someone commented and" when {
        "none of patterns match" in {
          decision(List(positiveComment), ApprovalsConfig(List("^nope$"), 1)) mustBe false
        }
      }
    }
  }

  //  "A Stack" must {
  //    "pop values in last-in-first-out order" in {
  //      val stack = new mutable.Stack[Int]
  //      stack.push(1)
  //      stack.push(2)
  //      stack.pop() mustBe 2
  //      stack.pop() mustBe 1
  //    }
  //    "throw NoSuchElementException if an empty stack is popped" in {
  //      val emptyStack = new mutable.Stack[Int]
  //      a[NoSuchElementException] must be thrownBy {
  //        emptyStack.pop()
  //      }
  //    }
  //  }
}