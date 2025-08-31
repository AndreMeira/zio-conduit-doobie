package conduit.domain.model.response

import conduit.domain.model.request.UserRequest
import conduit.domain.model.request.user.*
import conduit.domain.model.response.user.*

type UserResponse = UserResponse.Type
object UserResponse:
  type Type = AuthenticationResponse | ProfileResponse

  type Of[A <: UserRequest] = A match
    case AuthenticateRequest => AuthenticationResponse
    case GetUserRequest      => AuthenticationResponse
    case RegistrationRequest => AuthenticationResponse
    case UpdateUserRequest   => AuthenticationResponse
    case FollowUserRequest   => ProfileResponse
    case GetProfileRequest   => ProfileResponse
    case UnfollowUserRequest => ProfileResponse
