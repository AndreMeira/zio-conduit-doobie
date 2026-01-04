package conduit.domain.logic.entrypoint

import conduit.domain.model.error.ApplicationError
import conduit.domain.model.request.UserRequest
import conduit.domain.model.request.user.*
import conduit.domain.model.response.UserResponse
import conduit.domain.model.response.user.*
import zio.ZIO

trait UserEntrypoint {
  type Result[A] = ZIO[Any, ApplicationError, A]

  def follow(request: FollowUserRequest): Result[GetProfileResponse]
  def unfollow(request: UnfollowUserRequest): Result[GetProfileResponse]
  def getProfile(request: GetProfileRequest): Result[GetProfileResponse]
  def update(request: UpdateUserRequest): Result[AuthenticationResponse]
  def login(request: AuthenticateRequest): Result[AuthenticationResponse]
  def getCurrent(request: GetUserRequest): Result[AuthenticationResponse]
  def register(request: RegistrationRequest): Result[AuthenticationResponse]

  def run(request: UserRequest): Result[UserResponse] = request match
    case r: FollowUserRequest   => follow(r)
    case r: GetProfileRequest   => getProfile(r)
    case r: UpdateUserRequest   => update(r)
    case r: AuthenticateRequest => login(r)
    case r: GetUserRequest      => getCurrent(r)
    case r: RegistrationRequest => register(r)
    case r: UnfollowUserRequest => unfollow(r)
}
