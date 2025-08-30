package conduit.domain.model.request.comment

import conduit.domain.model.entity.Requester
import conduit.domain.model.types.article.ArticleSlug

case class ListCommentsRequest(requester: Requester, article: ArticleSlug)
