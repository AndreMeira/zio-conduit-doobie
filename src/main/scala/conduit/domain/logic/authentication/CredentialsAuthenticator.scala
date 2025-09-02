package conduit.domain.logic.authentication

import conduit.domain.model.entity.{ Credentials, User }
import conduit.domain.model.error.ApplicationError
import conduit.domain.model.error.ApplicationError.{ FromException, TransientError, UnauthorisedError }
import zio.ZIO

trait CredentialsAuthenticator[Tx] {
  protected type Error     = CredentialsAuthenticator.Failure | TransientError
  protected type Result[A] = ZIO[Tx, Error, A]

  def authenticate(credential: Credentials): Result[User.Authenticated]
  def hash(credentials: Credentials.Clear): Result[Credentials.Hashed]
}

object CredentialsAuthenticator:
  enum Failure extends ApplicationError:
    case InvalidCredentials                    extends Failure, UnauthorisedError
    case CanNotHashPassword(reason: Throwable) extends Failure, FromException(reason)

    override def message: String = this match
      case CanNotHashPassword(reason) => s"Can not hash password: ${reason.getMessage}"
      case InvalidCredentials         => "Invalid credentials provided"
