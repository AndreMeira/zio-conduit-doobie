package conduit.domain.service.entrypoint

import conduit.domain.logic.authorisation.ArticleAuthorisation
import conduit.domain.logic.entrypoint.ArticleEntrypoint
import conduit.domain.logic.monitoring.Monitor
import conduit.domain.logic.persistence.*
import conduit.domain.logic.validation.ArticleValidator
import conduit.domain.model.entity.{ Article, FavoriteArticle, Follower, Tag }
import conduit.domain.model.error.{ InconsistentState, NotFound }
import conduit.domain.model.patching.ArticlePatch
import conduit.domain.model.request.article.*
import conduit.domain.model.response.article.*
import conduit.domain.model.types.article.ArticleSlug
import conduit.domain.service.entrypoint.dsl.EntrypointDsl
import zio.ZIO
import zio.ZLayer
import izumi.reflect.Tag as ReflectionTag

class ArticleEntrypointService[Tx](
  monitor: Monitor,
  unitOfWork: UnitOfWork[Tx],
  authorisation: ArticleAuthorisation[Tx],
  validation: ArticleValidator[Tx],
  tags: TagRepository[Tx],
  slugs: ArticleSlugRepository[Tx],
  articles: ArticleRepository[Tx],
  followers: FollowerRepository[Tx],
  permalinks: PermalinkRepository[Tx],
  favorites: FavoriteArticleRepository[Tx],
) extends ArticleEntrypoint, EntrypointDsl(unitOfWork, authorisation) {

  override def get(request: GetArticleRequest): Result[GetArticleResponse] =
    monitor.track("ArticleEndpoint.get") {
      authorise(request):
        for {
          slug      <- validation.parse(request).validOrFail
          articleId <- permalinks.resolve(slug) ?! NotFound.article(slug)
          article   <- articles.find(articleId) ?! InconsistentState.noArticle(slug)
          follower   = request.requester.option.map(_.userId).map(Follower(_, article.data.author))
          following <- ZIO.foreach(follower)(followers.exists).someOrElse(false)
          favorite   = request.requester.option.map(_.userId).map(FavoriteArticle(_, article.id))
          favorited <- ZIO.foreach(favorite)(favorites.exists).someOrElse(false)
        } yield GetArticleResponse.make(article, favorited, following)
    }

  override def feed(request: ArticleFeedRequest): Result[ArticleListResponse] =
    monitor.track("ArticleEndpoint.feed") {
      authorise(request):
        for {
          query     <- validation.parse(request).validOrFail
          count     <- articles.countFeedOf(query.userId)
          articles  <- articles.feedOf(query.userId, query.offset, query.limit)
          favorites <- favorites.list(query.userId, articles.map(_.id))
          followed  <- followers.list(query.userId, articles.map(_.info.author))
        } yield ArticleListResponse.make(count, articles, favorites.toSet, followed.toSet)
    }

  override def create(request: CreateArticleRequest): Result[GetArticleResponse] =
    monitor.track("ArticleEndpoint.create") {
      authorise(request):
        for {
          (article, tag) <- validation.parse(request).validOrFail
          slug           <- slugs.nextAvailable(article.slug)
          created        <- articles.save(article.copy(slug = slug))
                              ?! InconsistentState.noProfile(article.author) // only possibility since we provide everything else
          _              <- permalinks.save(created.id, slug)
          tags           <- tags.save(tag.map(Tag(created.id, _)))
          result          = created.copy(tags = tags.map(_.tag))
        } yield GetArticleResponse.make(result, favorited = false, following = false)
    }

  override def update(request: UpdateArticleRequest): Result[GetArticleResponse] =
    monitor.track("ArticleEndpoint.update") {
      authorise(request):
        for {
          updates <- validation.parse(request).validOrFail
          article <- articles.find(updates.id) ?! InconsistentState.noArticle(updates.slug)
          patched  = ArticlePatch.apply(article.data, updates.patches)
          linked  <- permalinks.exists(updates.id, patched.slug)

          // If the slug is not yet linked to this article, find the next available slug
          nextSlug <- if linked then ZIO.succeed(patched.slug)
                      else slugs.nextAvailable(patched.slug)
          _        <- ZIO.when(!linked)(permalinks.save(article.id, nextSlug))

          updated <- articles.save(updates.id, patched.copy(slug = nextSlug))
                       ?! InconsistentState.noArticle(updates.slug)
        } yield GetArticleResponse.make(updated, false, false)
    }

  override def recent(request: ListArticlesRequest): Result[ArticleListResponse] =
    monitor.track("ArticleEndpoint.recent") {
      authorise(request):
        for {
          query     <- validation.parse(request).validOrFail
          count     <- articles.countSearch(query.filters)
          articles  <- articles.search(query.filters, query.offset, query.limit)
          requester  = request.requester.option.map(_.userId)
          favorites <- ZIO.foreach(requester)(favorites.list(_, articles.map(_.id)))
          followed  <- ZIO.foreach(requester)(followers.list(_, articles.map(_.info.author)))
        } yield {
          val favoritesSet = favorites.getOrElse(Nil).toSet
          val followedSet  = followed.getOrElse(Nil).toSet
          ArticleListResponse.make(count, articles, favoritesSet, followedSet)
        }
    }

  override def delete(request: DeleteArticleRequest): Result[DeleteArticleResponse] =
    monitor.track("ArticleEndpoint.delete") {
      authorise(request):
        for {
          slug      <- validation.parse(request).validOrFail
          articleId <- permalinks.resolve(slug) ?! NotFound.article(slug)
          article   <- articles.delete(articleId) ?! InconsistentState.noArticle(slug)
          _         <- permalinks.delete(article.id)
          _         <- favorites.deleteByArticle(article.id)
          _         <- tags.deleteByArticle(article.id)
        } yield DeleteArticleResponse(slug)
    }

  override def addFavorite(request: AddFavoriteArticleRequest): Result[GetArticleResponse] =
    monitor.track("ArticleEndpoint.addFavorite") {
      authorise(request):
        for {
          slug      <- validation.parse(request).validOrFail
          articleId <- permalinks.resolve(slug) ?! NotFound.article(slug)
          _         <- favorites.add(FavoriteArticle(request.requester.userId, articleId))
          article   <- articles.find(articleId) ?! InconsistentState.noArticle(slug)
          following <- followers.exists(Follower(request.requester.userId, article.data.author))
        } yield GetArticleResponse.make(article, favorited = true, following)
    }

  override def removeFavorite(request: RemoveFavoriteArticleRequest): Result[GetArticleResponse] =
    monitor.track("ArticleEndpoint.removeFavorite") {
      authorise(request):
        for {
          slug      <- validation.parse(request).validOrFail
          articleId <- permalinks.resolve(slug) ?! NotFound.article(slug)
          article   <- articles.find(articleId) ?! InconsistentState.noArticle(slug)
          _         <- favorites.delete(FavoriteArticle(request.requester.userId, article.id))
          following <- followers.exists(Follower(request.requester.userId, article.data.author))
        } yield GetArticleResponse.make(article, favorited = false, following)
    }

  override def tags(request: ListTagsRequest): Result[TagListResponse] =
    monitor.track("ArticleEndpoint.tags") {
      authorise(request):
        for {
          _    <- validation.parse(request).validOrFail
          tags <- tags.distinct
        } yield TagListResponse(tags)
    }
}

