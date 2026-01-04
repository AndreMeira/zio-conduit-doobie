package conduit.infrastructure.postgres.repository

import conduit.domain.logic.monitoring.Monitor
import conduit.domain.logic.persistence.ArticleSlugRepository
import conduit.domain.model.types.article.ArticleSlug
import conduit.infrastructure.postgres.Transaction
import conduit.infrastructure.postgres.Transaction.Transactional
import conduit.infrastructure.postgres.meta.FieldMeta.given
import doobie.implicits.toSqlInterpolator
import doobie.postgres.implicits.*
import zio.*

class PostgresArticleSlugRepository(monitor: Monitor) extends ArticleSlugRepository[Transaction] {
  override type Error = Transaction.Error

  override def nextAvailable(slug: ArticleSlug): Result[ArticleSlug] =
    monitor.track("PostgresArticleSlugRepository.nextAvailable", "resource" -> "db") {
      Transactional.withTime: now =>
        sql"""WITH current_rows AS (
             |    SELECT id, slug, version as current_version
             |    FROM slugs
             |    WHERE slug = $slug
             |    FOR UPDATE
             |),
             |updated_rows AS (
             |    UPDATE slugs
             |    SET version = current_version + 1, updated_at = $now
             |    FROM current_rows
             |    WHERE slugs.id = current_rows.id
             |    RETURNING version
             |),
             |inserted_rows AS (
             |    INSERT INTO slugs (slug, version, created_at, updated_at)
             |    SELECT $slug, 1, $now, $now
             |    WHERE NOT EXISTS (SELECT 1 FROM current_rows)
             |    RETURNING version
             |)
             |SELECT version FROM updated_rows
             |UNION ALL
             |SELECT version FROM inserted_rows"""
          .stripMargin
          .query[Int]
          .unique
          .map(slug.appendIndex)
    }
}

object PostgresArticleSlugRepository:
  val layer: ZLayer[Monitor, Nothing, ArticleSlugRepository[Transaction]] = ZLayer {
    for monitor <- ZIO.service[Monitor]
    yield PostgresArticleSlugRepository(monitor)
  }
