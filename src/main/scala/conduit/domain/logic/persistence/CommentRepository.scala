package conduit.domain.logic.persistence

import conduit.domain.model.entity.Comment
import conduit.domain.model.error.ApplicationError
import conduit.domain.model.types.article.ArticleId
import conduit.domain.model.types.comment.CommentId
import zio.ZIO

trait CommentRepository[Tx] {
  protected type Result[A] = ZIO[Tx, CommentRepository.Error, A] // for readability
  
  def find(commentId: CommentId): Result[Option[Comment]]
  def save(comment: Comment.Data): Result[Comment]
  def delete(commentId: CommentId): Result[Option[Comment]]
  def findByArticle(article: ArticleId): Result[List[Comment]]
}

object CommentRepository:
  trait Error extends ApplicationError.TransientError
