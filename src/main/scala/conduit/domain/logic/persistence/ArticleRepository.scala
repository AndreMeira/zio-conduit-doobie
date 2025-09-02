package conduit.domain.logic.persistence

import conduit.domain.logic.persistence.ArticleRepository.Search
import conduit.domain.model.entity.Article
import conduit.domain.model.error.ApplicationError.TransientError
import conduit.domain.model.types.article.{ ArticleId, ArticleSlug }
import conduit.domain.model.types.user.UserId
import zio.ZIO

trait ArticleRepository[Tx] {
  protected type Result[A] = ZIO[Tx, TransientError, A] // for readability

  def save(article: Article): Result[Article.Expanded]
  def save(article: Article.Data): Result[Article.Expanded]
  def delete(articleId: ArticleSlug): Result[Option[Article]]
  def exists(slug: ArticleSlug): Result[Boolean]
  def exists(slug: ArticleSlug, authorId: UserId): Result[Boolean]
  def findBySlug(slug: ArticleSlug): Result[Option[Article]]
  def findById(id: ArticleId): Result[Option[Article]]
  def findExpanded(slug: ArticleSlug): Result[Option[Article.Expanded]]
  def feedOf(userId: UserId, limit: Int, offset: Int): Result[List[Article.Overview]]
  def recent(filters: List[Search], limit: Int, offset: Int): Result[List[Article.Overview]]
  def countFeedOf(userId: UserId): Result[Int]
  def countSearch(filters: List[Search]): Result[Int]
}

object ArticleRepository:
  enum Search:
    case Tag(tag: String)
    case Author(username: String)
    case FavoriteOf(username: String)
