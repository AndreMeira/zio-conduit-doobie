package conduit.domain.model.request

import conduit.domain.model.request.user.*

type UserRequest = UserRequest.Type
object UserRequest:
  type Type =
    AuthenticateRequest
    | FollowUserRequest
    | GetProfileRequest
    | GetUserRequest
    | RegistrationRequest
    | UnfollowUserRequest
    | UpdateUserRequest
