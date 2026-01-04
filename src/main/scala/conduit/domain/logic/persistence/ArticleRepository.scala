package conduit.domain.logic.persistence

import conduit.domain.logic.persistence.ArticleRepository.Search
import conduit.domain.model.entity.Article
import conduit.domain.model.error.ApplicationError
import conduit.domain.model.types.article.{ ArticleId, ArticleSlug, ArticleTitle, AuthorId }
import conduit.domain.model.types.user.UserId
import zio.ZIO

trait ArticleRepository[Tx] {
  type Error <: ApplicationError
  protected type Result[A] = ZIO[Tx, Error, A]

  def titleExists(title: ArticleTitle, authorId: AuthorId): Result[Boolean]
  def find(id: ArticleId): Result[Option[Article.Expanded]]
  def feedOf(userId: UserId, offset: Int, limit: Int): Result[List[Article.Overview]]
  def search(filters: List[Search], offset: Int, limit: Int): Result[List[Article.Overview]]
  def countFeedOf(userId: UserId): Result[Int]
  def countSearch(filters: List[Search]): Result[Int]
  def save(article: Article.Data): Result[Option[Article.Expanded]]
  def save(articleId: ArticleId, article: Article.Data): Result[Option[Article.Expanded]]
  def delete(articleId: ArticleId): Result[Option[Article]]
}

object ArticleRepository:
  enum Search:
    case Tag(tag: String)
    case Author(username: String)
    case FavoriteOf(username: String)
