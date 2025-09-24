package conduit.domain.logic.persistence

import conduit.domain.model.error.ApplicationError
import conduit.domain.model.types.article.{ ArticleId, ArticleSlug, AuthorId }
import zio.ZIO

trait PermalinkRepository[Tx] {
  type Error <: ApplicationError
  protected type Result[A] = ZIO[Tx, Error, A] // for readability

  def save(articleId: ArticleId, slug: ArticleSlug): Result[Unit]
  def exists(articleId: ArticleId, slug: ArticleSlug): Result[Boolean]
  def exists(slug: ArticleSlug, authorId: AuthorId): Result[Boolean]
  def resolve(slug: ArticleSlug): Result[Option[ArticleId]]
  def delete(articleId: ArticleId): Result[Unit]
}
