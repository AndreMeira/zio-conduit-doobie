package conduit.domain.model.entity

import conduit.domain.model.types.user.UserId

enum Requester:
  case Anonymous
  case Authenticated(userId: UserId)

  def isAuthenticated: Boolean = this match
    case Anonymous        => false
    case Authenticated(_) => true

  def requesterId: Option[UserId] = this match
    case Anonymous         => None
    case Authenticated(id) => Some(id)