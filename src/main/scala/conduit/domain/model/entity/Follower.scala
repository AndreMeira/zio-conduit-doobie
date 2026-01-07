package conduit.domain.model.entity

import conduit.domain.model.types.article.AuthorId
import conduit.domain.model.types.user.*

/**
 * Represents a follower relationship between users.
 *
 * This entity enables the social aspect of the platform, allowing users
 * to follow authors they're interested in. Followers can receive feeds
 * of articles from authors they follow.
 *
 * @param by     The user who is following someone
 * @param author The author being followed
 */
case class Follower(
  by: UserId,
  author: AuthorId,
)
