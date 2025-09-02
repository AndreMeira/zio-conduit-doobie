package conduit.domain.logic.authorisation

import conduit.domain.logic.authorisation
import conduit.domain.model.entity.{ Article, Comment, User, UserProfile }
import conduit.domain.model.error.ApplicationError.{ TransientError, UnauthorisedError }
import conduit.domain.model.types.article.ArticleId
import conduit.domain.model.types.comment.CommentId
import conduit.domain.model.types.user.UserId
import zio.ZIO

trait Authorisation[Tx, A, E <: UnauthorisedError] {
  protected type Allowed = Authorisation.Result[E]
  protected type Result  = ZIO[Tx, TransientError, Allowed]

  def authorise(request: A): Result
  def allowed: Result = ZIO.succeed(Authorisation.Result.Allowed)
}

object Authorisation:
  enum Result[+E <: UnauthorisedError]:
    case Allowed
    case NotAllowed(reason: E)
