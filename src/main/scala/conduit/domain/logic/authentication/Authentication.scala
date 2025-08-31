package conduit.domain.logic.authentication

import conduit.domain.model.entity.{ Credentials, User, UserProfile }
import conduit.domain.model.error.ApplicationError
import conduit.domain.model.types.user.{ HashedPassword, SignedToken, Password, UserId }
import zio.ZIO

trait Authentication[Tx] {
  def hash(credentials: Credentials.Clear): ZIO[Tx, Authentication.Error, Credentials.Hashed]
  def generateToken(user: UserId): ZIO[Tx, Authentication.Error, SignedToken]
  def user(token: Option[SignedToken]): ZIO[Tx, Authentication.Error, User]
  def authenticate(token: SignedToken): ZIO[Tx, Authentication.Error, User.Authenticated]
  def authenticate(credential: Credentials): ZIO[Tx, Authentication.Error, User.Authenticated]
}

object Authentication:
  type Error = Failure | ApplicationError.TransientError

  enum Failure extends ApplicationError:
    case InvalidCredentials
    case TokenExpired
    case CanNotHashPassword(reason: Throwable)
    case CanNotGenerateToken(reason: Throwable)

    override def message: String = this match
      case InvalidCredentials          => "Invalid credentials provided"
      case CanNotHashPassword(reason)  => s"Can not hash password: ${reason.getMessage}"
      case CanNotGenerateToken(reason) => s"Can not generate token: ${reason.getMessage}"
      case TokenExpired                => "Token has expired"
