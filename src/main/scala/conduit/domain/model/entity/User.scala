package conduit.domain.model.entity

import conduit.domain.model.types.user.UserId

/**
 * Represents the authentication state of a user in the system.
 *
 * This enum models the two possible states a user can be in:
 * - Anonymous: User is not authenticated
 * - Authenticated: User is logged in with a valid user ID
 *
 * This design follows the principle of making invalid states unrepresentable,
 * ensuring that operations requiring authentication can safely access the user ID.
 */
enum User:
  /** Represents an unauthenticated user with no access to protected resources */
  case Anonymous

  /** Represents an authenticated user with a valid user ID for accessing protected resources */
  case Authenticated(userId: UserId)

  /**
   * Converts this User to an Option containing only the Authenticated case.
   *
   * This is useful for operations that need to work with authenticated users only,
   * allowing for easy pattern matching and safe extraction of the user ID.
   *
   * @return Some(Authenticated) if the user is authenticated, None if anonymous
   */
  def option: Option[User.Authenticated] = this match
    case User.Anonymous         => None
    case User.Authenticated(id) => Some(Authenticated(id))
