package conduit.domain.model.request.article

import conduit.domain.model.entity.Requester
import conduit.domain.model.types.article.ArticleSlug

case class GetArticleRequest(requester: Requester, article: ArticleSlug)
