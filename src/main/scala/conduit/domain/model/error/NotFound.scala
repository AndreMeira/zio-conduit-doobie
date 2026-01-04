package conduit.domain.model.error

import conduit.domain.model.types.article.{ ArticleId, ArticleSlug, AuthorId }
import conduit.domain.model.types.comment.CommentId
import conduit.domain.model.types.user.{ UserId, UserName }

enum NotFound extends ApplicationError.NotFoundError:
  case ArticleNotFound(slug: String)
  case CommentNotFound(commentId: Long)
  case UserIdNotFound(userId: Long)
  case ProfileNotFound(username: String)
  case AuthorOfArticleNotFound(articleId: Long)

  override def message: String = this match
    case ArticleNotFound(slug)              => s"Article with slug '$slug' not found"
    case CommentNotFound(commentId)         => s"Comment with id '$commentId' not found"
    case UserIdNotFound(userId)             => s"User with id '$userId' not found"
    case ProfileNotFound(username)          => s"Profile for user with username '$username' not found"
    case AuthorOfArticleNotFound(articleId) => s"Author for article with id '$articleId' not found"

object NotFound:
  def article(slug: ArticleSlug): NotFound     = NotFound.ArticleNotFound(slug)
  def comment(commentId: CommentId): NotFound  = NotFound.CommentNotFound(commentId)
  def user(userId: UserId): NotFound           = NotFound.UserIdNotFound(userId)
  def profile(username: UserName): NotFound    = NotFound.ProfileNotFound(username)
  def authorOf(articleId: ArticleId): NotFound = NotFound.AuthorOfArticleNotFound(articleId)
