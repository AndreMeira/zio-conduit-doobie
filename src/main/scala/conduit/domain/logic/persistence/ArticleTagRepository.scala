package conduit.domain.logic.persistence

import conduit.domain.model.error.ApplicationError.TransientError
import conduit.domain.model.types.article.{ ArticleId, ArticleTag }
import zio.ZIO

trait ArticleTagRepository[Tx] {
  type Result[A] = ZIO[Tx, TransientError, A]

  def distinct: Result[List[ArticleTag]]
  def deleteTags(articleId: ArticleId): Result[Unit]
  def getTags(articleId: ArticleId): Result[List[ArticleTag]]
  def addTag(articleId: ArticleId, tags: List[ArticleTag]): Result[Unit]
}
