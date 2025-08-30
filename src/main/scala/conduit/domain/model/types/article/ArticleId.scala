package conduit.domain.model.types.article

import zio.prelude.Subtype

type ArticleId = ArticleId.Type
object ArticleId extends Subtype[Long]
