package conduit.domain.logic.persistence

import conduit.domain.logic.persistence.ArticleRepository.Filter
import conduit.domain.model.entity.Article
import conduit.domain.model.error.ApplicationError
import conduit.domain.model.types.article.ArticleSlug
import zio.ZIO

trait ArticleRepository[Tx] {
  def find(slug: ArticleSlug): ZIO[Tx, ArticleRepository.Error, Option[Article]]
  def save(article: Article.Data): ZIO[Tx, ArticleRepository.Error, Article]
  def save(article: Article): ZIO[Tx, ArticleRepository.Error, Article]
  def feedOf(userId: String, limit: Int, offset: Int): ZIO[Tx, ArticleRepository.Error, List[Article.Overview]]
  def recent(filters: List[Filter], limit: Int, offset: Int): ZIO[Tx, ArticleRepository.Error, List[Article.Overview]]
}

object ArticleRepository:
  trait Error extends ApplicationError.TransientError

  enum Filter:
    case Author(username: String)
    case FavoriteOf(username: String)
    case Tag(tag: String)