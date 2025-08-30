package conduit.domain.model.types.article

import zio.prelude.Subtype

type ArticleSlugIndex = ArticleSlugIndex.Type
object ArticleSlugIndex extends Subtype[Int]
