package conduit.domain.model.request.article

import conduit.domain.model.entity.Requester
import conduit.domain.model.types.article.ArticleSlug

class RemoveFavoriteArticleRequest(val requester: Requester, val article: ArticleSlug)
