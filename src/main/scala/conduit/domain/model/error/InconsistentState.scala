package conduit.domain.model.error

import conduit.domain.model.types.article.{ ArticleId, ArticleSlug, AuthorId }
import conduit.domain.model.types.user.UserId
import conduit.domain.model.types.comment.CommentId

enum InconsistentState extends ApplicationError.IllegalStateError:
  case SlugWithNoArticle(slug: ArticleSlug)
  case UserWithNoProfile(user: UserId)
  case NoCredentialsForUser(user: UserId)
  case NoEmailForUser(user: UserId)

  override def message: String = this match {
    case UserWithNoProfile(user)    => s"User with id $user has no profile"
    case SlugWithNoArticle(slug)    => s"Permalink with slug $slug has no associated article"
    case NoCredentialsForUser(user) => s"User with id $user has no associated credentials"
    case NoEmailForUser(user)       => s"User with id $user has no associated email"
  }

object InconsistentState:
  def noEmail(user: UserId): InconsistentState        = InconsistentState.NoEmailForUser(user)
  def noCredentials(user: UserId): InconsistentState  = InconsistentState.NoCredentialsForUser(user)
  def noProfile(user: UserId): InconsistentState      = InconsistentState.UserWithNoProfile(user)
  def noArticle(slug: ArticleSlug): InconsistentState = InconsistentState.SlugWithNoArticle(slug)
  def noAuthor(author: AuthorId): InconsistentState   = InconsistentState.UserWithNoProfile(author)
