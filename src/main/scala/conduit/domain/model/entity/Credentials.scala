package conduit.domain.model.entity

import conduit.domain.model.types.user.{ Email, HashedPassword, Password }

enum Credentials:
  case Clear(email: Email, password: Password)
  case Hashed(email: Email, password: HashedPassword)

  def getEmail: Email = this match
    case Clear(email, _)  => email
    case Hashed(email, _) => email

  def getPassword: String = this match
    case Clear(_, password)  => password
    case Hashed(_, password) => password
