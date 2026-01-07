package conduit.domain.model.error

import conduit.domain.model.types.article.{ ArticleId, ArticleSlug, AuthorId }
import conduit.domain.model.types.comment.CommentId
import conduit.domain.model.types.user.{ UserId, UserName }

/**
 * Enumeration of specific "not found" errors in the Conduit system.
 *
 * These errors occur when operations reference entities that don't exist
 * in the system. Each case provides specific context about what was missing.
 */
enum NotFound extends ApplicationError.NotFoundError:
  /** Article with the specified slug could not be found */
  case ArticleNotFound(slug: String)

  /** Comment with the specified ID could not be found */
  case CommentNotFound(commentId: Long)

  /** User with the specified ID could not be found */
  case UserIdNotFound(userId: Long)

  /** User profile with the specified username could not be found */
  case ProfileNotFound(username: String)

  /** Author of the specified article could not be found */
  case AuthorOfArticleNotFound(articleId: Long)

  override def message: String = this match
    case ArticleNotFound(slug)              => s"Article with slug '$slug' not found"
    case CommentNotFound(commentId)         => s"Comment with id '$commentId' not found"
    case UserIdNotFound(userId)             => s"User with id '$userId' not found"
    case ProfileNotFound(username)          => s"Profile for user with username '$username' not found"
    case AuthorOfArticleNotFound(articleId) => s"Author for article with id '$articleId' not found"

object NotFound:
  /** Creates a NotFound error for a missing article */
  def article(slug: ArticleSlug): NotFound     = NotFound.ArticleNotFound(slug)

  /** Creates a NotFound error for a missing comment */
  def comment(commentId: CommentId): NotFound  = NotFound.CommentNotFound(commentId)

  /** Creates a NotFound error for a missing user */
  def user(userId: UserId): NotFound           = NotFound.UserIdNotFound(userId)

  /** Creates a NotFound error for a missing user profile */
  def profile(username: UserName): NotFound    = NotFound.ProfileNotFound(username)

  /** Creates a NotFound error for a missing article author */
  def authorOf(articleId: ArticleId): NotFound = NotFound.AuthorOfArticleNotFound(articleId)
