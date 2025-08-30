package conduit.domain.logic.persistence

import conduit.domain.model.entity.Follower
import conduit.domain.model.error.ApplicationError
import zio.ZIO

trait FollowerRepository[Tx] {
  def save(follower: Follower): ZIO[Tx, FollowerRepository.Error, Unit]
  def delete(follower: Follower): ZIO[Tx, FollowerRepository.Error, Unit]
  def exists(follower: Follower): ZIO[Tx, FollowerRepository.Error, Boolean]
}

object FollowerRepository:
  trait Error extends ApplicationError.TransientError
