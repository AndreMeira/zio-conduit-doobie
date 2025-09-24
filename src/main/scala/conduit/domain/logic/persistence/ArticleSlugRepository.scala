package conduit.domain.logic.persistence

import conduit.domain.model.error.ApplicationError
import conduit.domain.model.error.ApplicationError.TransientError
import conduit.domain.model.types.article.ArticleSlug
import zio.ZIO

trait ArticleSlugRepository[Tx] {
  type Error <: ApplicationError
  protected type Result[A] = ZIO[Tx, Error, A]

  def nextAvailable(slug: ArticleSlug): Result[ArticleSlug]
}
