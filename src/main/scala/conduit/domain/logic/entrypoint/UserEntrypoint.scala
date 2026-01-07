package conduit.domain.logic.entrypoint

import conduit.domain.model.error.ApplicationError
import conduit.domain.model.request.UserRequest
import conduit.domain.model.request.user.*
import conduit.domain.model.response.UserResponse
import conduit.domain.model.response.user.*
import zio.ZIO

/**
 * Entry point interface for all user-related operations in the Conduit system.
 *
 * This trait defines the business operations available for user management,
 * including authentication, profile management, and social features like following.
 * It serves as the primary interface between the application layer and domain logic.
 */
trait UserEntrypoint {
  /** Result type for all user operations */
  type Result[A] = ZIO[Any, ApplicationError, A]

  /**
   * Creates a follower relationship between the current user and another user.
   *
   * @param request Contains the username to follow and current user context
   * @return Profile response with updated following status
   */
  def follow(request: FollowUserRequest): Result[GetProfileResponse]

  /**
   * Removes a follower relationship between the current user and another user.
   *
   * @param request Contains the username to unfollow and current user context
   * @return Profile response with updated following status
   */
  def unfollow(request: UnfollowUserRequest): Result[GetProfileResponse]

  /**
   * Retrieves the public profile information for a user.
   *
   * @param request Contains the username to retrieve and optional current user context
   * @return Public profile information including following status
   */
  def getProfile(request: GetProfileRequest): Result[GetProfileResponse]

  /**
   * Updates the current user's profile and/or credentials.
   *
   * @param request Contains the user updates and authentication context
   * @return Updated user information with new authentication token if needed
   */
  def update(request: UpdateUserRequest): Result[AuthenticationResponse]

  /**
   * Authenticates a user with email and password credentials.
   *
   * @param request Contains email and password for authentication
   * @return User information with authentication token on success
   */
  def login(request: AuthenticateRequest): Result[AuthenticationResponse]

  /**
   * Retrieves the current authenticated user's information.
   *
   * @param request Contains the current user's authentication context
   * @return Current user's private information
   */
  def getCurrent(request: GetUserRequest): Result[AuthenticationResponse]

  /**
   * Registers a new user account in the system.
   *
   * @param request Contains new user registration data (username, email, password)
   * @return New user information with authentication token
   */
  def register(request: RegistrationRequest): Result[AuthenticationResponse]

  /**
   * Dispatches a user request to the appropriate handler method.
   *
   * This method provides a unified entry point for processing any user-related request,
   * routing it to the specific handler based on the request type.
   *
   * @param request The user request to process
   * @return Response corresponding to the request type
   */
  def run(request: UserRequest): Result[UserResponse] = request match
    case r: FollowUserRequest   => follow(r)
    case r: GetProfileRequest   => getProfile(r)
    case r: UpdateUserRequest   => update(r)
    case r: AuthenticateRequest => login(r)
    case r: GetUserRequest      => getCurrent(r)
    case r: RegistrationRequest => register(r)
    case r: UnfollowUserRequest => unfollow(r)
}
