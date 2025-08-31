package conduit.domain.logic.authorisation

import conduit.domain.logic.authorisation
import conduit.domain.model.entity.{ Article, Comment, User, UserProfile }
import conduit.domain.model.error.ApplicationError
import conduit.domain.model.types.article.ArticleId
import conduit.domain.model.types.comment.CommentId
import conduit.domain.model.types.user.UserId
import zio.ZIO

trait Authorisation[Tx] {
  def authoriseUpdate(user: UserProfile, requester: User): ZIO[Tx, Authorisation.Error, Unit]
  def authoriseUpdate(article: Article, requester: User): ZIO[Tx, Authorisation.Error, Unit]
  def authoriseDelete(article: Article, requester: User): ZIO[Tx, Authorisation.Error, Unit]
  def authoriseDelete(comment: Comment, requester: User): ZIO[Tx, Authorisation.Error, Unit]
  def authenticated(requester: User): ZIO[Tx, Authorisation.Error, User.Authenticated]
}

object Authorisation:
  type Error = Failure | ApplicationError.TransientError

  enum Failure extends ApplicationError.UnauthorisedError:
    case CanNotUpdateArticle(articleId: ArticleId)
    case CanNotDeleteArticle(articleId: ArticleId)
    case CanNotDeleteComment(commentId: CommentId)
    case CanNotUpdateUser(userId: UserId)
    case UserNotAuthenticated

    def message: String = this match
      case UserNotAuthenticated           => "User is not authenticated"
      case CanNotUpdateUser(userId)       => s"User can not update user $userId"
      case CanNotUpdateArticle(articleId) => s"User can not update article $articleId"
      case CanNotDeleteArticle(articleId) => s"User can not delete article $articleId"
      case CanNotDeleteComment(commentId) => s"User can not delete comment $commentId"
