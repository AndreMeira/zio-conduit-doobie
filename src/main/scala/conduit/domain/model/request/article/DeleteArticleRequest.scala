package conduit.domain.model.request.article

import conduit.domain.model.entity.User

case class DeleteArticleRequest(
  requester: User.Authenticated,
  slug: String,
)
