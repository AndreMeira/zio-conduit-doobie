package conduit.infrastructure.postgres.repository

import conduit.domain.logic.monitoring.Monitor
import conduit.domain.logic.persistence.CommentRepository
import conduit.domain.model.entity.Comment
import conduit.domain.model.types.article.ArticleId
import conduit.domain.model.types.comment.{ CommentAuthorId, CommentId }
import conduit.infrastructure.postgres.Transaction
import conduit.infrastructure.postgres.Transaction.Transactional
import doobie.implicits.toSqlInterpolator
import conduit.infrastructure.postgres.meta.FieldMeta.given
import doobie.implicits.*
import doobie.postgres.implicits.*
import zio.*

class PostgresCommentRepository(monitor: Monitor) extends CommentRepository[Transaction] {
  override type Error = Transaction.Error

  override def find(commentId: CommentId): Result[Option[Comment]] =
    monitor.track("PostgresCommentRepository.find", "resource" -> "db") {
      Transactional:
        sql"""SELECT
             |c.id, c.article_id, c.body, c.author_id, c.created_at, c.updated_at
             |FROM comments c
             |WHERE c.id = $commentId"""
          .stripMargin
          .query[Comment]
          .option
    }

  override def save(comment: Comment.Data): Result[Comment] =
    monitor.track("PostgresCommentRepository.save", "resource" -> "db") {
      Transactional.withTime: now =>
        sql"""INSERT INTO comments (article_id, body, author_id, created_at, updated_at)
             |VALUES (${comment.article}, ${comment.body}, ${comment.author}, $now, $now)
             |RETURNING id, article_id, body, author_id, created_at, updated_at"""
          .stripMargin
          .query[Comment]
          .unique
    }

  override def delete(commentId: CommentId): Result[Option[Comment]] =
    monitor.track("PostgresCommentRepository.delete", "resource" -> "db") {
      Transactional:
        sql"""DELETE FROM comments WHERE id = $commentId
             |RETURNING id, article_id, body, author_id, created_at, updated_at"""
          .stripMargin
          .query[Comment]
          .option
    }

  override def exists(commentId: CommentId, author: CommentAuthorId): Result[Boolean] =
    monitor.track("PostgresCommentRepository.exists", "resource" -> "db") {
      Transactional:
        sql"""SELECT 1 FROM comments WHERE id = $commentId AND author_id = $author LIMIT 1"""
          .query[Boolean]
          .option
          .map(_.isDefined)
    }

  override def findByArticle(article: ArticleId): Result[List[Comment]] =
    monitor.track("PostgresCommentRepository.findByArticle", "resource" -> "db") {
      Transactional:
        sql"""SELECT
             |c.id, c.article_id, c.body, c.author_id, c.created_at, c.updated_at
             |FROM comments c
             |WHERE c.article_id = $article
             |ORDER BY c.created_at ASC"""
          .stripMargin
          .query[Comment]
          .to[List]
    }
}

object PostgresCommentRepository:
  val layer: ZLayer[Monitor, Nothing, CommentRepository[Transaction]] = ZLayer {
    for monitor <- ZIO.service[Monitor]
    yield PostgresCommentRepository(monitor)
  }
