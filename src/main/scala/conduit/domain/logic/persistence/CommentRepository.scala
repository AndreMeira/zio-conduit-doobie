package conduit.domain.logic.persistence

import conduit.domain.model.entity.Comment
import conduit.domain.model.error.ApplicationError.TransientError
import conduit.domain.model.types.article.{ ArticleId, ArticleSlug }
import conduit.domain.model.types.comment.{ CommentAuthorId, CommentId }
import zio.ZIO

trait CommentRepository[Tx] {
  protected type Result[A] = ZIO[Tx, TransientError, A] // for readability

  def find(commentId: CommentId): Result[Option[Comment]]
  def save(comment: Comment.Data): Result[Comment]
  def delete(commentId: CommentId): Result[Option[Comment]]
  def exists(commentId: CommentId, author: CommentAuthorId): Result[Boolean]
  def findByArticle(article: ArticleId): Result[List[Comment]]
  def findByArticle(article: ArticleSlug): Result[List[Comment]]
}
