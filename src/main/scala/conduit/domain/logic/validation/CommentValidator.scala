package conduit.domain.logic.validation

import conduit.domain.model.error.ApplicationError
import conduit.domain.model.error.ApplicationError.ValidationError
import conduit.domain.model.request.comment.*
import conduit.domain.model.types.article.ArticleSlug
import conduit.domain.model.types.comment.{ CommentAuthorId, CommentBody, CommentId }
import zio.ZIO
import zio.prelude.Validation

// @see https://lexi-lambda.github.io/blog/2019/11/05/parse-don-t-validate/

trait CommentValidator[Tx] {
  type Error <: ApplicationError
  protected type Validated[A] = Validation[ValidationError, A]
  protected type Result[A]    = ZIO[Tx, Error, Validated[A]]

  protected type CommentData = (author: CommentAuthorId, body: CommentBody, slug: ArticleSlug)

  def parse(request: AddCommentRequest): Result[CommentData]
  def parse(request: DeleteCommentRequest): Result[CommentId]
  def parse(request: ListCommentsRequest): Result[ArticleSlug]
}
