package conduit.domain.model.response.user

import conduit.domain.model.entity.UserProfile

case class GetProfileResponse(profile: GetProfileResponse.Payload)

object GetProfileResponse:
  case class Payload(
    username: String,
    bio: Option[String],
    image: Option[String],
    following: Boolean,
  )

  def make(user: UserProfile, following: Boolean): GetProfileResponse =
    GetProfileResponse(
      Payload(
        username = user.data.name,
        bio = user.data.bio,
        image = user.data.image.map(_.toString),
        following = following,
      )
    )
