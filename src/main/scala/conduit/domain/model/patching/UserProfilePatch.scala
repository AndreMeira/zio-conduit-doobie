package conduit.domain.model.patching

import conduit.domain.model.entity.UserProfile
import conduit.domain.model.types.user.{ Biography, UserImage, UserName, Email as UserEmail }
import zio.prelude.Validation

/**
 * A patch to be applied to a user profile.
 * Each patch represents a change to a single field.
 * Patches can be combined to form a complete update to a user profile.
 */
enum UserProfilePatch:
  case Name(value: UserName)
  case Email(value: UserEmail)
  case Bio(value: Option[Biography])
  case Image(value: Option[UserImage])

  def apply(data: UserProfile.Data): UserProfile.Data =
    this match
      case UserProfilePatch.Name(value)  => data.copy(name = value)
      case UserProfilePatch.Email(value) => data.copy(email = value)
      case UserProfilePatch.Bio(value)   => data.copy(bio = value)
      case UserProfilePatch.Image(value) => data.copy(image = value)

object UserProfilePatch:
  def apply(value: UserProfile, patches: List[UserProfilePatch]): UserProfile =
    value.copy(data = apply(value.data, patches))

  def apply(value: UserProfile.Data, patches: List[UserProfilePatch]): UserProfile.Data =
    patches.foldLeft(value)((d, p) => p.apply(d))

  def name(value: String): Validation[UserName.Error, UserProfilePatch.Name] =
    UserName.fromString(value).map(UserProfilePatch.Name.apply)

  def email(value: String): Validation[UserEmail.Error, UserProfilePatch.Email] =
    UserEmail.fromString(value).map(UserProfilePatch.Email.apply)

  def bio(value: Option[String]): Validation[Biography.Error, UserProfilePatch.Bio] =
    value match
      case Some(bio) => Biography.fromString(bio).map(b => UserProfilePatch.Bio(Some(b)))
      case None      => Validation.succeed(UserProfilePatch.Bio(None))

  def image(value: Option[String]): Validation[UserImage.Error, UserProfilePatch.Image] =
    value match
      case Some(img) => UserImage.fromString(img).map(i => UserProfilePatch.Image(Some(i)))
      case None      => Validation.succeed(UserProfilePatch.Image(None))
