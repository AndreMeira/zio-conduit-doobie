package conduit.domain.model.entity

import conduit.domain.model.types.article.{ ArticleId, ArticleTag }

/**
 * Represents the association between an article and a tag.
 *
 * Tags provide a way to categorize and organize articles, enabling
 * users to filter content by topics of interest. This entity models
 * the many-to-many relationship between articles and tags.
 *
 * @param article The article being tagged
 * @param tag     The tag applied to the article
 */
case class Tag(
  article: ArticleId,
  tag: ArticleTag,
)
