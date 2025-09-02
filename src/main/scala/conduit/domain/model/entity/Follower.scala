package conduit.domain.model.entity

import conduit.domain.model.types.article.AuthorId
import conduit.domain.model.types.user.*

case class Follower(
  by: UserId,
  author: AuthorId,
)
