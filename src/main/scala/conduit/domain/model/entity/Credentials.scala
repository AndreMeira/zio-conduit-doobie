package conduit.domain.model.entity

import conduit.domain.model.types.user.{ Email, HashedPassword, Password }

enum Credentials:
  case Clear(email: Email, password: Password)
  case Hashed(email: Email, password: HashedPassword)
