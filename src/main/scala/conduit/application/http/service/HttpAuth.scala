package conduit.application.http.service

import conduit.application.http.service
import conduit.application.http.service.HttpAuth.Failure
import conduit.domain.logic.authentication.TokenAuthenticator
import conduit.domain.logic.monitoring.Monitor
import conduit.domain.model.entity.User
import conduit.domain.model.error.ApplicationError
import conduit.domain.model.types.user.SignedToken
import izumi.reflect.Tag as ReflectionTag
import zio.http.Request
import zio.{ ZIO, ZLayer }

class HttpAuth(monitor: Monitor, val auth: TokenAuthenticator[Any]) {
  type Error     = HttpAuth.Failure | auth.Error
  type Result[A] = ZIO[Any, Error, A]

  /**
   * Any user, authenticated or anonymous.
   */
  def anyone(request: Request): Result[User] =
    monitor.track("HttpAuth.anyone"):
      currentUser(request)

  /**
   * Requires an authenticated user.
   * Fails if the user is anonymous.
   */
  def authenticated(request: Request): Result[User.Authenticated] =
    monitor.track("HttpAuth.authenticated"):
      currentUser(request).flatMap {
        case user: User.Authenticated => ZIO.succeed(user)
        case User.Anonymous           => ZIO.fail(Failure.AccessDenied("Authentication required"))
      }

  /**
   * Requires an anonymous user (not authenticated).
   * Fails if the user is authenticated.
   */
  def anonymous(request: Request): Result[User.Anonymous.type] =
    monitor.track("HttpAuth.anonymous"):
      currentUser(request).flatMap {
        case User.Anonymous        => ZIO.succeed(User.Anonymous)
        case _: User.Authenticated => ZIO.fail(Failure.AccessDenied("User must be anonymous"))
      }

  private def currentUser(request: Request): Result[User] = {
    val token  = request.headers.get("Authorization")
    val signed = token.map(SignedToken.fromString(_, "Token"))
    auth.user(signed)
  }
}

object HttpAuth:
  enum Failure extends ApplicationError {
    case AccessDenied(reason: String) extends Failure, ApplicationError.UnauthorisedError

    override def message: String = this match
      case AccessDenied(reason) => s"Access denied: $reason"
  }

  val layer: ZLayer[TokenAuthenticator[Any] & Monitor, Nothing, HttpAuth] = ZLayer {
    for {
      monitor <- ZIO.service[Monitor]
      auth    <- ZIO.service[TokenAuthenticator[Any]]
    } yield HttpAuth(monitor, auth)
  }
