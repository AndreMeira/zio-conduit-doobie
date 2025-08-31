package conduit.domain.model.request.article

import conduit.domain.model.entity.User
import conduit.domain.model.types.article.ArticleSlug

class RemoveFavoriteArticleRequest(val requester: User, val article: ArticleSlug)
