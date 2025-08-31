package conduit.domain.model.response.user

case class AuthenticationResponse(user: AuthenticationResponse.Payload)

object AuthenticationResponse:
  case class Payload(
      email: String,
      token: String,
      username: String,
      bio: Option[String],
      image: Option[String],
    )
