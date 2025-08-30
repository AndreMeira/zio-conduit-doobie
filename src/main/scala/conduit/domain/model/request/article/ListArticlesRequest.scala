package conduit.domain.model.request.article

import ListArticlesRequest.Filter
import conduit.domain.model.entity.Requester

case class ListArticlesRequest(requester: Requester, offset: Int, limit: Int, filters: List[Filter])

object ListArticlesRequest:
  enum Filter:
    case Author(username: String)
    case FavoriteOf(username: String)
    case Tag(name: String)

