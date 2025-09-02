package conduit.domain.logic.endpoint

import conduit.domain.model.error.ApplicationError
import conduit.domain.model.request.UserRequest
import conduit.domain.model.request.user.*
import conduit.domain.model.response.UserResponse
import conduit.domain.model.response.user.*
import zio.ZIO

trait UserEndpoint[Tx] {
  type Result[A] = ZIO[Tx, ApplicationError, A]

  def follow(request: FollowUserRequest): Result[ProfileResponse]
  def unfollow(request: UnfollowUserRequest): Result[ProfileResponse]
  def getProfile(request: GetProfileRequest): Result[ProfileResponse]
  def update(request: UpdateUserRequest): Result[AuthenticationResponse]
  def login(request: AuthenticateRequest): Result[AuthenticationResponse]
  def getCurrent(request: GetUserRequest): Result[AuthenticationResponse]
  def register(request: RegistrationRequest): Result[AuthenticationResponse]

  def handle(request: UserRequest): Result[UserResponse] = request match
    case r: FollowUserRequest   => follow(r)
    case r: GetProfileRequest   => getProfile(r)
    case r: UpdateUserRequest   => update(r)
    case r: AuthenticateRequest => login(r)
    case r: GetUserRequest      => getCurrent(r)
    case r: RegistrationRequest => register(r)
    case r: UnfollowUserRequest => unfollow(r)
}
