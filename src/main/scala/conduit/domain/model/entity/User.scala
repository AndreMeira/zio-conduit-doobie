package conduit.domain.model.entity

import conduit.domain.model.types.user.UserId

enum User:
  case Anonymous
  case Authenticated(userId: UserId)

  def option: Option[User.Authenticated] = this match
    case User.Anonymous         => None
    case User.Authenticated(id) => Some(Authenticated(id))
