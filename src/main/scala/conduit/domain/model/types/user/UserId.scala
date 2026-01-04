package conduit.domain.model.types.user

import zio.prelude.Subtype

type UserId = UserId.Type
object UserId extends Subtype[Long]
