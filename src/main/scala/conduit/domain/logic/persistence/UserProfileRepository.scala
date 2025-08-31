package conduit.domain.logic.persistence

import conduit.domain.model.entity.{ Credentials, UserProfile }
import conduit.domain.model.error.ApplicationError
import conduit.domain.model.types.user.{ Email, UserId }
import zio.ZIO

trait UserProfileRepository[Tx] {
  protected type Result[A] = ZIO[Tx, UserProfileRepository.Error, A] // for readability

  def findById(id: UserId): Result[Option[UserProfile]]
  def findByEmail(email: Email): Result[Option[UserProfile]]
  def save(user: UserProfile.Data): Result[UserProfile]
  def save(user: UserProfile): Result[UserProfile]
}

object UserProfileRepository:
  trait Error extends ApplicationError.TransientError
