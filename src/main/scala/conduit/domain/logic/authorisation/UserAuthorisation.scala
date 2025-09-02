package conduit.domain.logic.authorisation

import conduit.domain.model.entity.User
import conduit.domain.model.error.ApplicationError.{ TransientError, UnauthorisedError }
import conduit.domain.model.request.UserRequest
import zio.ZIO

trait UserAuthorisation[Tx] extends Authorisation[Tx, UserRequest, UserAuthorisation.Failure]

object UserAuthorisation:
  enum Failure extends UnauthorisedError:
    case CanNotViewProfile(reason: String)
    case CanNotFollowUser(reason: String)
    case CanNotUnfollowUser(reason: String)
    case CanNotUpdateUser(reason: String)
    case CanNotRegisterUser(reason: String)
    case CanNotLoginUser(reason: String)

    override def message: String = this match
      case CanNotViewProfile(reason)  => s"Can not view profile: $reason"
      case CanNotFollowUser(reason)   => s"Can not follow user: $reason"
      case CanNotUnfollowUser(reason) => s"Can not unfollow user: $reason"
      case CanNotUpdateUser(reason)   => s"Can not update user: $reason"
      case CanNotRegisterUser(reason) => s"Can not register user: $reason"
      case CanNotLoginUser(reason)    => s"Can not login user: $reason"
