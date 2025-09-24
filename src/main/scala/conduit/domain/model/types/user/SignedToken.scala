package conduit.domain.model.types.user

import zio.prelude.Subtype

type SignedToken = SignedToken.Type
object SignedToken extends Subtype[String]:

  def bearer(value: String): SignedToken =
    fromString(value, "Bearer")

  def fromString(value: String, prefix: String): SignedToken =
    SignedToken(value.replaceFirst(prefix, "").trim)
