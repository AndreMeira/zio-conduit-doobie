package conduit.infrastructure.postgres.repository

import conduit.domain.logic.monitoring.Monitor
import conduit.domain.logic.persistence.ArticleRepository
import conduit.domain.logic.persistence.ArticleRepository.Search
import conduit.domain.logic.persistence.ArticleRepository.Search.Tag
import conduit.domain.model.entity.{ Article, UserProfile }
import conduit.domain.model.types.article.{ ArticleId, ArticleSlug, ArticleTitle, AuthorId }
import conduit.domain.model.types.user.UserId
import conduit.infrastructure.postgres.Transaction
import conduit.infrastructure.postgres.Transaction.Transactional
import conduit.infrastructure.postgres.meta.FieldMeta.given
import doobie.Fragment
import doobie.implicits.*
import doobie.postgres.implicits.*
import doobie.syntax.*
import zio.*

class PostgresArticleRepository(monitor: Monitor) extends ArticleRepository[Transaction] {

  override type Error = Transaction.Error

  private def exists(slug: ArticleSlug, authorId: AuthorId): Result[Boolean] =
    monitor.track("PostgresArticleRepository.exists", "resource" -> "db") {
      Transactional:
        sql"""SELECT 1 FROM permalinks
             |INNER JOIN articles ON permalinks.article_id = articles.id
             |WHERE permalinks.slug = $slug AND articles.author_id = $authorId LIMIT 1"""
          .stripMargin
          .query[Boolean]
          .option
          .map(_.isDefined)
    }

  override def titleExists(title: ArticleTitle, authorId: AuthorId): Result[Boolean] =
    monitor.track("PostgresArticleRepository.titleExists", "resource" -> "db") {
      Transactional:
        sql"""SELECT 1 FROM articles WHERE title = $title AND author_id = $authorId LIMIT 1"""
          .query[Boolean]
          .option
          .map(_.isDefined)
    }

  override def find(id: ArticleId): Result[Option[Article.Expanded]] =
    monitor.track("PostgresArticleRepository.find", "resource" -> "db") {
      Transactional:
        sql"""SELECT
             |a.id, -- ArticleId
             |a.slug, a.title, a.description, a.author_id, a.body, -- Article.Data
             |p.user_id, p.name, p.bio, p.image, p.created_at, p.updated_at, -- UserProfile.Data
             |ARRAY(SELECT name FROM tags t WHERE t.article_id = a.id) as tags, -- Array[ArticleTag]
             |(SELECT count(*) FROM favorites f WHERE f.article_id = a.id) as favorite_count, -- ArticleFavoriteCount
             |a.created_at, a.updated_at -- Article.Metadata
             |FROM articles a
             |JOIN profiles p ON a.author_id = p.user_id
             |WHERE a.id = $id"""
          .stripMargin
          .query[Article.Expanded]
          .option
    }

  override def feedOf(userId: UserId, offset: Int, limit: Int): Result[List[Article.Overview]] =
    monitor.track("PostgresArticleRepository.feedOf", "resource" -> "db") {
      Transactional:
        sql"""SELECT
             |a.id, -- ArticleId
             |a.slug, a.title, a.description, a.author_id, -- Article.Data
             |p.user_id, p.name, p.bio, p.image, p.created_at, p.updated_at, -- UserProfile.Data
             |ARRAY(SELECT name FROM tags t WHERE t.article_id = a.id) as tags, -- Array[ArticleTag]
             |(SELECT count(fav.id) FROM favorites fav WHERE fav.article_id = a.id) as favorite_count, -- ArticleFavoriteCount
             |a.created_at, a.updated_at -- Article.Metadata
             |FROM articles a
             |JOIN profiles p ON a.author_id = p.user_id
             |JOIN followers f ON f.followee_id = a.author_id
             |WHERE f.follower_id = $userId
             |ORDER BY a.created_at DESC
             |OFFSET $offset LIMIT $limit"""
          .stripMargin
          .query[Article.Overview]
          .to[List]
    }

  override def countFeedOf(userId: UserId): Result[Int] =
    monitor.track("PostgresArticleRepository.countFeedOf", "resource" -> "db") {
      Transactional:
        sql"""SELECT count(*) FROM articles a
             |JOIN followers f ON f.followee_id = a.author_id
             |WHERE f.follower_id = $userId"""
          .stripMargin
          .query[Int]
          .unique
    }

