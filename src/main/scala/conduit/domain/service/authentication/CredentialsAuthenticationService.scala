package conduit.domain.service.authentication

import conduit.domain.logic.authentication.CredentialsAuthenticator
import conduit.domain.logic.authentication.CredentialsAuthenticator.Failure
import conduit.domain.logic.authentication.CredentialsAuthenticator.Failure.*
import conduit.domain.logic.monitoring.Monitor
import conduit.domain.logic.persistence.UserRepository
import conduit.domain.model.entity.{ Credentials, User }
import conduit.domain.model.types.user.{ HashedPassword, Password }
import izumi.reflect.Tag
import org.apache.commons.codec.digest.DigestUtils
import zio.{ ZIO, ZLayer }

class CredentialsAuthenticationService[Tx](
  monitor: Monitor,
  conf: CredentialsAuthenticationService.Config,
  val userRepository: UserRepository[Tx],
) extends CredentialsAuthenticator[Tx] {

  // can fail with same errors as the injected repository or with CredentialsAuthenticator.Failure
  override type Error = userRepository.Error | CredentialsAuthenticator.Failure

  override def hash(password: Password): Result[HashedPassword] =
    monitor.track("CredentialsAuthenticationService.hash") {
      digest(s"${password}.${conf.passwordSalt}")
        .mapError(Failure.CanNotHashPassword(_))
        .map(hashed => HashedPassword(hashed))
    }

  override def authenticate(credentials: Credentials): Result[User.Authenticated] =
    monitor.track("CredentialsAuthenticationService.authenticate") {
      for {
        hashed <- credentials match
                    case c: Credentials.Clear  => hashCredentials(c)
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

  def layer[Tx: Tag]: ZLayer[UserRepository[Tx] & Config & Monitor, Nothing, CredentialsAuthenticator[Tx]] =
    ZLayer {
      for {
        monitor        <- ZIO.service[Monitor]
        config         <- ZIO.service[Config]
        userRepository <- ZIO.service[UserRepository[Tx]]
      } yield CredentialsAuthenticationService(monitor, config, userRepository)
    }
