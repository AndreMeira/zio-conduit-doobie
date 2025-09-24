package conduit.domain.model.response.user

import conduit.domain.model.entity.UserProfile
import conduit.domain.model.types.user.{ Email, SignedToken }

case class AuthenticationResponse(user: AuthenticationResponse.Payload)

object AuthenticationResponse:
  case class Payload(
    email: String,
    token: String,
    username: String,
    bio: Option[String],
    image: Option[String],
  )

  def make(email: Email, profile: UserProfile, token: SignedToken): AuthenticationResponse =
    AuthenticationResponse(
      Payload(
        email = email,
        token = token,
        username = profile.data.name,
        bio = profile.data.bio,
        image = profile.data.image.map(_.toString),
      )
    )
