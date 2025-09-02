package conduit.domain.service.validation

import conduit.domain.logic.monitoring.Monitor
import conduit.domain.logic.persistence.ArticleRepository
import conduit.domain.logic.validation.ArticleValidator
import conduit.domain.model.entity.Article
import conduit.domain.model.error.ApplicationError.ValidationError
import conduit.domain.model.patching.ArticlePatch
import conduit.domain.model.request.article.ListArticlesRequest.Filter
import conduit.domain.model.request.article.*
import conduit.domain.model.types.article.*
import conduit.domain.model.types.user.UserId
import conduit.domain.service.validation.ArticleValidationService.Invalid
import zio.ZIO
import zio.prelude.Validation

import scala.util.chaining.scalaUtilChainingOps

class ArticleValidationService[Tx](
  monitor: Monitor,
  articleRepository: ArticleRepository[Tx],
) extends ArticleValidator[Tx] {

  override def validate(request: UpdateArticleRequest): Result[List[ArticlePatch]] =
    monitor.track("ArticleValidationService.validateUpdate") {
      ZIO.succeed {
        List(
          request.payload.article.body.option.map(ArticlePatch.body),
          request.payload.article.title.option.map(ArticlePatch.title),
          request.payload.article.description.option.map(ArticlePatch.description),
        ).flatten match {
          case Nil     => Validation.succeed(Nil)
          case patches => Validation.validateAll(patches)
        }
      }
    }

  override def validate(request: CreateArticleRequest): Result[ArticleWithTags] =
    monitor.track("ArticleValidationService.validateCreate") {
      ZIO.succeed {
        Validation
          .validate(
            validateArticleData(request),
            validateArticleTags(request.payload.article.tagList.getOrElse(Nil)),
          )
          .map((article, tags) => (article = article, tags = tags))
      }
    }

  override def validate(request: GetArticleRequest): Result[ArticleSlug] =
    monitor.track("ArticleValidationService.validateGet") {
      ZIO.succeed {
        Validation.succeed(request.article)
      }
    }

  override def validate(request: DeleteArticleRequest): Result[ArticleSlug] =
    monitor.track("ArticleValidationService.validateDelete") {
      ZIO.succeed {
        Validation.succeed(request.article)
      }
    }

  override def validate(request: ListArticlesRequest): Result[SearchQuery] =
    monitor.track("ArticleValidationService.validateList") {
      ZIO.succeed {
        request
          .filters
          .map:
            case Filter.Tag(name)            => ArticleRepository.Search.Tag(name)
            case Filter.Author(username)     => ArticleRepository.Search.Author(username)
            case Filter.FavoriteOf(username) => ArticleRepository.Search.FavoriteOf(username)
          .pipe: filters =>
            Validation.succeed((filters = filters, limit = request.limit, offset = request.offset))
      }
    }

  override def validate(request: ArticleFeedRequest): Result[FeedQuery] =
    monitor.track("ArticleValidationService.validateFeed") {
      ZIO.succeed {
        val userId = request.requester.userId
        Validation.succeed((userId = userId, limit = request.limit, offset = request.offset))
      }
    }

  override def validate(request: AddFavoriteArticleRequest): Result[ArticleSlug] =
    monitor.track("ArticleValidationService.validateAddFavorite") {
      ZIO.succeed {
        Validation.succeed(request.article)
      }
    }

  override def validate(request: RemoveFavoriteArticleRequest): Result[ArticleSlug] =
    monitor.track("ArticleValidationService.validateRemoveFavorite") {
      ZIO.succeed {
        Validation.succeed(request.article)
      }
    }

  override def validate(request: ListTagsRequest): Result[Unit] =
    monitor.track("ArticleValidationService.validateListTags") {
      ZIO.succeed {
        Validation.succeed(())
      }
    }

  private def validateArticleData(request: CreateArticleRequest): Validation[ValidationError, Article.Data] =
    Validation
      .validate(
        request.payload.article.title.pipe(ArticleSlug.fromString),
        request.payload.article.title.pipe(ArticleTitle.fromString),
        request.payload.article.description.pipe(ArticleDescription.fromString),
        Validation.succeed(AuthorId(request.requester.userId)),
        request.payload.article.body.pipe(ArticleBody.fromString),
      )
      .map(Article.Data.apply)

  private def validateArticleTags(tags: List[String]): Validation[ValidationError, List[ArticleTag]] =
    Validation.validateAll(tags.map(ArticleTag.fromString))
}

object ArticleValidationService:
  enum Invalid extends ValidationError:
    case AuthorIsAnonymous
    case ArticleNotFound(slug: String)

    override def key: String = "article validation"

    override def message: String = this match
      case AuthorIsAnonymous     => "Author cannot be anonymous"
      case ArticleNotFound(slug) => s"Article with slug '${slug}' not found"
