package conduit.domain.service.authorisation

import conduit.domain.logic.authorisation.CommentAuthorisation
import conduit.domain.logic.authorisation.CommentAuthorisation.Failure
import conduit.domain.logic.authorisation.definition.Authorisation
import conduit.domain.logic.monitoring.Monitor
import conduit.domain.logic.persistence.CommentRepository
import conduit.domain.model.entity.User
import conduit.domain.model.request.CommentRequest
import conduit.domain.model.request.comment.{ AddCommentRequest, DeleteCommentRequest, ListCommentsRequest }
import conduit.domain.model.types.article.ArticleSlug
import conduit.domain.model.types.comment.{ CommentAuthorId, CommentId }
import zio.ZIO
import zio.ZLayer
import izumi.reflect.Tag as ReflectionTag

class CommentAuthorisationService[Tx](
  monitor: Monitor,
  val comments: CommentRepository[Tx],
) extends CommentAuthorisation[Tx] {

  override type Error = comments.Error // can only fail with same errors as the injected repository

  override def authorise(request: CommentRequest): Result =
    monitor.track("CommentAuthorisationService.authorise") {
      request match
        case request: DeleteCommentRequest => canDeleteComment(request)
        case _: ListCommentsRequest        => allowed // Any user (authenticated or not) can view comments
        case _: AddCommentRequest          => allowed // Any authenticated user can create comments
    }

  private def canDeleteComment(request: DeleteCommentRequest): Result = {
    val comment = CommentId(request.commentId)
    val author  = CommentAuthorId(request.requester.userId)
    val reason  = s"Comment ${request.commentId} does not belong to user ${request.requester.userId}"
    comments.exists(comment, author).map {
      case true  => Authorisation.Result.Allowed
      case false => Authorisation.Result.Denied(Failure.CanNotDeleteComment(reason))
    }
  }
}

object CommentAuthorisationService:
  def layer[Tx: ReflectionTag]: ZLayer[CommentRepository[Tx] & Monitor, Nothing, CommentAuthorisationService[Tx]] =
    ZLayer {
      for {
        monitor  <- ZIO.service[Monitor]
        comments <- ZIO.service[CommentRepository[Tx]]
      } yield CommentAuthorisationService(monitor, comments)
    }
