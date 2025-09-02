package conduit.domain.service.endpoint

import conduit.domain.logic.authorisation.{ ArticleAuthorisation, Authorisation }
import conduit.domain.logic.endpoint.ArticleEndpoint
import conduit.domain.logic.monitoring.Monitor
import conduit.domain.logic.persistence.*
import conduit.domain.logic.validation.ArticleValidator
import conduit.domain.model.entity.{ FavoriteArticle, Follower, Tag }
import conduit.domain.model.error.NotFound
import conduit.domain.model.patching.ArticlePatch
import conduit.domain.model.request.article.*
import conduit.domain.model.response.article.*
import conduit.domain.service.endpoint.dsl.EndpointDsl
import zio.ZIO

class ArticleEndpointService[Tx](
  monitor: Monitor,
  unitOfWork: UnitOfWork[Tx],
  authorisation: ArticleAuthorisation[Tx],
  validation: ArticleValidator[Tx],
  articles: ArticleRepository[Tx],
  favorites: FavoriteArticleRepository[Tx],
  followers: FollowerRepository[Tx],
  tags: TagRepository[Tx],
) extends ArticleEndpoint[Tx], EndpointDsl {

  override def get(request: GetArticleRequest): Result[GetArticleResponse] =
    monitor.track("ArticleEndpoint.get") {
      unitOfWork.execute:
        for {
          _         <- authorisation.authorise(request).allowedOrFail
          slug      <- validation.validate(request).validOrFail
          article   <- articles.findExpanded(slug).someOrFail(NotFound.article(slug))
          follower   = request.requester.option.map(_.userId).map(Follower(_, article.data.author))
          following <- ZIO.foreach(follower)(followers.exists).map(_.getOrElse(false))
          favorite   = request.requester.option.map(_.userId).map(FavoriteArticle(_, article.id))
          favorited <- ZIO.foreach(favorite)(favorites.exists).map(_.getOrElse(false))
        } yield GetArticleResponse.make(article, favorited, following)
    }

  override def feed(request: ArticleFeedRequest): Result[ArticleListResponse] =
    monitor.track("ArticleEndpoint.feed") {
      unitOfWork.execute:
        for {
          _         <- authorisation.authorise(request).allowedOrFail
          query     <- validation.validate(request).validOrFail
          count     <- articles.countFeedOf(query.userId)
          articles  <- articles.feedOf(query.userId, query.offset, query.limit)
          favorites <- favorites.list(query.userId, articles.map(_.id))
          followed  <- followers.list(query.userId, articles.map(_.info.author))
        } yield ArticleListResponse.make(count, articles, favorites.toSet, followed.toSet)
    }

  override def create(request: CreateArticleRequest): Result[GetArticleResponse] =
    monitor.track("ArticleEndpoint.create") {
      unitOfWork.execute:
        for {
          _       <- authorisation.authorise(request).allowedOrFail
          data    <- validation.validate(request).validOrFail
          article <- articles.save(data.article)
          _       <- tags.save(data.tags.map(Tag(article.id, _)))
        } yield GetArticleResponse.make(article, favorited = false, following = false)
    }

  override def update(request: UpdateArticleRequest): Result[GetArticleResponse] =
    monitor.track("ArticleEndpoint.update") {
      unitOfWork.execute:
        for {
          _       <- authorisation.authorise(request).allowedOrFail
          patches <- validation.validate(request).validOrFail
          slug     = request.article
          article <- articles.findBySlug(slug).someOrFail(NotFound.article(slug))
          patched  = ArticlePatch.apply(article, patches)
          updated <- articles.save(patched)
        } yield GetArticleResponse.make(updated, false, false)
    }

  override def recent(request: ListArticlesRequest): Result[ArticleListResponse] =
    monitor.track("ArticleEndpoint.recent") {
      unitOfWork.execute:
        for {
          _         <- authorisation.authorise(request).allowedOrFail
          query     <- validation.validate(request).validOrFail
          count     <- articles.countSearch(query.filters)
          articles  <- articles.recent(query.filters, query.offset, query.limit)
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
      unitOfWork.execute:
        for {
          _       <- authorisation.authorise(request).allowedOrFail
          slug    <- validation.validate(request).validOrFail
          article <- articles.delete(request.article).someOrFail(NotFound.article(slug))
          _       <- favorites.deleteByArticle(article.id)
          _       <- tags.deleteByArticle(article.id)
        } yield DeleteArticleResponse(slug)
    }

  override def addFavorite(request: AddFavoriteArticleRequest): Result[GetArticleResponse] =
    monitor.track("ArticleEndpoint.addFavorite") {
      unitOfWork.execute:
        for {
          _         <- authorisation.authorise(request).allowedOrFail
          slug      <- validation.validate(request).validOrFail
          article   <- articles.findExpanded(slug).someOrFail(NotFound.article(slug))
          _         <- favorites.add(FavoriteArticle(request.requester.userId, article.id))
          following <- followers.exists(Follower(request.requester.userId, article.data.author))
        } yield GetArticleResponse.make(article, favorited = true, following)
    }

  override def removeFavorite(request: RemoveFavoriteArticleRequest): Result[GetArticleResponse] =
    monitor.track("ArticleEndpoint.removeFavorite") {
      unitOfWork.execute:
        for {
          _         <- authorisation.authorise(request).allowedOrFail
          slug      <- validation.validate(request).validOrFail
          article   <- articles.findExpanded(slug).someOrFail(NotFound.article(slug))
          _         <- favorites.delete(FavoriteArticle(request.requester.userId, article.id))
          following <- followers.exists(Follower(request.requester.userId, article.data.author))
        } yield GetArticleResponse.make(article, favorited = false, following)
    }

  override def tags(request: ListTagsRequest): Result[TagListResponse] =
    monitor.track("ArticleEndpoint.tags") {
      unitOfWork.execute:
        for {
          _    <- authorisation.authorise(request).allowedOrFail
          _    <- validation.validate(request).validOrFail
          tags <- tags.listAll
        } yield TagListResponse(tags)
    }
}
