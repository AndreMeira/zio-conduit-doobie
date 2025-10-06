package conduit.domain.service.validation

import conduit.domain.logic.monitoring.Monitor
import conduit.domain.logic.persistence.ArticleRepository
import conduit.domain.logic.persistence.ArticleRepository.Search
import conduit.domain.logic.validation.ArticleValidator
import conduit.domain.model.entity.Article
import conduit.domain.model.error.ApplicationError.ValidationError
import conduit.domain.model.patching.ArticlePatch
import conduit.domain.model.request.article.*
import conduit.domain.model.request.article.ListArticlesRequest.Filter
import conduit.domain.model.types.article.*
import conduit.domain.service.validation.ArticleValidationService.Invalid
import izumi.reflect.Tag as ReflectionTag
import zio.prelude.Validation
import zio.{ ZIO, ZLayer }

import scala.util.chaining.scalaUtilChainingOps

class ArticleValidationService[Tx](
  monitor: Monitor,
  val articles: ArticleRepository[Tx],
) extends ArticleValidator[Tx] {
  override type Error = articles.Error // can only fail with the same errors as the injected repository

  override def parse(request: GetArticleRequest): Result[ArticleSlug] =
    monitor.track("ArticleValidationService.validateGet") {
      ZIO.succeed {
        Validation.succeed(ArticleSlug(request.slug))
      }
    }

  override def parse(request: DeleteArticleRequest): Result[ArticleSlug] =
    monitor.track("ArticleValidationService.validateDelete") {
      ZIO.succeed {
        Validation.succeed(ArticleSlug(request.slug))
      }
    }

  override def parse(request: ArticleFeedRequest): Result[FeedQuery] =
    monitor.track("ArticleValidationService.validateFeed") {
      ZIO.succeed {
        val userId = request.requester.userId
        Validation.succeed((userId = userId, limit = request.limit, offset = request.offset))
      }
    }

  override def parse(request: AddFavoriteArticleRequest): Result[ArticleSlug] =
    monitor.track("ArticleValidationService.validateAddFavorite") {
      ZIO.succeed {
        Validation.succeed(ArticleSlug(request.slug))
      }
    }

  override def parse(request: RemoveFavoriteArticleRequest): Result[ArticleSlug] =
    monitor.track("ArticleValidationService.validateRemoveFavorite") {
      ZIO.succeed {
        Validation.succeed(ArticleSlug(request.slug))
      }
    }

  override def parse(request: ListTagsRequest): Result[Unit] =
    monitor.track("ArticleValidationService.validateListTags") {
      ZIO.succeed {
        Validation.succeed(())
      }
    }

  override def parse(request: ListArticlesRequest): Result[SearchQuery] =
    monitor.track("ArticleValidationService.validateList") {
      ZIO.succeed {
        request
          .filters
          .map:
            case Filter.Tag(name)            => Search.Tag(name)
            case Filter.Author(username)     => Search.Author(username)
            case Filter.FavoriteOf(username) => Search.FavoriteOf(username)
          .pipe: filters =>
            Validation.succeed((filters = filters, limit = request.limit, offset = request.offset))
      }
    }

  override def parse(request: CreateArticleRequest): Result[ArticleWithTags] =
    monitor.track("ArticleValidationService.validateCreate") {
      val authorId = AuthorId(request.requester.userId)
      val title    = ArticleTitle(request.payload.article.title)

      validateArticleDoesNotExist(title, authorId).map { uniqueness =>
        val article = validateArticleData(request)
        val tags    = validateArticleTags(request.payload.article.tags)
        Validation
          .validate(uniqueness, article, tags) // combine all validations
          .map((_, article, tags) => (article = article, tags = tags))
      }
    }

  override def parse(request: UpdateArticleRequest): Result[PatchWithSlug] =
    monitor.track("ArticleValidationService.validateUpdate") {
      val authorId   = AuthorId(request.requester.userId)
      val maybeTitle = request.payload.article.title.option.map(ArticleTitle(_))

      validateArticleDoesNotExist(maybeTitle, authorId).map { uniqueness =>
        val patches = validatePatches(request)
        Validation
          .validate(uniqueness, patches) // combine all validations
          .map((_, patches) => (slug = ArticleSlug(request.slug), patches = patches))
      }
    }

  private def validateArticleData(request: CreateArticleRequest): Validation[ValidationError, Article.Data] =
    Validation
      .validate(
        request.payload.article.title.pipe(ArticleSlug.fromString), // slug depends on title
        request.payload.article.title.pipe(ArticleTitle.fromString),
        request.payload.article.description.pipe(ArticleDescription.fromString),
        Validation.succeed(AuthorId(request.requester.userId)),
        request.payload.article.body.pipe(ArticleBody.fromString),
      )
      .map(Article.Data.apply)

  private def validatePatches(request: UpdateArticleRequest): Validation[ValidationError, List[ArticlePatch]] =
    List(
      request.payload.article.body.option.map(ArticlePatch.body),
      request.payload.article.title.option.map(ArticlePatch.title),
      request.payload.article.description.option.map(ArticlePatch.description),
      request.payload.article.title.option.map(ArticlePatch.slug), // slug depends on title
    ).flatten match {
      case Nil     => Validation.succeed(Nil)
      case patches => Validation.validateAll(patches)
    }

  private def validateArticleDoesNotExist(title: Option[ArticleTitle], authorId: AuthorId): Result[Unit] =
    title match
      case None        => ZIO.succeed(Validation.succeed(())) // no title means no change, so no need to check uniqueness
      case Some(title) => validateArticleDoesNotExist(title, authorId)

  private def validateArticleDoesNotExist(title: ArticleTitle, authorId: AuthorId): Result[Unit] =
    articles
      .titleExists(title, authorId)
      .map:
        case true  => Validation.fail(Invalid.ArticleAlreadyExists(title, authorId))
        case false => Validation.succeed(())

  private def validateArticleTags(tags: List[String]): Validation[ValidationError, List[ArticleTag]] =
    Validation.validateAll(tags.map(ArticleTag.fromString))
}

object ArticleValidationService:
  enum Invalid extends ValidationError {
    case ArticleAlreadyExists(title: ArticleTitle, authorId: AuthorId)

    override def key: String = this match
      case ArticleAlreadyExists(_, _) => "article.title"

    override def message: String = this match
      case ArticleAlreadyExists(title, author) => s"Author '$author' already has an article with title '${title}'"
  }

  def layer[Tx: ReflectionTag]: ZLayer[ArticleRepository[Tx] & Monitor, Nothing, ArticleValidator[Tx]] =
    ZLayer {
      for
        monitor  <- ZIO.service[Monitor]
        articles <- ZIO.service[ArticleRepository[Tx]]
      yield ArticleValidationService(monitor, articles)
    }
