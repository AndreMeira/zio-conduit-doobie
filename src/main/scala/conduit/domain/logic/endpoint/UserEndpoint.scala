package conduit.domain.logic.endpoint

import conduit.domain.model.error.ApplicationError
import conduit.domain.model.request.user.*
import conduit.domain.model.response.user.*
import zio.ZIO

trait UserEndpoint[Tx] {
  def register(request: RegistrationRequest): ZIO[Tx, ApplicationError, AuthenticationResponse]
  def login(request: AuthenticateRequest): ZIO[Tx, ApplicationError, AuthenticationResponse]
  def getCurrent(request: GetUserRequest): ZIO[Tx, ApplicationError, AuthenticationResponse]
  def update(request: UpdateUserRequest): ZIO[Tx, ApplicationError, AuthenticationResponse]
  def follow(request: FollowUserRequest): ZIO[Tx, ApplicationError, ProfileResponse]
  def unfollow(request: UnfollowUserRequest): ZIO[Tx, ApplicationError, ProfileResponse]
  def getProfile(request: GetProfileRequest): ZIO[Tx, ApplicationError, ProfileResponse]
}
