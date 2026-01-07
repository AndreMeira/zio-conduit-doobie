package conduit.domain.model.entity

import conduit.domain.model.types.article.*

import java.time.Instant

/**
 * Represents a blog article in the Conduit system.
 *
 * An article is the primary content entity, containing the main data (title, body, etc.),
 * along with metadata for tracking creation and modification times.
 *
 * @param id       Unique identifier for the article
 * @param data     Core article content and information
 * @param metadata Timestamps for creation and last update
 */
case class Article(
  id: ArticleId,
  data: Article.Data,
  metadata: Article.Metadata,
)

object Article:
  /**
   * Metadata associated with an article, containing audit information.
   *
   * @param createdAt Timestamp when the article was first created
   * @param updatedAt Timestamp when the article was last modified
   */
  case class Metadata(
    createdAt: Instant,
    updatedAt: Instant,
  )

  /**
   * Core article data containing the main content and essential information.
   *
   * @param slug        URL-friendly identifier derived from the title
   * @param title       The article's display title
   * @param description Brief summary or preview of the article content
   * @param author      ID of the user who authored this article
   * @param body        Full content/body text of the article
   */
  case class Data(
    slug: ArticleSlug,
    title: ArticleTitle,
    description: ArticleDescription,
    author: AuthorId,
    body: ArticleBody,
  )

  /**
   * Compact version of article data for overview listings.
   *
   * This excludes the body content to reduce payload size when listing
   * multiple articles, such as in feeds or search results.
   *
   * @param slug        URL-friendly identifier derived from the title
   * @param title       The article's display title
   * @param description Brief summary or preview of the article content
   * @param author      ID of the user who authored this article
   */
  case class Info(
    slug: ArticleSlug,
    title: ArticleTitle,
    description: ArticleDescription,
    author: AuthorId,
  )

  /**
   * Fully denormalized article with all related information included.
   *
   * This representation includes the complete article data plus all related
   * entities (author profile, tags, favorite count) for efficient display
   * without additional queries.
   *
   * @param id            Unique identifier for the article
   * @param data          Core article content and information
   * @param author        Complete author profile information
   * @param tags          List of tags associated with the article
   * @param favoriteCount Number of users who have favorited this article
   * @param metadata      Timestamps for creation and last update
   */
  case class Expanded(
    id: ArticleId,
    data: Article.Data,
    author: UserProfile,
    tags: List[ArticleTag],
    favoriteCount: ArticleFavoriteCount,
    metadata: Article.Metadata,
  )

  /**
   * Denormalized article overview without the full body content.
   *
   * Similar to Expanded but uses Info instead of Data to exclude the article body,
   * making it suitable for article listings and feeds where full content isn't needed.
   *
   * @param id            Unique identifier for the article
   * @param info          Essential article information (without body)
   * @param author        Complete author profile information
   * @param tags          List of tags associated with the article
   * @param favoriteCount Number of users who have favorited this article
   * @param metadata      Timestamps for creation and last update
   */
  case class Overview(
    id: ArticleId,
    info: Article.Info,
    author: UserProfile,
    tags: List[ArticleTag],
    favoriteCount: ArticleFavoriteCount,
    metadata: Article.Metadata,
  )
