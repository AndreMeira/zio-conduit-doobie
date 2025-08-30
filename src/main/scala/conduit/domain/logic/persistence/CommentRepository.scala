package conduit.domain.logic.persistence

import conduit.domain.model.entity.Comment
import conduit.domain.model.error.ApplicationError
import conduit.domain.model.types.article.ArticleId
import conduit.domain.model.types.comment.CommentId
import zio.ZIO

trait CommentRepository[Tx] {
  def find(commentId: CommentId): ZIO[Tx, CommentRepository.Error, Option[Comment]]
  def save(comment: Comment.Data): ZIO[Tx, CommentRepository.Error, Comment]
  def delete(commentId: CommentId): ZIO[Tx, CommentRepository.Error, Option[Comment]]
  def findByArticle(article: ArticleId): ZIO[Tx, CommentRepository.Error, List[Comment]]
}

object CommentRepository:
  trait Error extends ApplicationError.TransientError
