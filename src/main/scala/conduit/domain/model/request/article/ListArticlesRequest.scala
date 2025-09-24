package conduit.domain.model.request.article

import conduit.domain.logic.persistence.ArticleRepository
import conduit.domain.model.entity.User
import conduit.domain.model.request.article.ListArticlesRequest.Filter

case class ListArticlesRequest(
  requester: User,
  offset: Int,
  limit: Int,
  filters: List[Filter],
)

object ListArticlesRequest:
  enum Filter:
    case Tag(name: String)
    case Author(username: String)
    case FavoriteOf(username: String)
