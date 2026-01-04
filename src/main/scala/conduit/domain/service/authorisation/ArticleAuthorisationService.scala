package conduit.domain.service.authorisation

import conduit.domain.logic.authorisation.ArticleAuthorisation
import conduit.domain.logic.authorisation.ArticleAuthorisation.Failure
import conduit.domain.logic.authorisation.definition.Authorisation
import conduit.domain.logic.monitoring.Monitor
import conduit.domain.logic.persistence.{ ArticleRepository, PermalinkRepository }
import conduit.domain.model.entity.User
import conduit.domain.model.request.ArticleRequest
import conduit.domain.model.request.article.*
import conduit.domain.model.types.article.{ ArticleSlug, AuthorId }
import izumi.reflect.Tag as ReflectionTag
import zio.{ ZIO, ZLayer }

class ArticleAuthorisationService[Tx](
  monitor: Monitor,
  val permalinks: PermalinkRepository[Tx],
) extends ArticleAuthorisation[Tx] {

  override type Error = permalinks.Error // can only fail with same errors as the injected repository

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
    belongsToUserOrDeny(ArticleSlug(request.slug), request.requester):
      Failure.CanNotUpdateArticle(_)

  private def canDeleteArticle(request: DeleteArticleRequest): Result =
    belongsToUserOrDeny(ArticleSlug(request.slug), request.requester):
      Failure.CanNotDeleteArticle(_)

  private def belongsToUserOrDeny(slug: ArticleSlug, user: User.Authenticated)(error: String => Failure): Result =
    val reason = s"Article $slug does not belong to user ${user.userId}"
    permalinks.exists(slug, AuthorId(user.userId)).map {
      case true  => Authorisation.Result.Allowed
      case false => Authorisation.Result.Denied(error(reason))
    }
}

object ArticleAuthorisationService:
  def layer[Tx: ReflectionTag]: ZLayer[PermalinkRepository[Tx] & Monitor, Nothing, ArticleAuthorisationService[Tx]] =
    ZLayer {
      for {
        monitor    <- ZIO.service[Monitor]
        permalinks <- ZIO.service[PermalinkRepository[Tx]]
      } yield ArticleAuthorisationService(monitor, permalinks)
    }
