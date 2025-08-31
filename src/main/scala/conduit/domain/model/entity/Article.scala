package conduit.domain.model.entity

import conduit.domain.model.types.article.*
import zio.prelude.Validation

import java.time.Instant

case class Article(
  id: ArticleId, 
  data: Article.Data, 
  metadata: Article.Metadata
)

object Article:
  // Inner entities
  case class Metadata(
    createdAt: Instant, 
    updatedAt: Instant
  )
  
  case class Data(
    slug: ArticleSlug, 
    title: ArticleTitle, 
    description: ArticleDescription, 
    author: AuthorId, 
    body: ArticleBody
  )

  // Shrinked version of Article for overviews, leaving out body
  case class Info(
    slug: ArticleSlug, 
    title: ArticleTitle, 
    description: ArticleDescription, 
    author: AuthorId
  )

  // Denormalized entities
  case class Expanded(
    id: ArticleId, 
    data: Article.Data, 
    author: UserProfile, 
    favoriteCount: ArticleFavoriteCount
  )
  
  case class Overview(
    id: ArticleId, 
    info: Article.Info, 
    author: UserProfile, 
    favoriteCount: ArticleFavoriteCount
  )

  enum Patch:
    case Title(value: ArticleTitle)
    case Description(value: ArticleDescription)
    case Body(value: ArticleBody)

  object Patch:
    def title(value: String): Validation[ArticleTitle.Error, Patch.Title] =
      ArticleTitle.fromString(value).map(Patch.Title.apply)

    def description(value: String): Validation[ArticleDescription.Error, Patch.Description] =
      ArticleDescription.fromString(value).map(Patch.Description.apply)

    def body(value: String): Validation[ArticleBody.Error, Patch.Body] =
      ArticleBody.fromString(value).map(Patch.Body.apply)

  def patch(article: Article.Data, patches: List[Patch]): Article.Data =
    patches.foldLeft(article) { (data, patch) =>
      patch match
        case Patch.Title(value)       => data.copy(title = value)
        case Patch.Description(value) => data.copy(description = value)
        case Patch.Body(value)        => data.copy(body = value)
    }
