package conduit.domain.model.entity

import conduit.domain.model.types.user.{ Biography, HashedPassword, UserId, UserImage, UserName }
import zio.prelude.Validation

import java.time.Instant

/**
 * Represents a user's public profile information in the Conduit system.
 *
 * The UserProfile contains all publicly visible information about a user,
 * separate from their authentication credentials. This separation allows
 * for clean display of user information without exposing sensitive data.
 *
 * @param id       Unique identifier for the user
 * @param data     Public profile information (name, bio, image)
 * @param metadata Timestamps for creation and last update
 */
case class UserProfile(
  id: UserId,
  data: UserProfile.Data,
  metadata: UserProfile.Metadata,
)

object UserProfile:
  /**
   * Metadata associated with a user profile, containing audit information.
   *
   * @param createdAt Timestamp when the profile was first created
   * @param updatedAt Timestamp when the profile was last modified
   */
  case class Metadata(
    createdAt: Instant,
    updatedAt: Instant,
  )

  /**
   * Public profile data that can be displayed to other users.
   *
   * @param name  Display name for the user (publicly visible)
   * @param bio   Optional biographical information about the user
   * @param image Optional URL to the user's profile image/avatar
   */
  case class Data(
    name: UserName,
    bio: Option[Biography],
    image: Option[UserImage],
  )
