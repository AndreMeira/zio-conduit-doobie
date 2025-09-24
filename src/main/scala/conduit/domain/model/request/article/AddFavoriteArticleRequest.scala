package conduit.domain.model.request.article

import conduit.domain.model.entity.User

case class AddFavoriteArticleRequest(
  requester: User.Authenticated,
  slug: String,
)
