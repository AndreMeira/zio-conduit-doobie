package conduit.domain.service.authorisation

import conduit.domain.logic.authorisation.UserAuthorisation
import conduit.domain.logic.monitoring.Monitor
import conduit.domain.logic.persistence.{ UserProfileRepository, UserRepository }
import conduit.domain.model.entity.User
import conduit.domain.model.request.UserRequest
import conduit.domain.model.request.user.*
import zio.{ ZIO, ZLayer }
import izumi.reflect.Tag as ReflectionTag

class UserAuthorisationService[Tx](monitor: Monitor) extends UserAuthorisation[Tx] {

  override type Error = Nothing // This service does not produce any errors

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

object UserAuthorisationService:
  def layer[Tx: ReflectionTag]: ZLayer[Monitor, Nothing, UserAuthorisation[Tx]] = ZLayer {
    for monitor <- ZIO.service[Monitor]
    yield UserAuthorisationService(monitor)
  }
