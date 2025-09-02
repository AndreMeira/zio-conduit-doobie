package conduit.domain.logic.persistence

import conduit.domain.model.error.ApplicationError
import conduit.domain.model.error.ApplicationError.TransientError
import conduit.domain.model.types.article.ArticleSlug
import zio.ZIO

trait SlugArticleRepository[Tx] {
  protected type Result[A] = ZIO[Tx, TransientError, A]

  def createDistinct(slug: ArticleSlug): Result[ArticleSlug]
}
