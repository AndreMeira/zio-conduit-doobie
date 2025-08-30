package conduit.domain.model.types.user

import zio.prelude.Subtype

type HashedToken = HashedToken.Type
object HashedToken extends Subtype[String]
