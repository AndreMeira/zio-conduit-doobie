package conduit.domain.model.error

import conduit.domain.model.types.article.{ ArticleId, ArticleSlug, AuthorId }
import conduit.domain.model.types.user.UserId
import conduit.domain.model.types.comment.CommentId

/**
 * Enumeration of system state inconsistency errors.
 *
 * These errors indicate that the system has reached an invalid state where
 * data relationships that should exist are missing. This typically indicates
 * bugs in the application logic or data corruption.
 */
enum InconsistentState extends ApplicationError.IllegalStateError:
  /** A slug exists but has no corresponding article */
  case SlugWithNoArticle(slug: ArticleSlug)

  /** A user exists but has no associated profile */
  case UserWithNoProfile(user: UserId)

  /** A user exists but has no associated authentication credentials */
  case NoCredentialsForUser(user: UserId)

  /** A user exists but has no associated email address */
  case NoEmailForUser(user: UserId)

  override def message: String = this match {
    case UserWithNoProfile(user)    => s"User with id $user has no profile"
    case SlugWithNoArticle(slug)    => s"Permalink with slug $slug has no associated article"
    case NoCredentialsForUser(user) => s"User with id $user has no associated credentials"
    case NoEmailForUser(user)       => s"User with id $user has no associated email"
  }

object InconsistentState:
  /** Creates an error for a user missing an email address */
  def noEmail(user: UserId): InconsistentState        = InconsistentState.NoEmailForUser(user)

  /** Creates an error for a user missing credentials */
  def noCredentials(user: UserId): InconsistentState  = InconsistentState.NoCredentialsForUser(user)

  /** Creates an error for a user missing a profile */
  def noProfile(user: UserId): InconsistentState      = InconsistentState.UserWithNoProfile(user)

  /** Creates an error for a slug missing its article */
  def noArticle(slug: ArticleSlug): InconsistentState = InconsistentState.SlugWithNoArticle(slug)

  /** Creates an error for an author missing a profile */
  def noAuthor(author: AuthorId): InconsistentState   = InconsistentState.UserWithNoProfile(author)
