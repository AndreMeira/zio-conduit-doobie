package conduit.domain.model.entity

import conduit.domain.model.types.user.{ Email, HashedPassword, Password }

/**
 * Represents user authentication credentials in different states.
 *
 * This enum ensures type safety by distinguishing between plain-text passwords
 * (as received from users) and hashed passwords (as stored in the database).
 * This design prevents accidentally storing clear-text passwords or attempting
 * to authenticate with already-hashed passwords.
 */
enum Credentials:
  /**
   * Plain-text credentials as provided by the user during authentication.
   *
   * Used when receiving login requests or registration data before password hashing.
   *
   * @param email    User's email address
   * @param password Plain-text password (never stored in this form)
   */
  case Clear(email: Email, password: Password)

  /**
   * Hashed credentials suitable for storage and authentication.
   *
   * Used for database storage and when comparing against stored credentials.
   * The password has been cryptographically hashed for security.
   *
   * @param email    User's email address
   * @param password Cryptographically hashed password
   */
  case Hashed(email: Email, password: HashedPassword)
