package conduit.domain.model.request.user

import conduit.domain.model.entity.User
import conduit.domain.model.types.user.UserName

case class FollowUserRequest(requester: User, username: UserName)
