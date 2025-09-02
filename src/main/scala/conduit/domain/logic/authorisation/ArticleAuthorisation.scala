package conduit.domain.logic.authorisation

import conduit.domain.model.entity.User
import conduit.domain.model.error.ApplicationError.{ TransientError, UnauthorisedError }
import conduit.domain.model.request.ArticleRequest
import zio.ZIO

trait ArticleAuthorisation[Tx] extends Authorisation[Tx, ArticleRequest, ArticleAuthorisation.Failure]

object ArticleAuthorisation:
  enum Failure extends UnauthorisedError:
    case CanNotUpdateArticle(reason: String)
    case CanNotDeleteArticle(reason: String)
    case CanNotFavoriteArticle(reason: String)
    case CanNotUnfavoriteArticle(reason: String)
    case CanNotCreateArticle(reason: String)
    case CanNotViewArticle(reason: String)
    case CanNotViewArticles(reason: String)

    override def message: String = this match
      case CanNotUpdateArticle(reason)     => s"Can not update article: $reason"
      case CanNotDeleteArticle(reason)     => s"Can not delete article: $reason"
      case CanNotFavoriteArticle(reason)   => s"Can not favorite article: $reason"
      case CanNotUnfavoriteArticle(reason) => s"Can not unfavorite article: $reason"
      case CanNotCreateArticle(reason)     => s"Can not create article: $reason"
      case CanNotViewArticle(reason)       => s"Can not view article: $reason"
      case CanNotViewArticles(reason)      => s"Can not view articles: $reason"
