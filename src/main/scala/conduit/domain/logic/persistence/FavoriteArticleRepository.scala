package conduit.domain.logic.persistence

import conduit.domain.model.entity.FavoriteArticle
import conduit.domain.model.error.ApplicationError
import conduit.domain.model.error.ApplicationError.TransientError
import conduit.domain.model.types.article.*
import conduit.domain.model.types.user.UserId
import zio.ZIO

trait FavoriteArticleRepository[Tx] {
  protected type Result[A] = ZIO[Tx, TransientError, A]

  def add(favorite: FavoriteArticle): Result[FavoriteArticle]
  def save(favorite: FavoriteArticle): Result[FavoriteArticle]
  def delete(favorite: FavoriteArticle): Result[Option[FavoriteArticle]]
  def deleteByArticle(article: ArticleId): Result[Int] // number of deleted favorites
  def count(article: ArticleId): Result[ArticleFavoriteCount]
  def count(articles: List[ArticleId]): Result[Map[ArticleId, ArticleFavoriteCount]]
  def exists(favorite: FavoriteArticle): Result[Boolean]
  def list(by: UserId, articles: List[ArticleId]): Result[List[ArticleId]]
}
