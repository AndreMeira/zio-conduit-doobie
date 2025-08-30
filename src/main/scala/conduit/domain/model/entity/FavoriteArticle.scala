package conduit.domain.model.entity

import conduit.domain.model.types.article.ArticleId
import conduit.domain.model.types.user.UserId

case class FavoriteArticle(by: UserId, article: ArticleId)
