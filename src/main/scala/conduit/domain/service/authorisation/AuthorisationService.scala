package conduit.domain.service.authorisation

import conduit.domain.logic.authorisation.Authorisation
import conduit.domain.model.entity.{ Article, Comment, User, UserProfile }
import zio.ZIO

class AuthorisationService extends Authorisation[Any] {

  override def authoriseUpdate(user: UserProfile, requester: User): ZIO[Any, Authorisation.Failure, Unit] =
    authenticated(requester).map { auth =>
      if auth.userId == user.id then ()
      else Authorisation.Failure.CanNotUpdateUser(user.id)
    }

  override def authoriseUpdate(article: Article, requester: User): ZIO[Any, Authorisation.Failure, Unit] =
    authenticated(requester).map { auth =>
      if auth.userId == article.data.author then ()
      else Authorisation.Failure.CanNotUpdateArticle(article.id)
    }

  override def authoriseDelete(article: Article, requester: User): ZIO[Any, Authorisation.Failure, Unit] =
    authenticated(requester).map { auth =>
      if auth.userId == article.data.author then ()
      else Authorisation.Failure.CanNotDeleteArticle(article.id)
    }

  override def authoriseDelete(comment: Comment, requester: User): ZIO[Any, Authorisation.Failure, Unit] =
    authenticated(requester).map { auth =>
      if auth.userId == comment.data.author then ()
      else Authorisation.Failure.CanNotDeleteComment(comment.id)
    }

  override def authenticated(requester: User): ZIO[Any, Authorisation.Failure, User.Authenticated] =
    requester match
      case auth: User.Authenticated => ZIO.succeed(auth)
      case User.Anonymous           => ZIO.fail(Authorisation.Failure.UserNotAuthenticated)
}
