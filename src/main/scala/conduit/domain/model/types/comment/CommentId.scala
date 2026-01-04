package conduit.domain.model.types.comment

import zio.prelude.Subtype

type CommentId = CommentId.Type
object CommentId extends Subtype[Long]
