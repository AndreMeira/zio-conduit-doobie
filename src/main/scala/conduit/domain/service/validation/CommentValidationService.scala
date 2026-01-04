package conduit.domain.service.validation

import conduit.domain.logic.monitoring.Monitor
import conduit.domain.logic.validation.CommentValidator
import conduit.domain.model.entity.{ Article, Comment }
import conduit.domain.model.error.ApplicationError.ValidationError
import conduit.domain.model.request.comment.{ AddCommentRequest, DeleteCommentRequest, ListCommentsRequest }
import conduit.domain.model.types.article.ArticleSlug
import conduit.domain.model.types.comment.{ CommentAuthorId, CommentBody, CommentId }
import zio.{ ZIO, ZLayer }
import zio.prelude.Validation

import izumi.reflect.Tag as ReflectionTag

class CommentValidationService[Tx](monitor: Monitor) extends CommentValidator[Tx] {

  override type Error = Nothing // cannot fail

  override def parse(request: DeleteCommentRequest): Result[CommentId] =
    monitor.track("CommentValidationService.validateDelete") {
      ZIO.succeed {
        Validation.succeed(CommentId(request.commentId))
      }
    }

  override def parse(request: ListCommentsRequest): Result[ArticleSlug] =
    monitor.track("CommentValidationService.validateList") {
      ZIO.succeed {
        Validation.succeed(ArticleSlug(request.slug))
      }
    }

  override def parse(request: AddCommentRequest): Result[CommentData] =
    monitor.track("CommentValidationService.validateCreate") {
      ZIO.succeed {
        CommentBody
          .fromString(request.payload.comment.body)
          .map: body =>
            val slug   = ArticleSlug(request.slug)
            val author = CommentAuthorId(request.requester.userId)
            (author = author, body = body, slug = slug)
      }
    }
}

object CommentValidationService:
  def layer[Tx: ReflectionTag]: ZLayer[Monitor, Nothing, CommentValidator[Tx]] =
    ZLayer {
      for monitor <- ZIO.service[Monitor]
      yield CommentValidationService(monitor)
    }
