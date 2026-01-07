package conduit.domain.logic.authentication

import conduit.domain.model.entity.{ Credentials, User }
import conduit.domain.model.error.ApplicationError
import conduit.domain.model.error.ApplicationError.{ FromException, TransientError, UnauthorisedError }
import conduit.domain.model.types.user.{ HashedPassword, Password }
import zio.ZIO

/**
 * Interface for user authentication and password hashing operations.
 *
 * Provides secure credential handling including password hashing and
 * authentication verification. This trait is parameterized by transaction type
 * to support different implementation strategies (e.g., BCrypt, Argon2).
 *
 * @tparam Tx The transaction context type
 */
trait CredentialsAuthenticator[Tx] {
  /** Error type constraint that includes authentication-specific failures */
  type Error >: CredentialsAuthenticator.Failure <: ApplicationError

  /** Result type that includes transaction context and error handling */
  protected type Result[A] = ZIO[Tx, Error, A]

  /**
   * Authenticates a user by verifying their credentials.
   *
   * Validates the provided credentials against stored hashed credentials
   * and returns an authenticated user if successful.
   *
   * @param credential User credentials to authenticate
   * @return Authenticated user on success, failure on invalid credentials
   */
  def authenticate(credential: Credentials): Result[User.Authenticated]

  /**
   * Hashes a plain-text password using a secure hashing algorithm.
   *
   * Uses a cryptographically secure hashing function (e.g., BCrypt)
   * with appropriate salt and work factors for password storage.
   *
   * @param password Plain-text password to hash
   * @return Securely hashed password
   */
  def hash(password: Password): Result[HashedPassword]

  /**
   * Convenience method to hash credentials by extracting and hashing the password.
   *
   * Takes clear-text credentials and returns hashed credentials suitable
   * for storage, preserving the email while hashing the password.
   *
   * @param credentials Clear-text credentials
   * @return Credentials with hashed password
   */
  def hashCredentials(credentials: Credentials.Clear): Result[Credentials.Hashed] =
    hash(credentials.password).map(Credentials.Hashed(credentials.email, _))
}

object CredentialsAuthenticator:
  /**
   * Enumeration of authentication-specific failures.
   */
  enum Failure extends ApplicationError:
    /** Authentication failed due to invalid credentials */
    case InvalidCredentials                    extends Failure, UnauthorisedError

    /** Password hashing operation failed due to system error */
    case CanNotHashPassword(reason: Throwable) extends Failure, FromException(reason)

    override def kind: String = this match
      case InvalidCredentials    => "InvalidCredentials"
      case CanNotHashPassword(_) => "CanNotHashPassword"

    override def message: String = this match
      case CanNotHashPassword(reason) => s"Can not hash password: ${reason.getMessage}"
      case InvalidCredentials         => "Invalid credentials provided"
