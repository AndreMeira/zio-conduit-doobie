package conduit.domain.model.entity

import conduit.domain.model.types.user.{ Biography, HashedPassword, UserId, UserImage, UserName, Email as UserEmail }
import zio.prelude.Validation

import java.time.Instant

case class UserProfile(id: UserId, data: UserProfile.Data, metadata: UserProfile.Metadata)

object UserProfile:
  case class Metadata(createdAt: Instant, updatedAt: Instant)
  case class Data(name: UserName, email: UserEmail, bio: Option[Biography], image: Option[UserImage])

  enum Patch:
    case Name(value: UserName)
    case Email(value: UserEmail)
    case Bio(value: Option[Biography])
    case Image(value: Option[UserImage])

  object Patch:
    def name(value: String): Validation[UserName.Error, Patch.Name] =
      UserName.fromString(value).map(Patch.Name.apply)

    def email(value: String): Validation[UserEmail.Error, Patch.Email] =
      UserEmail.fromString(value).map(Patch.Email.apply)

    def bio(value: Option[String]): Validation[Biography.Error, Patch.Bio] =
      value match
        case Some(bio) => Biography.fromString(bio).map(b => Patch.Bio(Some(b)))
        case None      => Validation.succeed(Patch.Bio(None))

    def image(value: Option[String]): Validation[UserImage.Error, Patch.Image] =
      value match
        case Some(img) => UserImage.fromString(img).map(i => Patch.Image(Some(i)))
        case None      => Validation.succeed(Patch.Image(None))

    def patch(data: Data, patches: List[Patch]): Data =
      patches.foldLeft(data) { (acc, patch) =>
        patch match
          case Patch.Name(value)  => acc.copy(name = value)
          case Patch.Email(value) => acc.copy(email = value)
          case Patch.Bio(value)   => acc.copy(bio = value)
          case Patch.Image(value) => acc.copy(image = value)
      }
