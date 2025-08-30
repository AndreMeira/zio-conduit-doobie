package conduit.domain.logic.authentication

import conduit.domain.model.entity.{Credential, Requester, User}
import conduit.domain.model.error.ApplicationError
import conduit.domain.model.types.user.{HashedPassword, HashedToken, Password}
import zio.ZIO

trait Authentication[Tx] {
  def hashPassword(password: Password): ZIO[Tx, Authentication.Error, HashedPassword]
  def generateToken(user: User): ZIO[Tx, Authentication.Error, HashedToken]
  def requester(token: Option[HashedToken]): ZIO[Tx, Authentication.Error, Requester]
  def authenticate(credential: Credential): ZIO[Tx, Authentication.Error, User]
}

object Authentication:
  type Error = Failure | ApplicationError.TransientError

  enum Failure extends ApplicationError:
    case InvalidCredentials
    case CanNotHashPassword(reason: Throwable)
    case CanNotGenerateToken(reason: Throwable)

    override def message: String = this match
      case InvalidCredentials          => "Invalid credentials provided"
      case CanNotHashPassword(reason)  => s"Can not hash password: ${reason.getMessage}"
      case CanNotGenerateToken(reason) => s"Can not generate token: ${reason.getMessage}"
