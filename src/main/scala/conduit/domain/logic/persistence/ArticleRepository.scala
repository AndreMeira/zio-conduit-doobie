package conduit.domain.logic.persistence

import conduit.domain.logic.persistence.ArticleRepository.Filter
import conduit.domain.model.entity.{ Article, UserProfile }
import conduit.domain.model.error.ApplicationError
import conduit.domain.model.types.article.{ ArticleSlug, ArticleFavoriteCount as FavoriteCount }
import zio.ZIO

trait ArticleRepository[Tx] {
  protected type Result[A] = ZIO[Tx, ArticleRepository.Error, A] // for readability

  def save(article: Article): Result[Article.Expanded]
  def save(article: Article.Data): Result[Article.Expanded]
  def find(slug: ArticleSlug): Result[Article.Expanded]
  def feedOf(userId: String, limit: Int, offset: Int): Result[List[Article.Overview]]
  def recent(filters: List[Filter], limit: Int, offset: Int): Result[List[Article.Overview]]
}

object ArticleRepository:
  trait Error extends ApplicationError.TransientError

  enum Filter:
    case Author(username: String)
    case FavoriteOf(username: String)
    case Tag(tag: String)
