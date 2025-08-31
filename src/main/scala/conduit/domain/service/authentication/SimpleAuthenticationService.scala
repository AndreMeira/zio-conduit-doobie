package conduit.domain.service.authentication

import conduit.domain.logic.authentication.Authentication
import conduit.domain.logic.authentication.Authentication.Failure.*
import conduit.domain.logic.persistence.{ UserRepository, UserProfileRepository }
import conduit.domain.model.entity.{ Credentials, User, UserProfile }
import conduit.domain.model.types.user.{ HashedPassword, SignedToken, Password, UserId }
import io.jsonwebtoken.security.Keys
import io.jsonwebtoken.{ Claims, Jwts }
import org.apache.commons.codec.digest.DigestUtils
import zio.{ Clock, ZIO }

import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.concurrent.TimeUnit
import scala.util.chaining.scalaUtilChainingOps

class SimpleAuthenticationService[Tx](
    conf: SimpleAuthenticationService.Config,
    userRepository: UserRepository[Tx],
  ) extends Authentication[Tx] {

  override def hash(credentials: Credentials.Clear): ZIO[Tx, Authentication.Error, Credentials.Hashed] =
    digest(s"${credentials.password}.${conf.passwordSalt}")
      .mapError(CanNotHashPassword(_))
      .map(hashed => Credentials.Hashed(credentials.email, HashedPassword(hashed)))

  override def generateToken(user: UserId): ZIO[Tx, Authentication.Error, SignedToken] =
    Clock.currentTime(ChronoUnit.MILLIS).flatMap { current =>
      ZIO
        .attempt:
          Jwts
            .builder()
            // @todo include issuer
            .issuedAt(Date(current))
            .subject(user.toString)
            .expiration(Date(current + 3600000))
            .signWith(Keys.hmacShaKeyFor(conf.tokenSalt.getBytes))
            .compact()
            .pipe(SignedToken(_))
        .mapError(CanNotGenerateToken(_))
    }

  override def authenticate(credentials: Credentials): ZIO[Tx, Authentication.Error, User.Authenticated] =
    for {
      userId <- userRepository.find(credentials)
      userId <- ZIO.fromOption(userId).orElseFail(InvalidCredentials)
    } yield User.Authenticated(userId)

  override def user(token: Option[SignedToken]): ZIO[Tx, Authentication.Error, User] =
    token match {
      case None        => ZIO.succeed(User.Anonymous)
      case Some(token) =>
        authenticate(token).catchSome {
          case InvalidCredentials => ZIO.succeed(User.Anonymous)
          case TokenExpired       => ZIO.succeed(User.Anonymous)
        }
    }

  override def authenticate(token: SignedToken): ZIO[Tx, Authentication.Error, User.Authenticated] =
    for {
      payload <- decodeToken(token)
      _       <- checkExpiration(payload)
      userId  <- getUserId(payload)
    } yield User.Authenticated(userId)

  private def decodeToken(token: SignedToken): ZIO[Any, Authentication.Failure, Claims] =
    ZIO
      .attempt:
        Jwts
          .parser()
          .verifyWith(Keys.hmacShaKeyFor(conf.tokenSalt.getBytes()))
          .build()
          .parseSignedClaims(token)
          .getPayload
      .mapError(_ => InvalidCredentials)

  private def checkExpiration(payload: Claims): ZIO[Any, Authentication.Failure, Unit] =
    for {
      now    <- Clock.currentTime(TimeUnit.MILLISECONDS).map(Date(_))
      expired = Option(payload.getExpiration).forall(_.before(now))
      _      <- ZIO.when(expired)(ZIO.fail(TokenExpired))
    } yield ()

  private def getUserId(payload: Claims): ZIO[Any, Authentication.Failure, UserId] =
    ZIO
      .attempt:
        payload
          .getSubject
          .pipe(Option(_).getOrElse(""))
          .pipe(_.toLong)
          .pipe(UserId(_))
      .mapError(_ => InvalidCredentials)

  private def digest(s: String): ZIO[Any, Throwable, String] =
    ZIO.attempt(DigestUtils.sha256Hex(s))
}

object SimpleAuthenticationService:
  case class Config(passwordSalt: String, tokenSalt: String)
