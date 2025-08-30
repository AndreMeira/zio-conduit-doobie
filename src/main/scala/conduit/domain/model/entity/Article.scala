package conduit.domain.model.entity

import conduit.domain.model.types.article.*
import zio.prelude.Validation

import java.time.Instant

case class Article(id: ArticleId, data: Article.Data, metadata: Article.Metadata)

object Article:
  case class Data(info: Info, body: ArticleBody)
  case class Metadata(createdAt: Instant, updatedAt: Instant)
  case class Info(slug: ArticleSlug, title: ArticleTitle, description: ArticleDescription, author: AuthorId)
  case class Overview(id: ArticleId, info: Info, metadata: Metadata)

  enum Patch:
    case Title(value: ArticleTitle)
    case Description(value: ArticleDescription)
    case Body(value: ArticleBody)

  object Patch:
    def title(value: String): Validation[ArticleTitle.Error, Patch.Title] =
      ArticleTitle.fromString(value).map(Patch.Title.apply)

    def description(value: String): Validation[ArticleDescription.Error, ArticleDescription] =
      ArticleDescription.fromString(value)

    def body(value: String): Validation[ArticleBody.Error, Patch.Body] =
      ArticleBody.fromString(value).map(Patch.Body.apply)

  def patch(article: Article.Data, patches: List[Patch]): Article.Data =
    patches.foldLeft(article) { (data, patch) =>
      patch match
        case Patch.Title(value)       => data.copy(info = data.info.copy(title = value))
        case Patch.Description(value) => data.copy(info = data.info.copy(description = value))
        case Patch.Body(value)        => data.copy(body = value)
    }