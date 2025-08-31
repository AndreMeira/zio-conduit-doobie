package conduit.domain.model.request.article

import ListArticlesRequest.Filter
import conduit.domain.model.entity.User

case class ListArticlesRequest(
    requester: User,
    offset: Int,
    limit: Int,
    filters: List[Filter],
  )

object ListArticlesRequest:
  enum Filter:
    case Author(username: String)
    case FavoriteOf(username: String)
    case Tag(name: String)
