package conduit.domain.service.authorisation

import conduit.domain.logic.authorisation.{ Authorisation, CommentAuthorisation }
import conduit.domain.logic.authorisation.CommentAuthorisation.Failure
import conduit.domain.logic.monitoring.Monitor
import conduit.domain.logic.persistence.CommentRepository
import conduit.domain.model.entity.User
import conduit.domain.model.request.CommentRequest
import conduit.domain.model.request.comment.{ AddCommentRequest, DeleteCommentRequest, ListCommentsRequest }
import conduit.domain.model.types.article.ArticleSlug
import conduit.domain.model.types.comment.{ CommentAuthorId, CommentId }
import zio.ZIO

class CommentAuthorisationService[Tx](
  monitor: Monitor,
  comments: CommentRepository[Tx],
) extends CommentAuthorisation[Tx] {

  override def authorise(request: CommentRequest): Result =
    monitor.track("CommentAuthorisationService.authorise") {
      request match
        case request: DeleteCommentRequest => canDeleteComment(request)
        case _: ListCommentsRequest        => allowed // Any user (authenticated or not) can view comments
        case _: AddCommentRequest          => allowed // Any authenticated user can create comments
    }

  private def canDeleteComment(request: DeleteCommentRequest): Result = {
    val author = CommentAuthorId(request.requester.userId)
    val reason = s"Comment ${request.comment} does not belong to user ${request.requester.userId}"
    comments.exists(request.comment, author).map {
      case true  => Authorisation.Result.Allowed
      case false => Authorisation.Result.NotAllowed(Failure.CanNotDeleteComment(reason))
    }
  }
}
