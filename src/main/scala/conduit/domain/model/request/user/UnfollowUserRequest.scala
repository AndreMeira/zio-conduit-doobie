package conduit.domain.model.request.user

import conduit.domain.model.entity.Requester
import conduit.domain.model.types.user.UserName

case class UnfollowUserRequest(requester: Requester, username: UserName)
