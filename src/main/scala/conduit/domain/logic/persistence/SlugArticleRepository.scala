package conduit.domain.logic.persistence

import conduit.domain.model.error.ApplicationError
import conduit.domain.model.types.article.{ ArticleSlug, ArticleSlugIndex }
import zio.ZIO

trait SlugArticleRepository[Tx] {
  protected type Result[A] = ZIO[Tx, SlugArticleRepository.Error, A] // for readability
  def createDistinct(slug: ArticleSlug): Result[ArticleSlug]
}

object SlugArticleRepository:
  trait Error extends ApplicationError.TransientError
