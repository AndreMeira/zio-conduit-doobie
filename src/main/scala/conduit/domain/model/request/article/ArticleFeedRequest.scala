package conduit.domain.model.request.article

import conduit.domain.model.entity.Requester

case class ArticleFeedRequest(requester: Requester, offset: Int, limit: Int)
