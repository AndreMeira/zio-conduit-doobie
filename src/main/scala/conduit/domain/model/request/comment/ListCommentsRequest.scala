package conduit.domain.model.request.comment

import conduit.domain.model.entity.User
import conduit.domain.model.types.article.ArticleSlug

case class ListCommentsRequest(requester: User, article: ArticleSlug)
