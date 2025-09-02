package conduit.domain.service.authentication

import conduit.domain.logic.authentication.CredentialsAuthenticator
import conduit.domain.logic.authentication.CredentialsAuthenticator.Failure
import conduit.domain.logic.authentication.CredentialsAuthenticator.Failure.*
import conduit.domain.logic.monitoring.Monitor
import conduit.domain.logic.persistence.UserRepository
import conduit.domain.model.entity.{ Credentials, User }
import conduit.domain.model.types.user.HashedPassword
import org.apache.commons.codec.digest.DigestUtils
import zio.ZIO

class CredentialsAuthenticationService[Tx](
  monitor: Monitor,
  conf: CredentialsAuthenticationService.Config,
  userRepository: UserRepository[Tx],
) extends CredentialsAuthenticator[Tx] {

  override def hash(credentials: Credentials.Clear): Result[Credentials.Hashed] =
    monitor.track("CredentialsAuthenticationService.hash") {
      digest(s"${credentials.password}.${conf.passwordSalt}")
        .mapError(Failure.CanNotHashPassword(_))
        .map(hashed => Credentials.Hashed(credentials.email, HashedPassword(hashed)))
    }

  override def authenticate(credentials: Credentials): Result[User.Authenticated] =
    monitor.track("CredentialsAuthenticationService.authenticate") {
      for {
        hashed <- credentials match
                    case c: Credentials.Clear  => hash(c)
                    case h: Credentials.Hashed => ZIO.succeed(h)
        userId <- userRepository.find(hashed)
        userId <- ZIO.fromOption(userId).orElseFail(InvalidCredentials)
      } yield User.Authenticated(userId)
    }

  private def digest(s: String): ZIO[Any, Throwable, String] =
    ZIO.attempt(DigestUtils.sha256Hex(s))
}

object CredentialsAuthenticationService:
  case class Config(passwordSalt: String)
