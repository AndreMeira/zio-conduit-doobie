package conduit.domain.model.response.user

case class ProfileResponse(profile: ProfileResponse.Payload)

object ProfileResponse:
  case class Payload(
      username: String,
      bio: Option[String],
      image: Option[String],
      following: Boolean,
    )
