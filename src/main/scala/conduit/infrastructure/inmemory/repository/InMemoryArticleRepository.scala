package conduit.infrastructure.inmemory.repository

import conduit.domain.logic.monitoring.Monitor
import conduit.domain.logic.persistence.ArticleRepository
import conduit.domain.logic.persistence.ArticleRepository.Search
import conduit.domain.model.entity.{ Article, UserProfile }
import conduit.domain.model.types.article.*
import conduit.domain.model.types.user.{ UserId, UserName }
import zio.{ Clock, ZIO, ZLayer }

import java.time.Instant

class InMemoryArticleRepository(monitor: Monitor) extends ArticleRepository[Transaction] {

  override type Error = Nothing

  override def titleExists(title: ArticleTitle, authorId: AuthorId): Result[Boolean] =
    monitor.track("InMemoryArticleRepository.titleExists", "resource" -> "db") {
      Transaction.execute: state =>
        for {
          articles   <- state.articles.get
          titleAuthor = articles.view.values.map(article => article.data.title -> article.data.author)
        } yield titleAuthor.iterator.contains(title -> authorId)
    }

  override def find(id: ArticleId): Result[Option[Article.Expanded]] =
    monitor.track("InMemoryArticleRepository.find", "resource" -> "db") {
      Transaction.execute: state =>
        for {
          tags      <- state.tags.get
          articles  <- state.articles.get
          profiles  <- state.profiles.get
          favorites <- state.favorites.get

          article     = articles.get(id)
          profile     = article.flatMap(article => profiles.get(article.data.author))
          articleTags = article.fold(List.empty)(article => tags.getOrElse(article.id, List.empty))
          favorite    = article.fold(Set.empty)(article => favorites.getOrElse(article.id, Set.empty))
        } yield for {
          content <- article
          author  <- profile
          count    = ArticleFavoriteCount(favorite.size)
        } yield Article.Expanded(content.id, content.data, author, articleTags, count, content.metadata)
    }

  override def feedOf(userId: UserId, offset: Int, limit: Int): Result[List[Article.Overview]] =
    monitor.track("InMemoryArticleRepository.feedOf", "resource" -> "db") {
      Transaction.execute: state =>
        for {
          tags      <- state.tags.get
          articles  <- state.articles.get
          profiles  <- state.profiles.get
          followers <- state.followers.get
          favorites <- state.favorites.get

          followed = followers.getOrElse(userId, Set.empty)
          filtered = articles.values.filter(article => followed.contains(article.data.author))
          sorted   = filtered.toList.sortBy(_.metadata.createdAt)(using Ordering[Instant].reverse)
          paged    = sorted.slice(offset, offset + limit)
        } yield buildOverviewList(paged, profiles, tags, favorites)
    }

  override def search(filters: List[ArticleRepository.Search], offset: Int, limit: Int): Result[List[Article.Overview]] =
    monitor.track("InMemoryArticleRepository.search", "resource" -> "db") {
      Transaction.execute: state =>
        for {
          tags      <- state.tags.get
          profiles  <- state.profiles.get
          favorites <- state.favorites.get
          articles  <- state.articles.get
          search    <- parseFilters(filters)

          found  = articles.values.filter(search)
          sorted = found.toList.sortBy(_.metadata.createdAt)(using Ordering[Instant].reverse)
          paged  = sorted.slice(offset, offset + limit)
        } yield buildOverviewList(paged, profiles, tags, favorites)
    }

  override def countFeedOf(userId: UserId): Result[Int] =
    monitor.track("InMemoryArticleRepository.countFeedOf", "resource" -> "db") {
      Transaction.execute: state =>
        for {
          articles  <- state.articles.get
          followers <- state.followers.get

          followed = followers.getOrElse(userId, Set.empty)
          filtered = articles.values.filter(article => followed.contains(article.data.author))
        } yield filtered.size
    }

  override def countSearch(filters: List[ArticleRepository.Search]): Result[Int] =
    monitor.track("InMemoryArticleRepository.countSearch", "resource" -> "db") {
      Transaction.execute: state =>
        for {
          articles <- state.articles.get
          search   <- parseFilters(filters)
          found     = articles.values.filter(search)
        } yield found.toSet.size
    }

