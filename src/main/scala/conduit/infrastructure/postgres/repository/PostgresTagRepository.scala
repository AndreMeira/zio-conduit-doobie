package conduit.infrastructure.postgres.repository

import cats.implicits.*
import conduit.domain.logic.monitoring.Monitor
import conduit.domain.logic.persistence.TagRepository
import conduit.domain.model.entity.Tag
import conduit.domain.model.types.article.{ ArticleId, ArticleTag }
import conduit.infrastructure.postgres.Transaction
import conduit.infrastructure.postgres.Transaction.Transactional
import conduit.infrastructure.postgres.meta.FieldMeta.given
import doobie.implicits.*
import doobie.syntax.*
import zio.*

class PostgresTagRepository(monitor: Monitor) extends TagRepository[Transaction] {
  override type Error = Transaction.Error

  override def distinct: Result[List[ArticleTag]] =
    monitor.track("PostgresTagRepository.distinct", "resource" -> "db") {
      Transactional:
        sql"""SELECT DISTINCT name FROM tags"""
          .query[ArticleTag]
          .to[List]
    }

  override def save(tag: List[Tag]): Result[List[Tag]] =
    monitor.track("PostgresTagRepository.save", "resource" -> "db") {
      Transactional:
        val values = tag.map(t => fr"(${t.article}, ${t.tag})").intercalate(fr",")
        sql"""INSERT INTO tags (article_id, name) VALUES $values
             |ON CONFLICT DO NOTHING
             |RETURNING article_id, name"""
          .stripMargin
          .query[Tag]
          .to[List]
    }

  override def deleteByArticle(articleId: ArticleId): Result[Int] =
    monitor.track("PostgresTagRepository.deleteByArticle", "resource" -> "db") {
      Transactional:
        sql"""DELETE FROM tags WHERE article_id = $articleId"""
          .update
          .run
    }
}

object PostgresTagRepository:
  val layer: ZLayer[Monitor, Nothing, TagRepository[Transaction]] = ZLayer {
    for monitor <- ZIO.service[Monitor]
    yield PostgresTagRepository(monitor)
  }
