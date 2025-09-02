package conduit.application.http.guard

import conduit.application.http.guard
import conduit.application.http.guard.RequestGuard.Failure
import conduit.domain.logic.authentication.TokenAuthenticator
import conduit.domain.model.entity.User
import conduit.domain.model.error.ApplicationError
import conduit.domain.model.types.user.SignedToken
import io.circe.{ Decoder, parser }
import zio.ZIO
import zio.http.Request

class RequestGuard(auth: TokenAuthenticator[Any]) {
  type Error                 = RequestGuard.Failure | auth.Error
  type Result[A]             = ZIO[Any, Error, A]
  type Decoded[A, U <: User] = (decoded: A, user: U)

  def anyone(request: Request): Result[User] = currentUser(request)

  def authenticated(request: Request): Result[User.Authenticated] =
    currentUser(request).flatMap {
      case user: User.Authenticated => ZIO.succeed(user)
      case User.Anonymous           => ZIO.fail(Failure.AccessDenied("Authentication required"))
    }

  def anonymous(request: Request): Result[User.Anonymous.type] =
    currentUser(request).flatMap {
      case User.Anonymous        => ZIO.succeed(User.Anonymous)
      case _: User.Authenticated => ZIO.fail(Failure.AccessDenied("User must be anonymous"))
    }

  def anyoneDecode[A: Decoder](request: Request): Result[Decoded[A, User]] =
    for {
      user    <- anyone(request)
      decoded <- decode[A](request)
    } yield (decoded = decoded, user = user)

  def authenticatedDecode[A: Decoder](request: Request): Result[Decoded[A, User.Authenticated]] =
    for {
      user    <- authenticated(request)
      decoded <- decode[A](request)
    } yield (decoded = decoded, user = user)

  def anonymousDecode[A: Decoder](request: Request): Result[Decoded[A, User.Anonymous.type]] =
    for {
      user    <- anonymous(request)
      decoded <- decode[A](request)
    } yield (decoded = decoded, user = user)

  private def currentUser(request: Request): ZIO[Any, Error, User] =
    for {
      token <- ZIO.succeed(request.headers.get("Authorization"))
      signed = token.map(SignedToken.fromString(_, "Token"))
      user  <- auth.user(signed)
    } yield user

  private def decode[A: Decoder](request: Request): ZIO[Any, Failure, A] =
    for {
      body   <- request.body.asString.mapError(_ => Failure.EmptyBody)
      decoded = parser.decode[A](body).left.map(err => Failure.InvalidJson(err.getMessage))
      result <- ZIO.fromEither(decoded)
    } yield result

}

object RequestGuard:
  enum Failure extends ApplicationError:
    case EmptyBody
    case InvalidJson(reason: String)
    case AccessDenied(reason: String) extends Failure, ApplicationError.UnauthorisedError

    override def message: String = this match
      case EmptyBody            => "Request body cannot be empty"
      case InvalidJson(reason)  => s"Invalid JSON: $reason"
      case AccessDenied(reason) => s"Access denied: $reason"
