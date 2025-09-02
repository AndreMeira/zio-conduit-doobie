package conduit.domain.model.request.article

import conduit.domain.model.entity.User
import conduit.domain.model.types.article.ArticleSlug

case class GetArticleRequest(
  requester: User,
  article: ArticleSlug,
)
