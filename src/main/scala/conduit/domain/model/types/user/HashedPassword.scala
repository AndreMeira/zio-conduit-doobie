package conduit.domain.model.types.user

import zio.prelude.Subtype

type HashedPassword = HashedPassword.Type
object HashedPassword extends Subtype[String]
