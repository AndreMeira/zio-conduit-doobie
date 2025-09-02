package conduit.domain.service.authorisation

import conduit.domain.logic.authorisation.ArticleAuthorisation.Failure
import conduit.domain.logic.authorisation.{ ArticleAuthorisation, Authorisation }
import conduit.domain.logic.monitoring.Monitor
import conduit.domain.logic.persistence.ArticleRepository
import conduit.domain.model.entity.User
import conduit.domain.model.request.ArticleRequest
import conduit.domain.model.request.article.*
import conduit.domain.model.types.article.{ ArticleSlug, AuthorId }
import zio.ZIO

class ArticleAuthorisationService[Tx](
  monitor: Monitor,
  articles: ArticleRepository[Tx],
) extends ArticleAuthorisation[Tx] {

  override def authorise(request: ArticleRequest): Result =
    monitor.track("ArticleAuthorisationService.authorise") {
      request match
        case request: UpdateArticleRequest   => canUpdateArticle(request)
        case request: DeleteArticleRequest   => canDeleteArticle(request)
        case _: AddFavoriteArticleRequest    => allowed // Any authenticated user can favorite an article
        case _: RemoveFavoriteArticleRequest => allowed // Any authenticated user can unfavorite an article
        case _: CreateArticleRequest         => allowed // Any authenticated user can create an article
        case _: GetArticleRequest            => allowed // Any user (authenticated or not) can view an article
        case _: ListArticlesRequest          => allowed // Any user (authenticated or not) can view articles
        case _: ListTagsRequest              => allowed // Any user (authenticated or not) can view tags
        case _: ArticleFeedRequest           => allowed // Any authenticated user can view their feed
    }

  private def canUpdateArticle(request: UpdateArticleRequest): Result =
    articleBelongsToUser(request.article, request.requester): reason =>
      Failure.CanNotUpdateArticle(reason)

  private def canDeleteArticle(request: DeleteArticleRequest): Result =
    articleBelongsToUser(request.article, request.requester): reason =>
      Failure.CanNotDeleteArticle(reason)

  private def articleBelongsToUser(slug: ArticleSlug, user: User.Authenticated)(error: String => Failure): Result =
    val reason = s"Article $slug does not belong to user ${user.userId}"
    articles.exists(slug, AuthorId(user.userId)).map {
      case true  => Authorisation.Result.Allowed
      case false => Authorisation.Result.NotAllowed(error(reason))
    }
}
