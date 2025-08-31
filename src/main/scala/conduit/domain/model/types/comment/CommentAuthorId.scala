package conduit.domain.model.types.comment

import conduit.domain.model.types.user.UserId
import zio.prelude.Subtype

type CommentAuthorId = CommentAuthorId.Type
object CommentAuthorId extends Subtype[UserId]
