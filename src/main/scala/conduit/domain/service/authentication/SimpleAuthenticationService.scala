package conduit.domain.service.authentication

import conduit.domain.logic.authentication.Authentication
import conduit.domain.logic.authentication.Authentication.Failure.*
import conduit.domain.logic.persistence.{UserCredentialRepository, UserRepository}
import conduit.domain.model.entity.{Credential, Requester, User}
import conduit.domain.model.types.user.{HashedPassword, HashedToken, Password, UserId}
import io.jsonwebtoken.security.Keys
import io.jsonwebtoken.{Claims, Jwts}
import org.apache.commons.codec.digest.DigestUtils
import zio.{Clock, ZIO}

import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.concurrent.TimeUnit
import scala.util.chaining.scalaUtilChainingOps

class SimpleAuthenticationService[Tx](
  conf: SimpleAuthenticationService.Config,
  userRepository: UserRepository[Tx],
  userCredsRepository: UserCredentialRepository[Tx],
) extends Authentication[Tx] {

  override def hashPassword(password: Password): ZIO[Tx, Authentication.Error, HashedPassword] = {
    digest(s"${password}.${conf.passwordSalt}")
      .map(HashedPassword(_))
      .mapError(CanNotHashPassword(_))
  }

  override def generateToken(user: User): ZIO[Tx, Authentication.Error, HashedToken] = {
    // @todo include issuer
    Clock.currentTime(ChronoUnit.MILLIS).flatMap { current =>
      ZIO.attempt {
        Jwts.builder()
          .issuedAt(Date(current))
          .subject(user.id.toString)
          .expiration(Date(current + 3600000))
          .signWith(Keys.hmacShaKeyFor(conf.tokenSalt.getBytes))
          .compact()
          .pipe(HashedToken(_))
      }.mapError(CanNotGenerateToken(_))
    }
  }

  override def authenticate(credential: Credential): ZIO[Tx, Authentication.Error, User] =
    for {
      credsOpt <- userCredsRepository.find(credential)
      creds    <- ZIO.fromOption(credsOpt).orElseFail(InvalidCredentials)
      userOpt  <- userRepository.findByEmail(creds.email)
      _        <- ZIO.when(userOpt.isEmpty):
        ZIO.logError(s"User ${creds.email} not found for existing credentials")
      user     <- ZIO.fromOption(userOpt).orElseFail(InvalidCredentials)
    } yield user

  override def requester(token: Option[HashedToken]): ZIO[Tx, Authentication.Error, Requester] =
    token match {
      case None        => ZIO.succeed(Requester.Anonymous)
      case Some(token) => requester(token)
    }

  private def requester(token: HashedToken): ZIO[Tx, Authentication.Error, Requester] = {
    // @todo check issuer
    for {
      payload <- decodeToken(token)
      _       <- checkExpiration(payload)
      userId  <- getUserId(payload)
    } yield Requester.Authenticated(userId)
  }

  private def decodeToken(token: HashedToken): ZIO[Any, Authentication.Failure, Claims] = {
    ZIO.attempt {
      Jwts.parser()
        .verifyWith(Keys.hmacShaKeyFor(conf.tokenSalt.getBytes()))
        .build()
        .parseSignedClaims(token)
        .getPayload
    }
  }.mapError(_ => InvalidCredentials)

  private def checkExpiration(payload: Claims): ZIO[Any, Authentication.Failure, Unit] = {
    for {
      current <- Clock.currentTime(TimeUnit.MILLISECONDS)
      exp     = Option(payload.getExpiration).getOrElse(new Date(0))
      _       <- ZIO.when(exp.before(new Date(current)))(ZIO.fail(InvalidCredentials))
    } yield ()
  }

  private def getUserId(payload: Claims): ZIO[Any, Authentication.Failure, UserId] = {
    ZIO.attempt {
      payload
        .getSubject
        .pipe(Option(_).getOrElse(""))
        .pipe(_.toLong)
        .pipe(UserId(_))
    }.mapError(_ => InvalidCredentials)
  }

  private def digest(s: String): ZIO[Any, Throwable, String] = {
    ZIO.attempt(DigestUtils.sha256Hex(s))
  }
}

object SimpleAuthenticationService {
  case class Config(passwordSalt: String, tokenSalt: String)

}
