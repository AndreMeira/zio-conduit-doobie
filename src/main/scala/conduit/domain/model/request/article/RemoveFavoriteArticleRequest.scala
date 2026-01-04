package conduit.domain.model.request.article

import conduit.domain.model.entity.User
import conduit.domain.model.types.article.ArticleSlug

case class RemoveFavoriteArticleRequest(
  requester: User.Authenticated,
  slug: String,
)
