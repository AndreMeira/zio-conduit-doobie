package conduit.domain.logic.persistence

import conduit.domain.model.entity.Tag
import conduit.domain.model.error.ApplicationError.TransientError
import conduit.domain.model.types.article.{ ArticleId, ArticleTag }
import zio.ZIO

trait TagRepository[Tx] {
  protected type Result[A] = ZIO[Tx, TransientError, A] // for readability

  def listAll: Result[List[ArticleTag]]
  def save(tag: List[Tag]): Result[List[Tag]]
  def deleteByArticle(articleId: ArticleId): Result[Int] // number of deleted tags
}
