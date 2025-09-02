package conduit.domain.model.patching

import conduit.domain.model.entity.Article
import conduit.domain.model.types.article.{ ArticleBody, ArticleDescription, ArticleTitle }
import zio.prelude.Validation

/**
 * A patch to be applied to an article.
 * Each patch represents a change to a single field.
 * Patches can be combined to form a complete update to an article.
 */
enum ArticlePatch:
  case Title(value: ArticleTitle)
  case Description(value: ArticleDescription)
  case Body(value: ArticleBody)

  def apply(data: Article.Data): Article.Data =
    this match
      case ArticlePatch.Body(value)        => data.copy(body = value)
      case ArticlePatch.Title(value)       => data.copy(title = value)
      case ArticlePatch.Description(value) => data.copy(description = value)

object ArticlePatch:
  def apply(value: Article.Data, patches: List[ArticlePatch]): Article.Data =
    patches.foldLeft(value)((d, p) => p.apply(d))

  def apply(article: Article, patches: List[ArticlePatch]): Article =
    article.copy(data = apply(article.data, patches))

  def title(value: String): Validation[ArticleTitle.Error, ArticlePatch.Title] =
    ArticleTitle.fromString(value).map(ArticlePatch.Title.apply)

  def description(value: String): Validation[ArticleDescription.Error, ArticlePatch.Description] =
    ArticleDescription.fromString(value).map(ArticlePatch.Description.apply)

  def body(value: String): Validation[ArticleBody.Error, ArticlePatch.Body] =
    ArticleBody.fromString(value).map(ArticlePatch.Body.apply)
