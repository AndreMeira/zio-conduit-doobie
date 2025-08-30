package conduit.domain.logic.persistence

import conduit.domain.model.entity.{Credential, User}
import conduit.domain.model.error.ApplicationError
import conduit.domain.model.types.user.{Email, HashedPassword, UserId}
import zio.ZIO

trait UserRepository[Tx] {
  def findById(id: UserId): ZIO[Tx, UserRepository.Error, Option[User]]
  def findByEmail(email: Email): ZIO[Tx, UserRepository.Error, Option[User]]
  def save(user: User.Data): ZIO[Tx, UserRepository.Error, User]
  def save(user: User): ZIO[Tx, UserRepository.Error, User]
}

object UserRepository:
  trait Error extends ApplicationError.TransientError
