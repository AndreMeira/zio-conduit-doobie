package conduit.domain.logic.validation

import conduit.domain.model.entity.Comment
import conduit.domain.model.error.ApplicationError.{ TransientError, ValidationError }
import conduit.domain.model.error.{ ApplicationError, InvalidInput }
import conduit.domain.model.request.comment.*
import conduit.domain.model.types.article.ArticleSlug
import conduit.domain.model.types.comment.{ CommentAuthorId, CommentBody, CommentId }
import zio.ZIO
import zio.prelude.Validation

trait CommentValidator[Tx] {
  protected type Validated[A] = Validation[ValidationError, A]
  protected type Result[A]    = ZIO[Tx, TransientError, Validated[A]]

  protected type CommentData = (author: CommentAuthorId, body: CommentBody, slug: ArticleSlug)

  def validate(request: AddCommentRequest): Result[CommentData]
  def validate(request: DeleteCommentRequest): Result[CommentId]
  def validate(request: ListCommentsRequest): Result[ArticleSlug]
}
