package conduit.domain.model.entity

import conduit.domain.model.types.article.ArticleId
import conduit.domain.model.types.user.UserId

/**
 * Represents a user's favorite article relationship.
 *
 * This entity tracks which articles a user has marked as favorites,
 * enabling features like favorite counts and personal favorite lists.
 * It models the many-to-many relationship between users and articles.
 *
 * @param by      The user who favorited the article
 * @param article The article that was favorited
 */
case class FavoriteArticle(
  by: UserId,
  article: ArticleId,
)
