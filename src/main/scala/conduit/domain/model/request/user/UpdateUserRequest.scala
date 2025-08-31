package conduit.domain.model.request.user

import conduit.domain.model.entity.User
import conduit.domain.model.request.Patchable

case class UpdateUserRequest(requester: User, payload: UpdateUserRequest.Payload)

object UpdateUserRequest:
  case class Payload(user: Data) // wrapping due to api spec
  case class Data(
      email: Patchable[String],
      username: Patchable[String],
      password: Patchable[String],
      bio: Patchable[String],
      image: Patchable[String],
    )
