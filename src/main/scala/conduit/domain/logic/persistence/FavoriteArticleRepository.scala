package conduit.domain.logic.persistence

import conduit.domain.model.entity.FavoriteArticle
import conduit.domain.model.error.ApplicationError
import conduit.domain.model.types.article.*
import zio.ZIO

trait FavoriteArticleRepository[Tx] {
  def save(favorite: FavoriteArticle): ZIO[Tx, FavoriteArticleRepository.Error, Unit]
  def delete(favorite: FavoriteArticle): ZIO[Tx, FavoriteArticleRepository.Error, Unit]
  def count(article: ArticleId): ZIO[Tx, FavoriteArticleRepository.Error, ArticleFavoriteCount]
  def count(articles: List[ArticleId]): ZIO[Tx, FavoriteArticleRepository.Error, Map[ArticleId, ArticleFavoriteCount]]
}

object FavoriteArticleRepository:
  trait Error extends ApplicationError.TransientError