object ArticleEntrypointService:
  type Dependency[Tx] =
    UnitOfWork[Tx] & ArticleAuthorisation[Tx] & ArticleValidator[Tx] & TagRepository[Tx] & ArticleSlugRepository[Tx] & ArticleRepository[Tx] &
      FollowerRepository[Tx] & PermalinkRepository[Tx] & FavoriteArticleRepository[Tx] & Monitor

  def layer[Tx: ReflectionTag]: ZLayer[Dependency[Tx], Nothing, ArticleEntrypointService[Tx]] =
    ZLayer {
      for {
        monitor       <- ZIO.service[Monitor]
        unitOfWork    <- ZIO.service[UnitOfWork[Tx]]
        authorisation <- ZIO.service[ArticleAuthorisation[Tx]]
        validation    <- ZIO.service[ArticleValidator[Tx]]
        tags          <- ZIO.service[TagRepository[Tx]]
        slugs         <- ZIO.service[ArticleSlugRepository[Tx]]
        articles      <- ZIO.service[ArticleRepository[Tx]]
        followers     <- ZIO.service[FollowerRepository[Tx]]
        permalinks    <- ZIO.service[PermalinkRepository[Tx]]
        favorites     <- ZIO.service[FavoriteArticleRepository[Tx]]
      } yield ArticleEntrypointService(
        monitor,
        unitOfWork,
        authorisation,
        validation,
        tags,
        slugs,
        articles,
        followers,
        permalinks,
        favorites,
      )
    }
