package conduit.domain.service.authorisation

import conduit.domain.logic.authorisation.Authorisation
import conduit.domain.model.entity.{Article, Comment, Requester, User}
import zio.ZIO

class AuthorisationService extends Authorisation[Any] {
  type Result = ZIO[Any, Authorisation.Failure, Unit]

  override def authoriseUpdate(user: User, requester: Requester): Result =
    authenticated(requester).map { auth =>
      if auth.userId == user.id then ()
      else Authorisation.Failure.CanNotUpdateUser(user.id)
    }

  override def authoriseUpdate(article: Article, requester: Requester): Result =
    authenticated(requester).map { auth =>
      if auth.userId == article.data.info.author then ()
      else Authorisation.Failure.CanNotUpdateArticle(article.id)
    }

  override def authoriseDelete(article: Article, requester: Requester): Result =
    authenticated(requester).map { auth =>
      if auth.userId == article.data.info.author then ()
      else Authorisation.Failure.CanNotDeleteArticle(article.id)
    }

  override def authoriseDelete(comment: Comment, requester: Requester): Result =
    authenticated(requester).map { auth =>
      if auth.userId == comment.data.author then ()
      else Authorisation.Failure.CanNotDeleteComment(comment.id)
    }

  override def authenticated(requester: Requester): ZIO[Any, Authorisation.Failure, Requester.Authenticated] =
    requester match
      case auth: Requester.Authenticated => ZIO.succeed(auth)
      case Requester.Anonymous           => ZIO.fail(Authorisation.Failure.UserNotAuthenticated)
}
