package conduit.domain.model.request.comment

import conduit.domain.model.entity.User

case class ListCommentsRequest(requester: User, slug: String)
