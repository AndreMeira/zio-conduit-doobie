package conduit.domain.model.request.article

import conduit.domain.model.entity.User

case class ArticleFeedRequest(
  requester: User.Authenticated,
  offset: Int,
  limit: Int,
)
