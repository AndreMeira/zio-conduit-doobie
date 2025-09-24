package conduit.infrastructure.postgres.repository

import conduit.domain.logic.monitoring.Monitor
import conduit.domain.logic.persistence.FavoriteArticleRepository
import conduit.domain.model.entity.FavoriteArticle
import conduit.domain.model.types.article.{ ArticleFavoriteCount, ArticleId }
import conduit.domain.model.types.user.UserId
import conduit.infrastructure.postgres.Transaction
import conduit.infrastructure.postgres.Transaction.Transactional
import doobie.implicits.toSqlInterpolator
import conduit.infrastructure.postgres.meta.FieldMeta.given
import doobie.implicits.*
import doobie.postgres.implicits.*
import cats.syntax.list.*
import doobie.Fragments
import zio.*

class PostgresFavoriteArticleRepository(monitor: Monitor) extends FavoriteArticleRepository[Transaction] {
  override type Error = Transaction.Error

  override def add(favorite: FavoriteArticle): Result[FavoriteArticle] =
    monitor.track("PostgresFavoriteArticleRepository.add", "resource" -> "db") {
      Transactional.withTime: now =>
        sql"""INSERT INTO favorites (article_id, user_id, created_at)
             |VALUES (${favorite.article}, ${favorite.by}, $now)
             |ON CONFLICT (article_id, user_id) DO NOTHING
             |RETURNING article_id, user_id, created_at"""
          .stripMargin
          .query[FavoriteArticle]
          .option
          .map:
            case Some(favorite) => favorite
            case None           => favorite // If already exists, return the input favorite
    }

  override def delete(favorite: FavoriteArticle): Result[Option[FavoriteArticle]] =
    monitor.track("PostgresFavoriteArticleRepository.delete", "resource" -> "db") {
      Transactional:
        sql"""DELETE FROM favorites
             |WHERE article_id = ${favorite.article} AND user_id = ${favorite.by}
             |RETURNING user_id, article_id"""
          .stripMargin
          .query[FavoriteArticle]
          .option
    }

  override def deleteByArticle(article: ArticleId): Result[Int] =
    monitor.track("PostgresFavoriteArticleRepository.deleteByArticle", "resource" -> "db") {
      Transactional:
        sql"""DELETE FROM favorites WHERE article_id = $article"""
          .stripMargin
          .update
          .run
    }

  override def count(article: ArticleId): Result[ArticleFavoriteCount] =
    monitor.track("PostgresFavoriteArticleRepository.count", "resource" -> "db") {
      Transactional:
        sql"""SELECT count(*) FROM favorites WHERE article_id = $article"""
          .query[Int]
          .unique
          .map(ArticleFavoriteCount.apply)
    }

  override def count(articles: List[ArticleId]): Result[Map[ArticleId, ArticleFavoriteCount]] =
    monitor.track("PostgresFavoriteArticleRepository.count", "resource" -> "db") {
      ZIO
        .foreach(articles.toNel): articles =>
          Transactional {
            sql"""SELECT article_id, count(*) as favorite_count
                 |FROM favorites
                 |WHERE ${Fragments.in(fr"article_id", articles)}
                 |GROUP BY article_id"""
              .stripMargin
              .query[(ArticleId, Int)]
              .map((articleId, count) => articleId -> ArticleFavoriteCount(count))
              .to[List]
              .map(_.toMap)
          }
        .someOrElse(Map.empty)
    }

  override def exists(favorite: FavoriteArticle): Result[Boolean] =
    monitor.track("PostgresFavoriteArticleRepository.exists", "resource" -> "db") {
      Transactional:
        sql"""SELECT 1 FROM favorites WHERE article_id = ${favorite.article} AND user_id = ${favorite.by} LIMIT 1"""
          .query[Boolean]
          .option
          .map(_.isDefined)
    }

  override def list(by: UserId, articles: List[ArticleId]): Result[List[ArticleId]] =
    monitor.track("PostgresFavoriteArticleRepository.list", "resource" -> "db") {
      ZIO
        .foreach(articles.toNel): articles =>
          Transactional {
            sql"""SELECT article_id FROM favorites
                 |WHERE user_id = $by AND ${Fragments.in(fr"article_id", articles)}"""
              .stripMargin
              .query[ArticleId]
              .to[List]
          }
        .someOrElse(Nil)
    }
}

object PostgresFavoriteArticleRepository:
  val layer: ZLayer[Monitor, Nothing, FavoriteArticleRepository[Transaction]] = ZLayer {
    for monitor <- ZIO.service[Monitor]
    yield PostgresFavoriteArticleRepository(monitor)
  }
