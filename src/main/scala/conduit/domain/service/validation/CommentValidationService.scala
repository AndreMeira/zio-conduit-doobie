package conduit.domain.service.validation

import conduit.domain.logic.monitoring.Monitor
import conduit.domain.logic.persistence.{ ArticleRepository, CommentRepository }
import conduit.domain.logic.validation.{ ArticleValidator, CommentValidator }
import conduit.domain.model.entity.{ Article, Comment }
import conduit.domain.model.error.ApplicationError.ValidationError
import conduit.domain.model.request.comment.{ AddCommentRequest, DeleteCommentRequest, ListCommentsRequest }
import conduit.domain.model.types.article.{ ArticleId, ArticleSlug }
import conduit.domain.model.types.comment.{ CommentAuthorId, CommentBody, CommentId }
import conduit.domain.service.validation.CommentValidationService.Invalid
import zio.ZIO
import zio.prelude.Validation

class CommentValidationService[Tx](
  monitor: Monitor,
  articleValidator: ArticleValidator[Tx],
  comments: CommentRepository[Tx],
) extends CommentValidator[Tx] {

  override def validate(request: AddCommentRequest): Result[CommentData] =
    monitor.track("CommentValidationService.validateCreate") {
      ZIO.succeed {
        val slug   = request.article
        val author = CommentAuthorId(request.requester.userId)
        CommentBody.fromString(request.payload.comment.body) match {
          case Validation.Failure(logs, errors) => Validation.Failure(logs, errors)
          case Validation.Success(_, body)      => Validation.succeed((author = author, body = body, slug = slug))
        }
      }
    }

  override def validate(request: DeleteCommentRequest): Result[CommentId] =
    monitor.track("CommentValidationService.validateDelete") {
      ZIO.succeed(Validation.succeed(request.comment))
    }

  override def validate(request: ListCommentsRequest): Result[ArticleSlug] =
    monitor.track("CommentValidationService.validateList") {
      ZIO.succeed(Validation.succeed(request.article))
    }

  private def validateCreate(article: Validated[Article], request: AddCommentRequest): Validated[Comment.Data] =
    Validation
      .validate(article.map(_.id), CommentBody.fromString(request.payload.comment.body))
      .map(Comment.Data(_, _, CommentAuthorId(request.requester.userId)))
}

object CommentValidationService:
  enum Invalid extends ValidationError:
    case CommentNotFound(id: Long)

    override def key: String     = "comment"
    override def message: String = this match
      case CommentNotFound(id) => s"with id '$id' not found"
