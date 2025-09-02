package conduit.domain.logic.persistence

import conduit.domain.model.entity.Follower
import conduit.domain.model.error.ApplicationError
import conduit.domain.model.error.ApplicationError.TransientError
import conduit.domain.model.types.article.AuthorId
import conduit.domain.model.types.user.UserId
import zio.ZIO

trait FollowerRepository[Tx] {
  protected type Result[A] = ZIO[Tx, TransientError, A]

  def save(follower: Follower): Result[Follower]
  def delete(follower: Follower): Result[Option[Follower]]
  def exists(follower: Follower): Result[Boolean]
  def list(by: UserId, authors: List[AuthorId]): Result[List[AuthorId]]
}
