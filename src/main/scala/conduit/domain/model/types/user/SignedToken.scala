package conduit.domain.model.types.user

import zio.prelude.Subtype

type SignedToken = SignedToken.Type
object SignedToken extends Subtype[String]:

  def fromString(value: String, prefix: String): SignedToken =
    if value.startsWith(prefix)
    then SignedToken(value.substring(prefix.length).trim)
    else SignedToken(value.trim)
