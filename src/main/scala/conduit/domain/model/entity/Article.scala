package conduit.domain.model.entity

import conduit.domain.model.types.article.*

import java.time.Instant

case class Article(
  id: ArticleId,
  data: Article.Data,
  metadata: Article.Metadata,
)

object Article:
  // Inner entities
  case class Metadata(
    createdAt: Instant,
    updatedAt: Instant,
  )

  // Core article data
  case class Data(
    slug: ArticleSlug,
    title: ArticleTitle,
    description: ArticleDescription,
    author: AuthorId,
    body: ArticleBody,
  )

  // Shrinked version of Article for overviews, leaving out body
  case class Info(
    slug: ArticleSlug,
    title: ArticleTitle,
    description: ArticleDescription,
    author: AuthorId,
  )

  // Denormalized entities
  case class Expanded(
    id: ArticleId,
    data: Article.Data,
    author: UserProfile,
    tags: List[ArticleTag],
    favoriteCount: ArticleFavoriteCount,
    metadata: Article.Metadata,
  )

  case class Overview(
    id: ArticleId,
    info: Article.Info,
    author: UserProfile,
    tags: List[ArticleTag],
    favoriteCount: ArticleFavoriteCount,
    metadata: Article.Metadata,
  )
