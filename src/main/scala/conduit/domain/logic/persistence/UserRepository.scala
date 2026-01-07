package conduit.domain.logic.persistence

import conduit.domain.model.entity.Credentials
import conduit.domain.model.error.ApplicationError
import conduit.domain.model.error.ApplicationError.TransientError
import conduit.domain.model.types.user.{ Email, UserId }
import zio.ZIO

/**
 * Repository interface for user authentication and credential management.
 *
 * Provides persistence operations for user credentials, including creation,
 * retrieval, and validation. This trait is parameterized by transaction type
 * to support different persistence implementations (PostgreSQL, in-memory, etc.).
 *
 * @tparam Tx The transaction context type (e.g., PostgresTransaction, MemoryTransaction)
 */
trait UserRepository[Tx] {
  /** Error type constraint for repository operations */
  type Error <: ApplicationError

  /** Result type that includes transaction context and error handling */
  protected type Result[A] = ZIO[Tx, Error, A]

  /**
   * Checks if a user with the given ID exists in the system.
   *
   * @param userId User ID to check
   * @return true if user exists, false otherwise
   */
  def exists(userId: UserId): Result[Boolean]

  /**
   * Creates a new user account with the provided credentials.
   *
   * @param credential Hashed credentials for the new user
   * @return The newly assigned user ID
   */
  def save(credential: Credentials.Hashed): Result[UserId]

  /**
   * Updates the credentials for an existing user.
   *
   * @param userId     ID of the user to update
   * @param credential New hashed credentials
   * @return Unit on success
   */
  def save(userId: UserId, credential: Credentials.Hashed): Result[Unit]

  /**
   * Finds a user ID by matching credentials for authentication.
   *
   * @param credential Hashed credentials to match against
   * @return Some(userId) if credentials match, None if no match found
   */
  def find(credential: Credentials.Hashed): Result[Option[UserId]]

  /**
   * Retrieves the email address associated with a user ID.
   *
   * @param userId User ID to look up
   * @return Some(email) if found, None if user has no email
   */
  def findEmail(userId: UserId): Result[Option[Email]]

  /**
   * Checks if an email address is already registered in the system.
   *
   * @param email Email address to check
   * @return true if email exists, false otherwise
   */
  def emailExists(email: Email): Result[Boolean]

  /**
   * Retrieves the stored credentials for a user.
   *
   * @param userId User ID to look up
   * @return Some(credentials) if found, None if user has no credentials
   */
  def findCredentials(userId: UserId): Result[Option[Credentials.Hashed]]
}
