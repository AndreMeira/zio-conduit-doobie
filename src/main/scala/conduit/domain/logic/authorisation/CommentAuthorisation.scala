package conduit.domain.logic.authorisation

import conduit.domain.model.entity.User
import conduit.domain.model.error.ApplicationError.{ TransientError, UnauthorisedError }
import conduit.domain.model.request.CommentRequest
import zio.ZIO

trait CommentAuthorisation[Tx] extends Authorisation[Tx, CommentRequest, CommentAuthorisation.Failure]

object CommentAuthorisation:
  enum Failure extends UnauthorisedError:
    case CanNotDeleteComment(reason: String)
    case CanNotCreateComment(reason: String)
    case CanNotViewComments(reason: String)

    override def message: String = this match
      case CanNotDeleteComment(reason) => s"Can not delete comment: $reason"
      case CanNotCreateComment(reason) => s"Can not create comment: $reason"
      case CanNotViewComments(reason)  => s"Can not view comments: $reason"
