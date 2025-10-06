package conduit.domain.logic.authentication

import conduit.domain.model.entity.User
import conduit.domain.model.error.ApplicationError
import conduit.domain.model.error.ApplicationError.{ FromException, TransientError, UnauthorisedError }
import conduit.domain.model.types.user.{ SignedToken, UserId }
import zio.ZIO

trait TokenAuthenticator[Tx] {
  type Error >: TokenAuthenticator.Failure <: ApplicationError
  protected type Result[A] = ZIO[Tx, Error, A]

  def user(token: Option[SignedToken]): Result[User]
  def generateToken(user: UserId): Result[SignedToken]
  def authenticate(token: SignedToken): Result[User.Authenticated]
}

object TokenAuthenticator:
  enum Failure extends ApplicationError:
    case TokenMissing                           extends Failure, UnauthorisedError
    case InvalidToken                           extends Failure, UnauthorisedError
    case TokenExpired                           extends Failure, UnauthorisedError
    case DecodeError(reason: Throwable)         extends Failure, FromException(reason)
    case CanNotGenerateToken(reason: Throwable) extends Failure, FromException(reason)

    override def kind: String = this match
      case TokenMissing           => "TokenMissing"
      case InvalidToken           => "InvalidToken"
      case TokenExpired           => "TokenExpired"
      case DecodeError(_)         => "DecodeError"
      case CanNotGenerateToken(_) => "CanNotGenerateToken"

    override def message: String = this match
      case TokenMissing                => "Token is missing"
      case InvalidToken                => "Invalid token provided"
      case TokenExpired                => "Token has expired"
      case DecodeError(reason)         => s"Can not decode token: ${reason.getMessage}"
      case CanNotGenerateToken(reason) => s"Can not generate token: ${reason.getMessage}"
