package conduit.domain.model.entity

import conduit.domain.model.types.article.{ArticleId, ArticleTag}

case class Tag(article: ArticleId, tag: ArticleTag)
