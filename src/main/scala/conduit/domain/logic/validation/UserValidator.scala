package conduit.domain.logic.validation

import conduit.domain.model.entity.{ Credentials, UserProfile }
import conduit.domain.model.error.ApplicationError.{ TransientError, ValidationError }
import conduit.domain.model.patching.UserProfilePatch
import conduit.domain.model.request.user.*
import conduit.domain.model.types.article.AuthorId
import conduit.domain.model.types.user.{ UserId, UserName }
import zio.ZIO
import zio.prelude.Validation

trait UserValidator[Tx] {
  protected type Validated[A] = Validation[ValidationError, A]
  protected type Result[A]    = ZIO[Tx, TransientError, Validated[A]]
  protected type Registration = (user: UserProfile.Data, creds: Credentials.Clear) // for readability only

  def validate(request: FollowUserRequest): Result[AuthorId]
  def validate(request: UnfollowUserRequest): Result[AuthorId]
  def validate(request: GetProfileRequest): Result[UserName]
  def validate(request: UpdateUserRequest): Result[List[UserProfilePatch]]
  def validate(request: AuthenticateRequest): Result[Credentials.Clear]
  def validate(request: GetUserRequest): Result[UserId]
  def validate(request: RegistrationRequest): Result[Registration]
}
