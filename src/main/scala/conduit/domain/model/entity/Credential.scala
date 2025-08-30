package conduit.domain.model.entity

import conduit.domain.model.types.user.{Email, HashedPassword, Password}

case class Credential(email: Email, password: HashedPassword)

object Credential:
  case class Clear(email: Email, password: Password)
