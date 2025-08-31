package conduit.domain.logic.persistence

import conduit.domain.model.entity.{ Credentials, User, UserProfile }
import conduit.domain.model.error.ApplicationError
import conduit.domain.model.types.user.UserId
import zio.ZIO

trait UserRepository[Tx] {
  protected type Result[A] = ZIO[Tx, UserRepository.Error, A] // for readability

  def save(credential: Credentials): Result[UserId]
  def find(credential: Credentials): Result[Option[User]]
}

object UserRepository:
  trait Error extends ApplicationError.TransientError
