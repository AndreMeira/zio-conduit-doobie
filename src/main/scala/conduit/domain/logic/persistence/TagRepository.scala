package conduit.domain.logic.persistence

import conduit.domain.model.entity.Tag
import conduit.domain.model.error.ApplicationError
import conduit.domain.model.error.ApplicationError.TransientError
import conduit.domain.model.types.article.{ ArticleId, ArticleTag }
import zio.ZIO

trait TagRepository[Tx] {
  type Error <: ApplicationError
  protected type Result[A] = ZIO[Tx, Error, A] // for readability

  def distinct: Result[List[ArticleTag]]
  def save(tag: List[Tag]): Result[List[Tag]]
  def deleteByArticle(articleId: ArticleId): Result[Int] // number of deleted tags
}
