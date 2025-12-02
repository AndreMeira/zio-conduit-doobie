package conduit.domain.model.patching

import conduit.domain.model.entity.Article
import conduit.domain.model.types.article.{ ArticleBody, ArticleDescription, ArticleSlug, ArticleTitle }
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
  case Slug(value: ArticleSlug)

  def apply(data: Article.Data): Article.Data =
    this match
      case ArticlePatch.Body(value)        => data.copy(body = value)
      case ArticlePatch.Title(value)       => data.copy(title = value)
      case ArticlePatch.Description(value) => data.copy(description = value)
      case ArticlePatch.Slug(value)        => data.copy(slug = value)

object ArticlePatch:
  def apply(articleData: Article.Data, patches: List[ArticlePatch]): Article.Data =
    patches.foldLeft(articleData)((articleData, patch) => patch.apply(articleData))

  def apply(article: Article, patches: List[ArticlePatch]): Article =
    article.copy(data = ArticlePatch.apply(article.data, patches))

  def title(value: String): Validation[ArticleTitle.Error, ArticlePatch.Title] =
    ArticleTitle.fromString(value).map(ArticlePatch.Title.apply)

  def description(value: String): Validation[ArticleDescription.Error, ArticlePatch.Description] =
    ArticleDescription.fromString(value).map(ArticlePatch.Description.apply)

  def body(value: String): Validation[ArticleBody.Error, ArticlePatch.Body] =
    ArticleBody.fromString(value).map(ArticlePatch.Body.apply)

  def slug(value: String): Validation[ArticleSlug.Error, ArticlePatch.Slug] =
    ArticleSlug.fromString(value).map(ArticlePatch.Slug.apply)
