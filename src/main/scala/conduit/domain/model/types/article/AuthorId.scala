package conduit.domain.model.types.article

import conduit.domain.model.types.user.UserId
import zio.prelude.Subtype

type AuthorId = AuthorId.Type
object AuthorId extends Subtype[UserId]
