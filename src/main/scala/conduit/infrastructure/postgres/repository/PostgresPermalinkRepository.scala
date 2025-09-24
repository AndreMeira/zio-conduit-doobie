package conduit.infrastructure.postgres.repository

import conduit.domain.logic.monitoring.Monitor
import conduit.domain.logic.persistence.{ FollowerRepository, PermalinkRepository }
import conduit.domain.model.types.article.*
import conduit.infrastructure.postgres.Transaction
import conduit.infrastructure.postgres.Transaction.Transactional
import doobie.implicits.toSqlInterpolator
import conduit.infrastructure.postgres.meta.FieldMeta.given
import zio.{ ZIO, ZLayer }

class PostgresPermalinkRepository(monitor: Monitor) extends PermalinkRepository[Transaction] {
  override type Error = Transaction.Error

  override def save(articleId: ArticleId, slug: ArticleSlug): Result[Unit] =
    monitor.track("PostgresPermalinkRepository.save", "resource" -> "db") {
      Transactional:
        sql"""INSERT INTO permalinks (article_id, slug) VALUES ($articleId, $slug)"""
          .update
          .run
          .map(_ => ())
    }

  override def exists(articleId: ArticleId, slug: ArticleSlug): Result[Boolean] =
    monitor.track("PostgresPermalinkRepository.exists", "resource" -> "db") {
      Transactional:
        sql"""SELECT 1 FROM permalinks WHERE article_id = $articleId AND slug = $slug LIMIT 1"""
          .query[Int]
          .option
          .map(_.isDefined)
    }

  override def exists(slug: ArticleSlug, authorId: AuthorId): Result[Boolean] =
    monitor.track("PostgresPermalinkRepository.existsByAuthor", "resource" -> "db") {
      Transactional:
        sql"""SELECT 1 FROM permalinks
             |INNER JOIN articles ON permalinks.article_id = articles.id
             |WHERE permalinks.slug = $slug AND articles.author_id = $authorId LIMIT 1"""
          .stripMargin
          .query[Int]
          .option
          .map(_.isDefined)
    }

  override def resolve(slug: ArticleSlug): Result[Option[ArticleId]] =
    monitor.track("PostgresPermalinkRepository.resolve", "resource" -> "db") {
      Transactional:
        sql"""SELECT article_id FROM permalinks WHERE slug = $slug LIMIT 1"""
          .query[ArticleId]
          .option
    }

  override def delete(articleId: ArticleId): Result[Unit] =
    monitor.track("PostgresPermalinkRepository.delete", "resource" -> "db") {
      Transactional:
        sql"""DELETE FROM permalinks WHERE article_id = $articleId"""
          .update
          .run
          .map(_ => ())
    }

}

object PostgresPermalinkRepository:
  val layer: ZLayer[Monitor, Nothing, PermalinkRepository[Transaction]] = ZLayer {
    for monitor <- ZIO.service[Monitor]
    yield PostgresPermalinkRepository(monitor)
  }
