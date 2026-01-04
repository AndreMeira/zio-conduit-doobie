package conduit.domain.model.request.user

import conduit.domain.model.entity.{ User, UserProfile }
import conduit.domain.model.error.ApplicationError.ValidationError
import conduit.domain.model.patching.UserProfilePatch
import conduit.domain.model.request.Patchable
import conduit.domain.model.request.Patchable.Empty
import zio.prelude.Validation

case class UpdateUserRequest(
  requester: User.Authenticated,
  payload: UpdateUserRequest.Payload,
)

object UpdateUserRequest:
  case class Payload(user: Data) // wrapping due to api spec

  case class Data(
    email: Patchable[String],
    username: Patchable[String],
    password: Patchable[String],
    bio: Patchable[String],
    image: Patchable[String],
  )
