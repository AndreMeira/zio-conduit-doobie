package conduit.domain.model.entity

import conduit.domain.model.types.user.{ Biography, HashedPassword, UserId, UserImage, UserName }
import zio.prelude.Validation

import java.time.Instant

case class UserProfile(
  id: UserId,
  data: UserProfile.Data,
  metadata: UserProfile.Metadata,
)

object UserProfile:
  case class Metadata(
    createdAt: Instant,
    updatedAt: Instant,
  )

  case class Data(
    name: UserName,
    bio: Option[Biography],
    image: Option[UserImage],
  )
