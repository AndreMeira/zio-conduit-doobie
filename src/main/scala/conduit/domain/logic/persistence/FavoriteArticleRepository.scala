package conduit.domain.logic.persistence

import conduit.domain.model.entity.FavoriteArticle
import conduit.domain.model.error.ApplicationError
import conduit.domain.model.types.article.*
import zio.ZIO

trait FavoriteArticleRepository[Tx] {
  protected type Result[A] = ZIO[Tx, FavoriteArticleRepository.Error, A] // for readability
  
  def save(favorite: FavoriteArticle): Result[FavoriteArticle]
  def delete(favorite: FavoriteArticle): Result[Option[FavoriteArticle]]
  def count(article: ArticleId): Result[ArticleFavoriteCount]
  def count(articles: List[ArticleId]): Result[Map[ArticleId, ArticleFavoriteCount]]
}

object FavoriteArticleRepository:
  trait Error extends ApplicationError.TransientError
