package conduit.domain.service.authorisation

import conduit.domain.logic.authorisation.UserAuthorisation
import conduit.domain.logic.monitoring.Monitor
import conduit.domain.logic.persistence.{ UserProfileRepository, UserRepository }
import conduit.domain.model.entity.User
import conduit.domain.model.request.UserRequest
import conduit.domain.model.request.user.*

class UserAuthorisationService[Tx](
  monitor: Monitor,
  users: UserRepository[Tx],
  profiles: UserProfileRepository[Tx],
) extends UserAuthorisation[Tx] {

  override def authorise(request: UserRequest): Result =
    monitor.track("UserAuthorisationService.authorise") {
      request match
        case _: UpdateUserRequest   => allowed // Any authenticated user can update their own details
        case _: GetProfileRequest   => allowed // Any user (authenticated or not) can view profiles
        case _: FollowUserRequest   => allowed // Any authenticated user can follow users
        case _: UnfollowUserRequest => allowed // Any authenticated user can unfollow users
        case _: RegistrationRequest => allowed // Any anonymous user can register
        case _: AuthenticateRequest => allowed // Any anonymous user can login
        case _: GetUserRequest      => allowed // Any authenticated user can view their own details
    }
}