  override def save(article: Article.Data): Result[Option[Article.Expanded]] =
    monitor.track("PostgresArticleRepository.save", "resource" -> "db", "operation" -> "insert") {
      Transactional.withTime: now =>
        sql"""WITH inserted_article AS (
             |  INSERT INTO articles (author_id, title, description, body, slug, created_at, updated_at)
             |  VALUES (${article.author}, ${article.title}, ${article.description}, ${article.body}, ${article.slug}, $now, $now)
             |  RETURNING *
             |)
             |SELECT
             |ia.id, -- ArticleId
             |ia.slug, ia.title, ia.description, ia.author_id, ia.body, -- Article.Data
             |p.user_id, p.name, p.bio, p.image, p.created_at, p.updated_at, -- UserProfile.Data
             |ARRAY[]::TEXT[] as tags, -- Array[ArticleTag] empty on creation
             |(SELECT count(*) FROM favorites f WHERE f.article_id = ia.id) as favorite_count, -- ArticleFavoriteCount
             | ia.created_at, ia.updated_at -- Article.Metadata
             |FROM inserted_article as ia
             |JOIN profiles p ON ia.author_id = p.user_id"""
          .stripMargin
          .query[Article.Expanded]
          .option
    }

  override def save(articleId: ArticleId, article: Article.Data): Result[Option[Article.Expanded]] =
    monitor.track("PostgresArticleRepository.save", "resource" -> "db", "operation" -> "update") {
      Transactional.withTime: now =>
        sql"""WITH updated_article AS (
             |  UPDATE articles
             |  SET title = ${article.title}, description = ${article.description}, body = ${article.body}, slug = ${article.slug},updated_at = $now
             |  WHERE id = $articleId
             |  RETURNING *
             |)
             |SELECT
             |ua.id, -- ArticleId
             |ua.slug, ua.title, ua.description, ua.author_id, ua.body, -- Article.Data
             |p.user_id, p.name, p.bio, p.image, p.created_at, p.updated_at, -- UserProfile.Data
             |ARRAY(SELECT name FROM tags t WHERE t.article_id = ua.id) as tags, -- Array[ArticleTag]
             |(SELECT count(*) FROM favorites f WHERE f.article_id = ua.id) as favorite_count, -- ArticleFavoriteCount
             |ua.created_at, ua.updated_at -- Article.Metadata
             |FROM updated_article as ua
             |JOIN profiles p ON ua.author_id = p.user_id"""
          .stripMargin
          .query[Article.Expanded]
          .option
    }

  override def delete(articleId: ArticleId): Result[Option[Article]] =
    monitor.track("PostgresArticleRepository.delete", "resource" -> "db") {
      Transactional:
        sql"""DELETE FROM articles
             |WHERE id = $articleId
             |RETURNING id, slug, title, description, author_id, body, created_at, updated_at"""
          .stripMargin
          .query[Article]
          .option
    }

  override def search(filters: List[ArticleRepository.Search], offset: Int, limit: Int): Result[List[Article.Overview]] =
    monitor.track("PostgresArticleRepository.search", "resource" -> "db") {
      Transactional:
        // @todo optimize this query
        sql"""SELECT
             |a.id, -- ArticleId
             |a.slug, a.title, a.description, a.author_id, -- Article.Data
             |p.user_id, p.name, p.bio, p.image, p.created_at, p.updated_at, -- UserProfile.Data
             |ARRAY(SELECT name FROM tags t WHERE t.article_id = a.id) as tagList, -- Array[ArticleTag]
             |(SELECT count(*) FROM favorites f WHERE f.article_id = a.id) as favorite_count, -- ArticleFavoriteCount
             |a.created_at, a.updated_at -- Article.Metadata
             |FROM articles a
             |INNER JOIN profiles p ON a.author_id = p.user_id
             |${sqlJoinFilters(filters)}
             |ORDER BY a.created_at DESC
             |OFFSET $offset LIMIT $limit"""
          .stripMargin
          .query[Article.Overview]
          .to[List]
    }

  override def countSearch(filters: List[ArticleRepository.Search]): Result[Int] =
    monitor.track("PostgresArticleRepository.countSearch", "resource" -> "db") {
      Transactional:
        sql"""SELECT count(*) FROM articles a
             |JOIN profiles p ON a.author_id = p.user_id
             |${sqlJoinFilters(filters)}"""
          .stripMargin
          .query[Int]
          .unique
    }

  private def sqlJoinFilters(filters: List[ArticleRepository.Search]): Fragment =
    filters
      .map:
        case Search.Tag(tag) =>
          fr"INNER JOIN tags tf ON tf.article_id = a.id AND tf.name = $tag"

        case Search.Author(author) =>
          fr"INNER JOIN profiles pfa ON pfa.user_id = a.author_id AND pfa.name = $author"

        case Search.FavoriteOf(username) =>
          fr"""INNER JOIN favorites fv ON fv.article_id = a.id
              |INNER JOIN profiles pfv ON pfv.user_id = fv.user_id AND pfv.name = $username
              """.stripMargin
      .foldLeft(fr"")(_ +~+ _)

}

object PostgresArticleRepository:
  val layer: ZLayer[Monitor, Nothing, ArticleRepository[Transaction]] = ZLayer {
    for monitor <- ZIO.service[Monitor]
    yield PostgresArticleRepository(monitor)
  }
