package conduit.domain.service.authentication

import conduit.domain.logic.authentication.TokenAuthenticator
import conduit.domain.logic.authentication.TokenAuthenticator.Failure
import conduit.domain.logic.monitoring.Monitor
import conduit.domain.model.entity.User
import conduit.domain.model.types.user.{ SignedToken, UserId }
import io.jsonwebtoken.security.Keys
import io.jsonwebtoken.{ Claims, JwtException, Jwts }
import izumi.reflect.Tag as ReflectionTag
import org.apache.commons.codec.digest.DigestUtils
import zio.{ Clock, ZIO, ZLayer }

import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.concurrent.TimeUnit
import scala.util.chaining.scalaUtilChainingOps

class TokenAuthenticationService[Tx](
  monitor: Monitor,
  conf: TokenAuthenticationService.Config,
) extends TokenAuthenticator[Tx] {

  override type Error = TokenAuthenticator.Failure // can only fail with TokenAuthenticator.Failure

  override def user(token: Option[SignedToken]): Result[User] =
    monitor.track("TokenAuthenticationService.user") {
      token match
        case Some(token) =>
          authenticate(token).catchSome:
            case Failure.InvalidToken => ZIO.succeed(User.Anonymous)
            case Failure.TokenExpired => ZIO.succeed(User.Anonymous)
            case Failure.TokenMissing => ZIO.succeed(User.Anonymous)
        case None        => ZIO.succeed(User.Anonymous)
    }

  override def authenticate(token: SignedToken): Result[User.Authenticated] =
    monitor.track("TokenAuthenticationService.authenticate") {
      for {
        payload <- decodeToken(token)
        _       <- checkExpiration(payload)
        userId  <- getUserId(payload)
      } yield User.Authenticated(userId)
    }

  override def generateToken(user: UserId): Result[SignedToken] =
    monitor.track("TokenAuthenticationService.generateToken") {
      Clock.currentTime(ChronoUnit.MILLIS).flatMap { now =>
        ZIO
          .attempt:
            Jwts
              .builder()
              // @todo include issuer
              .issuedAt(Date(now))
              .subject(user.toString)
              .expiration(Date(now + 3600000))
              .signWith(Keys.hmacShaKeyFor(conf.tokenSalt.getBytes))
              .compact()
              .pipe(SignedToken(_))
          .mapError(Failure.CanNotGenerateToken(_))
      }
    }

  private def decodeToken(token: SignedToken): Result[Claims] =
    ZIO
      .attempt:
        Jwts
          .parser()
          .verifyWith(Keys.hmacShaKeyFor(conf.tokenSalt.getBytes()))
          .build()
          .parseSignedClaims(token)
          .getPayload
      .mapError:
        case ex: JwtException => Failure.InvalidToken
        case ex               => Failure.DecodeError(ex)

  private def checkExpiration(payload: Claims): Result[Unit] =
    for {
      now    <- Clock.currentTime(TimeUnit.MILLISECONDS).map(Date(_))
      expired = Option(payload.getExpiration).forall(_.before(now))
      _      <- ZIO.when(expired)(ZIO.fail(Failure.TokenExpired))
    } yield ()

  private def getUserId(payload: Claims): Result[UserId] =
    ZIO
      .attempt:
        payload
          .getSubject
          .pipe(Option(_).get) // exception caught by ZIO.attempt
          .pipe(_.toLong)      // exception caught by ZIO.attempt
          .pipe(UserId(_))
      .mapError:
        case exc: NumberFormatException  => Failure.InvalidToken
        case exc: NoSuchElementException => Failure.InvalidToken
        case exc                         => Failure.DecodeError(exc)

  private def digest(s: String): ZIO[Any, Throwable, String] =
    ZIO.attempt(DigestUtils.sha256Hex(s))
}

object TokenAuthenticationService:
  case class Config(passwordSalt: String, tokenSalt: String)

  def layer[Tx: ReflectionTag]: ZLayer[Monitor & Config, Nothing, TokenAuthenticator[Tx]] =
    ZLayer {
      for {
        monitor <- ZIO.service[Monitor]
        config  <- ZIO.service[Config]
      } yield TokenAuthenticationService(monitor, config)
    }
