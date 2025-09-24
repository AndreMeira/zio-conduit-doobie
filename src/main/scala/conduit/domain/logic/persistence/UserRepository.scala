package conduit.domain.logic.persistence

import conduit.domain.model.entity.Credentials
import conduit.domain.model.error.ApplicationError
import conduit.domain.model.error.ApplicationError.TransientError
import conduit.domain.model.types.user.{ Email, UserId }
import zio.ZIO

trait UserRepository[Tx] {
  type Error <: ApplicationError
  protected type Result[A] = ZIO[Tx, Error, A]

  def exists(userId: UserId): Result[Boolean]
  def save(credential: Credentials.Hashed): Result[UserId]
  def save(userId: UserId, credential: Credentials.Hashed): Result[Unit]
  def find(credential: Credentials.Hashed): Result[Option[UserId]]
  def findEmail(userId: UserId): Result[Option[Email]]
  def emailExists(email: Email): Result[Boolean]
  def findCredentials(userId: UserId): Result[Option[Credentials.Hashed]]
}