  override def save(article: Article.Data): Result[Option[Article.Expanded]] =
    monitor.track("InMemoryArticleRepository.save", "resource" -> "db") {
      Transaction.execute: state =>
        for {
          now   <- Clock.instant
          id    <- state.nextId.map(ArticleId(_))
          entity = Article(id, article, Article.Metadata(now, now))
          found <- state.articles.update(_ + (id -> entity)) *> find(id)
        } yield found
    }

  override def save(articleId: ArticleId, article: Article.Data): Result[Option[Article.Expanded]] =
    monitor.track("InMemoryArticleRepository.update", "resource" -> "db") {
      Transaction.execute: state =>
        for {
          now      <- Clock.instant
          articles <- state.articles.get
          existing  = articles.get(articleId)
          newMeta   = existing.map(_.metadata.copy(updatedAt = now))
          updated   = existing.zip(newMeta).map((_, meta) => Article(articleId, article, meta))
          _        <- state.articles.update(_ ++ updated.map(article => articleId -> article))
          found    <- find(articleId)
        } yield found
    }

  override def delete(articleId: ArticleId): Result[Option[Article]] =
    monitor.track("InMemoryArticleRepository.delete", "resource" -> "db") {
      Transaction.execute: state =>
        for {
          articles <- state.articles.get
          _        <- state.articles.update(_ - articleId)
        } yield articles.get(articleId)
    }

  private def tagFilter(tag: ArticleTag): Result[Article => Boolean] =
    Transaction.execute { state =>
      for {
        tags      <- state.tags.get
        articleIds = tags.filter((_, tags) => tags.contains(tag)).keys.toSet
      } yield article => articleIds.contains(article.id)
    }

  private def authorFilter(name: UserName): Result[Article => Boolean] =
    Transaction.execute { state =>
      for {
        profiles <- state.profiles.get
        userId    = profiles.find((_, profile) => profile.data.name == name).map((id, _) => id)
      } yield article => userId.contains(article.data.author)
    }

  private def favoriteOfFilter(name: UserName): Result[Article => Boolean] =
    Transaction.execute { state =>
      for {
        profiles  <- state.profiles.get
        favorites <- state.favorites.get
        userId     = profiles.find((_, profile) => profile.data.name == name).map((id, _) => id)
        favorited  = userId.map(uid => favorites.filter((_, users) => users.contains(uid)).keys.toSet)
      } yield article => favorited.getOrElse(Set.empty).contains(article.id)
    }

  private def parseFilters(filters: List[ArticleRepository.Search]): Result[Article => Boolean] = {
    val tag        = filters.collectFirst { case Search.Tag(tag) => ArticleTag(tag) }
    val author     = filters.collectFirst { case Search.Author(name) => UserName(name) }
    val favoriteOf = filters.collectFirst { case Search.FavoriteOf(name) => UserName(name) }

    for {
      tagFilter        <- ZIO.foreach(tag)(tagFilter).someOrElse((a: Article) => true)
      authorFilter     <- ZIO.foreach(author)(authorFilter).someOrElse((a: Article) => true)
      favoriteOfFilter <- ZIO.foreach(favoriteOf)(favoriteOfFilter).someOrElse((a: Article) => true)
    } yield article => tagFilter(article) && authorFilter(article) && favoriteOfFilter(article)
  }

  private def buildOverviewList(
    articles: List[Article],
    profiles: Map[UserId, UserProfile],
    tags: Map[ArticleId, List[ArticleTag]],
    favorites: Map[ArticleId, Set[UserId]],
  ): List[Article.Overview] = for {
    article <- articles
    author  <- profiles.get(article.data.author)

    content     = article.data
    articleTags = tags.getOrElse(article.id, List.empty)
    favorite    = favorites.getOrElse(article.id, Set.empty)
    count       = ArticleFavoriteCount(favorite.size)
    info        = Article.Info(content.slug, content.title, content.description, content.author)
  } yield Article.Overview(article.id, info, author, articleTags, count, article.metadata)
}

object InMemoryArticleRepository {
  val layer: ZLayer[Monitor, Nothing, ArticleRepository[Transaction]] =
    ZLayer {
      for monitor <- ZIO.service[Monitor]
      yield InMemoryArticleRepository(monitor)
    }
}
