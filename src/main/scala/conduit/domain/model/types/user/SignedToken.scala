package conduit.domain.model.types.user

import zio.prelude.Subtype

type SignedToken = SignedToken.Type
object SignedToken extends Subtype[String]
