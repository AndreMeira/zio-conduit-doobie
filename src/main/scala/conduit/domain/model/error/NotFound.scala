package conduit.domain.model.error

import conduit.domain.model.types.article.{ ArticleId, ArticleSlug, AuthorId }
import conduit.domain.model.types.comment.CommentId
import conduit.domain.model.types.user.{ UserId, UserName }

enum NotFound extends ApplicationError.NotFoundError:
  case ArticleNotFound(slug: String)
  case ArticleIdNotFound(articleId: Long)
  case CommentNotFound(commentId: Long)
  case UserNotFound(username: String)
  case UserIdNotFound(userId: Long)
  case ProfileNotFound(username: String)
  case ProfileUserIdNotFound(userId: Long)
  case AuthorNotFound(authorId: Long)
  case AuthorOfArticleNotFound(articleId: Long)
  case AuthorOfArticleSlugNotFound(articleSlug: String)

  override def key: String = this match
    case ArticleNotFound(_)             => "article"
    case ArticleIdNotFound(_)           => "article"
    case CommentNotFound(_)             => "comment"
    case UserNotFound(_)                => "user"
    case UserIdNotFound(_)              => "user"
    case ProfileNotFound(_)             => "profile"
    case ProfileUserIdNotFound(_)       => "profile"
    case AuthorNotFound(_)              => "author"
    case AuthorOfArticleNotFound(_)     => "author"
    case AuthorOfArticleSlugNotFound(_) => "author"

  override def message: String = this match
    case ArticleNotFound(slug)                            => s"Article with slug '$slug' not found"
    case ArticleIdNotFound(articleId)                     => s"Article with id '$articleId' not found"
    case CommentNotFound(commentId)                       => s"Comment with id '$commentId' not found"
    case UserNotFound(username)                           => s"User with username '$username' not found"
    case UserIdNotFound(userId)                           => s"User with id '$userId' not found"
    case ProfileNotFound(username)                        => s"Profile for user with username '$username' not found"
    case ProfileUserIdNotFound(userId)                    => s"Profile for user with id '$userId' not found"
    case AuthorNotFound(authorId)                         => s"Author with id '$authorId' not found"
    case AuthorOfArticleNotFound(articleId)               => s"Author for article with id '$articleId' not found"
    case AuthorOfArticleSlugNotFound(articleSlug: String) => s"Author for article with slug '$articleSlug' not found"

object NotFound:
  def article(slug: ArticleSlug): NotFound         = NotFound.ArticleNotFound(slug)
  def article(articleId: ArticleId): NotFound      = NotFound.ArticleIdNotFound(articleId)
  def comment(commentId: CommentId): NotFound      = NotFound.CommentNotFound(commentId)
  def user(username: UserName): NotFound           = NotFound.UserNotFound(username)
  def user(userId: UserId): NotFound               = NotFound.UserIdNotFound(userId)
  def profile(username: UserName): NotFound        = NotFound.ProfileNotFound(username)
  def profile(userId: UserId): NotFound            = NotFound.ProfileUserIdNotFound(userId)
  def author(authorId: AuthorId): NotFound         = NotFound.AuthorNotFound(authorId)
  def authorOf(articleId: ArticleId): NotFound     = NotFound.AuthorOfArticleNotFound(articleId)
  def authorOf(articleSlug: ArticleSlug): NotFound = NotFound.AuthorOfArticleSlugNotFound(articleSlug)
