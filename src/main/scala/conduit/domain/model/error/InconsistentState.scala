package conduit.domain.model.error

import conduit.domain.model.types.article.{ ArticleId, ArticleSlug, AuthorId }
import conduit.domain.model.types.user.UserId
import conduit.domain.model.types.comment.CommentId

enum InconsistentState extends ApplicationError.IllegalStateError:
  case ArticleWithNoAuthor(article: ArticleSlug)
  case CommentWithNoArticle(comment: CommentId, article: ArticleId)
  case UserWithNoProfile(user: UserId)

  override def message: String = this match {
    case CommentWithNoArticle(comment, article) =>
      s"Comment with id $comment is associated with non-existing article with id $article"

    case ArticleWithNoAuthor(article) =>
      s"Article with slug $article has no associated author"

    case UserWithNoProfile(user) =>
      s"User with id $user has no profile"
  }

object InconsistentState:
  def noProfile(user: UserId): InconsistentState =
    InconsistentState.UserWithNoProfile(user)

  def noArticle(comment: CommentId, article: ArticleId): InconsistentState =
    InconsistentState.CommentWithNoArticle(comment, article)

  def noAuthor(article: ArticleSlug): InconsistentState =
    InconsistentState.ArticleWithNoAuthor(article)
