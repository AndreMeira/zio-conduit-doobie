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

/**
 * Interface for validating and parsing user-related requests.
 *
 * This trait follows the "parse, don't validate" principle, converting
 * raw request data into validated domain objects or reporting specific
 * validation errors. Each parse method returns a ZIO that can fail with
 * validation errors or succeed with validated data.
 *
 * @tparam Tx The transaction context type
 */
trait UserValidator[Tx] {
  /** Error type constraint for validation operations */
  type Error <: ApplicationError

  /** Validation result type that accumulates validation errors */
  protected type Validated[A] = Validation[ValidationError, A]

  /** Result type combining validation with transaction context */
  protected type Result[A]    = ZIO[Tx, Error, Validated[A]]

  /** Type alias for update patches (profile patches and credential patches) */
  protected type Patches      = (profile: List[UserProfilePatch], creds: List[CredentialsPatch])

  /** Type alias for registration data (profile data and clear credentials) */
  protected type Registration = (user: UserProfile.Data, creds: Credentials.Clear)

  /**
   * Validates and extracts the username from a follow user request.
   *
   * @param request Follow user request to validate
   * @return Validated username or validation errors
   */
  def parse(request: FollowUserRequest): Result[UserName]

  /**
   * Validates and extracts the username from an unfollow user request.
   *
   * @param request Unfollow user request to validate
   * @return Validated username or validation errors
   */
  def parse(request: UnfollowUserRequest): Result[UserName]

  /**
   * Validates and extracts the username from a get profile request.
   *
   * @param request Get profile request to validate
   * @return Validated username or validation errors
   */
  def parse(request: GetProfileRequest): Result[UserName]

  /**
   * Validates and extracts update patches from an update user request.
   *
   * Separates the request into profile updates and credential updates,
   * validating each set of changes independently.
   *
   * @param request Update user request to validate
   * @return Validated patches (profile and credential changes) or validation errors
   */
  def parse(request: UpdateUserRequest): Result[Patches]

  /**
   * Validates and extracts credentials from an authentication request.
   *
   * @param request Authentication request to validate
   * @return Validated clear credentials or validation errors
   */
  def parse(request: AuthenticateRequest): Result[Credentials.Clear]

  /**
   * Validates and extracts the user ID from a get user request.
   *
   * @param request Get user request to validate
   * @return Validated user ID or validation errors
   */
  def parse(request: GetUserRequest): Result[UserId]

  /**
   * Validates and extracts registration data from a registration request.
   *
   * Validates both the profile information and credentials needed to
   * create a new user account.
   *
   * @param request Registration request to validate
   * @return Validated registration data (profile and credentials) or validation errors
   */
  def parse(request: RegistrationRequest): Result[Registration]
}
