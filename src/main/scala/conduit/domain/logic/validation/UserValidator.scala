package conduit.domain.logic.validation

import conduit.domain.model.entity.{ Credentials, UserProfile }
import conduit.domain.model.error.ApplicationError
import conduit.domain.model.error.ApplicationError.ValidationError
import conduit.domain.model.patching.{ CredentialsPatch, UserProfilePatch }
import conduit.domain.model.request.user.*
import conduit.domain.model.types.user.{ UserId, UserName }
import zio.ZIO
import zio.prelude.Validation

// @see https://lexi-lambda.github.io/blog/2019/11/05/parse-don-t-validate/

trait UserValidator[Tx] {
  type Error <: ApplicationError
  protected type Validated[A] = Validation[ValidationError, A]
  protected type Result[A]    = ZIO[Tx, Error, Validated[A]]

  protected type Patches      = (profile: List[UserProfilePatch], creds: List[CredentialsPatch])
  protected type Registration = (user: UserProfile.Data, creds: Credentials.Clear) // for readability only

  def parse(request: FollowUserRequest): Result[UserName]
  def parse(request: UnfollowUserRequest): Result[UserName]
  def parse(request: GetProfileRequest): Result[UserName]
  def parse(request: UpdateUserRequest): Result[Patches]
  def parse(request: AuthenticateRequest): Result[Credentials.Clear]
  def parse(request: GetUserRequest): Result[UserId]
  def parse(request: RegistrationRequest): Result[Registration]
}
