package conduit.domain.model.request.article

import conduit.domain.model.entity.User

case class ArticleFeedRequest(
    requester: User,
    offset: Int,
    limit: Int,
  )
