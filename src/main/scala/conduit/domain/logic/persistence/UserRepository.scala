package conduit.domain.logic.persistence

import conduit.domain.model.entity.Credentials
import conduit.domain.model.error.ApplicationError.TransientError
import conduit.domain.model.types.user.UserId
import zio.ZIO

trait UserRepository[Tx] {
  protected type Result[A] = ZIO[Tx, TransientError, A]

  def exists(userId: UserId): Result[Boolean]
  def save(credential: Credentials.Hashed): Result[UserId]
  def find(credential: Credentials.Hashed): Result[Option[UserId]]
}
