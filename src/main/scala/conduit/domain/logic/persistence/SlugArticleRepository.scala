package conduit.domain.logic.persistence

import conduit.domain.model.error.ApplicationError
import conduit.domain.model.types.article.{ArticleSlug, ArticleSlugIndex}
import zio.ZIO

trait SlugArticleRepository[Tx] {
  def createDistinct(slug: ArticleSlug): ZIO[Tx, SlugArticleRepository.Error, ArticleSlug]
}

object SlugArticleRepository:
  trait Error extends ApplicationError.TransientError