package conduit.domain.model.request.article

import conduit.domain.model.entity.User
import conduit.domain.model.types.article.ArticleSlug

case class AddFavoriteArticleRequest(requester: User, article: ArticleSlug)
