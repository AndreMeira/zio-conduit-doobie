package conduit.domain.logic.persistence

import conduit.domain.model.entity.Follower
import conduit.domain.model.error.ApplicationError
import zio.ZIO

trait FollowerRepository[Tx] {
  protected type Result[A] = ZIO[Tx, FollowerRepository.Error, A] // for readability
  
  def save(follower: Follower): Result[Follower]
  def delete(follower: Follower): Result[Option[Follower]]
  def exists(follower: Follower): Result[Boolean]
}

object FollowerRepository:
  trait Error extends ApplicationError.TransientError
