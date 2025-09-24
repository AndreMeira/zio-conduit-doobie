package conduit.infrastructure.postgres.repository

import conduit.domain.logic.persistence.ArticleRepository.Search
import conduit.domain.model.entity.{ Follower, Generator, Tag as EntityTag, FavoriteArticle }
import conduit.domain.model.error.{ ApplicationError, InconsistentState }
import conduit.domain.model.types.article.*
import conduit.domain.model.types.user.UserId
import conduit.infrastructure.inmemory.monitor.NoopMonitor
import conduit.infrastructure.postgres.PostgresTestAspect.{ transaction, withMigration }
import conduit.infrastructure.postgres.{ PostgresMigration, PostgresSpecLayers, PostgresUnitOfWork }
import zio.ZIO
import zio.test.{ TestAspect, ZIOSpec, assertTrue }

object PostgresArticleRepositorySpec extends ZIOSpec[PostgresUnitOfWork & PostgresMigration] {
  override val bootstrap = PostgresSpecLayers.layer
  val users              = PostgresUserRepository(new NoopMonitor)
  val profiles           = PostgresUserProfileRepository(new NoopMonitor)
  val articles           = PostgresArticleRepository(new NoopMonitor)
  val followers          = PostgresFollowerRepository(new NoopMonitor)
  val tags               = PostgresTagRepository(new NoopMonitor)
  val favorites          = PostgresFavoriteArticleRepository(new NoopMonitor)

  case object ArticleNotCreated extends ApplicationError:
    val message = "Article was not created"

  case object ArticleNotFound extends ApplicationError:
    val message = "Article was not found"

  def spec = suiteAll("PostgresArticleRepository") {

    test("Check title exists") {
      transaction:
        for {
          creds    <- Generator.credentials.runHead.map(_.get)
          profile  <- Generator.profileData.runHead.map(_.get)
          article  <- Generator.articleData.runHead.map(_.get)
          userId   <- users.save(creds)
          _        <- profiles.create(userId, profile)
          created  <- articles.save(article.copy(author = AuthorId(userId)))
          exists   <- articles.titleExists(article.title, AuthorId(userId))
          noExists <- articles.titleExists(ArticleTitle(article.title + "x"), AuthorId(userId))
        } yield assertTrue(exists && !noExists && created.isDefined)
    } @@ withMigration

    test("Find article by id") {
      transaction:
        for {
          creds    <- Generator.credentials.runHead.map(_.get)
          profile  <- Generator.profileData.runHead.map(_.get)
          article  <- Generator.articleData.runHead.map(_.get)
          userId   <- users.save(creds)
          _        <- profiles.create(userId, profile)
          created  <- articles.save(article.copy(author = AuthorId(userId)))
          found    <- articles.find(created.map(_.id).getOrElse(ArticleId(-1)))
          notFound <- articles.find(ArticleId(-1))
        } yield assertTrue(found.isDefined && found.get.id == created.get.id && notFound.isEmpty) &&
        assertTrue(created.map(_.author.data).contains(profile)) &&
        assertTrue(created.map(_.favoriteCount).contains(ArticleFavoriteCount(0)))
    } @@ withMigration

    test("build feed of user") {
      transaction:
        for {
          // Create users
          creds1   <- Generator.credentials.runHead.map(_.get)
          profile1 <- Generator.profileData.runHead.map(_.get)
          userId1  <- users.save(creds1)
          _        <- profiles.create(userId1, profile1)

          creds2   <- Generator.credentials.runHead.map(_.get)
          profile2 <- Generator.profileData.runHead.map(_.get)
          userId2  <- users.save(creds2)
          _        <- profiles.create(userId2, profile2)

          creds3   <- Generator.credentials.runHead.map(_.get)
          profile3 <- Generator.profileData.runHead.map(_.get)
          userId3  <- users.save(creds3)
          _        <- profiles.create(userId3, profile3)

          // User 1 follows User 2 and User 3
          _ <- followers.save(Follower(by = userId1, author = AuthorId(userId2)))
          _ <- followers.save(Follower(by = userId1, author = AuthorId(userId3)))

          // Create articles for User 2 and User 3
          articleData2 <- Generator.articleData.runHead.map(_.get)
          articleData3 <- Generator.articleData.runHead.map(_.get)
          created2     <- articles.save(articleData2.copy(author = AuthorId(userId2)))
          created3     <- articles.save(articleData3.copy(author = AuthorId(userId3)))

          created2 <- ZIO.fromOption(created2).mapError(_ => ArticleNotCreated)
          created3 <- ZIO.fromOption(created3).mapError(_ => ArticleNotCreated)

          // Fetch feed for User 1
          feed      <- articles.feedOf(userId1, offset = 0, limit = 10)
          feedCount <- articles.countFeedOf(userId1)

          // Fetch feed for User 2 (who follows no one)
          emptyFeed      <- articles.feedOf(UserId(userId2), offset = 0, limit = 10)
          emptyFeedCount <- articles.countFeedOf(userId2)
        } yield assertTrue(feed.length == 2) &&
        assertTrue(feedCount == 2) &&
        assertTrue(emptyFeed.isEmpty) &&
        assertTrue(emptyFeedCount == 0) &&
        assertTrue(feed.map(_.id).toSet == Set(created2.id, created3.id)) &&
        assertTrue(feed.map(_.author.data).toSet == Set(profile2, profile3))
    } @@ withMigration

    test("Find latest articles") {
      transaction:
        for {
          creds       <- Generator.credentials.runHead.map(_.get)
          profile     <- Generator.profileData.runHead.map(_.get)
          articleList <- Generator.articleData.runCollectN(20)
          distincts    = articleList.distinctBy(_.title).take(20)
          userId      <- users.save(creds)
          _           <- profiles.create(userId, profile)
          toInsert     = distincts.map(a => a.copy(author = AuthorId(userId)))
          created     <- ZIO.foreach(toInsert)(article => articles.save(article)).map(_.flatten)
          latest      <- articles.search(List.empty, offset = 0, limit = 10)
        } yield assertTrue(latest.nonEmpty) &&
        assertTrue(latest.length == 10) &&
        assertTrue(latest.map(_.id).toSet.subsetOf(created.map(_.id).toSet)) &&
        assertTrue(latest.map(_.author.data).toSet == Set(profile))
    } @@ withMigration

    test("Find articles by author") {
      transaction:
        for {
          creds       <- Generator.credentials.runHead.map(_.get)
          profile     <- Generator.profileData.runHead.map(_.get)
          articleList <- Generator.articleData.runCollectN(20)
          distincts    = articleList.distinctBy(_.title).take(20)
          userId      <- users.save(creds)
          _           <- profiles.create(userId, profile)
          created     <- ZIO.foreach(distincts)(a => articles.save(a.copy(author = AuthorId(userId)))).map(_.flatten)
          byAuthor    <- articles.search(List(Search.Author(profile.name)), offset = 0, limit = 10)
          byUnknown   <- articles.search(List(Search.Author(profile.name + "x")), offset = 0, limit = 10)
        } yield assertTrue(byAuthor.nonEmpty) &&
        assertTrue(byAuthor.length == 10) &&
        assertTrue(byAuthor.map(_.id).toSet.subsetOf(created.map(_.id).toSet)) &&
        assertTrue(byAuthor.map(_.author.data).toSet == Set(profile)) &&
        assertTrue(byUnknown.isEmpty)
    } @@ withMigration

    test("find articles by tags") {
      transaction:
        for {
          creds          <- Generator.credentials.runHead.map(_.get)
          profile        <- Generator.profileData.runHead.map(_.get)
          articleList    <- Generator.articleData.runCollectN(20)
          distincts       = articleList.distinctBy(_.title).take(20)
          userId         <- users.save(creds)
          _              <- profiles.create(userId, profile)
          created        <- ZIO.foreach(distincts)(a => articles.save(a.copy(author = AuthorId(userId)))).map(_.flatten)
          tagList         = created.take(10).map(a => EntityTag(a.id, ArticleTag("test")))
          _              <- tags.save(tagList)
          byTag          <- articles.search(List(Search.Tag("test")), offset = 0, limit = 10)
          byTagAndAuthor <- articles.search(List(Search.Tag("test"), Search.Author(profile.name)), offset = 0, limit = 10)
          byUnknown      <- articles.search(List(Search.Tag("x"), Search.Author(profile.name)), offset = 0, limit = 10)
        } yield assertTrue(byTag.nonEmpty) &&
        assertTrue(byTag.length == 10) &&
        assertTrue(byTagAndAuthor.length == 10) &&
        assertTrue(byTag.toSet == byTagAndAuthor.toSet) &&
        assertTrue(byTag.map(_.id).toSet.subsetOf(created.map(_.id).toSet)) &&
        assertTrue(byTag.map(_.author.data).toSet == Set(profile)) &&
        assertTrue(byTag.exists(_.tags.contains(ArticleTag("test")))) &&
        assertTrue(byUnknown.isEmpty)
    } @@ withMigration

    test("find articles by favorited") {
      transaction:
        for {
          // Create users
          creds1   <- Generator.credentials.runHead.map(_.get)
          profile1 <- Generator.profileData.runHead.map(_.get)
          userId1  <- users.save(creds1)
          _        <- profiles.create(userId1, profile1)

          creds2   <- Generator.credentials.runHead.map(_.get)
          profile2 <- Generator.profileData.runHead.map(_.get)
          userId2  <- users.save(creds2)
          _        <- profiles.create(userId2, profile2)

          // Create articles for User 2
          articleList <- Generator.articleData.runCollectN(20)
          distincts    = articleList.distinctBy(_.title).take(20)
          created     <- ZIO.foreach(distincts)(a => articles.save(a.copy(author = AuthorId(userId2)))).map(_.flatten)

          // User 1 favorite half of User 2's articles
          toFavorite = created.take(10)
          _         <- ZIO.foreach(toFavorite)(a => favorites.add(FavoriteArticle(userId1, a.id)))

          // Search by favorited
          byFavorited      <- articles.search(List(Search.FavoriteOf(profile1.name)), offset = 0, limit = 10)
          byUnknownUser    <- articles.search(List(Search.FavoriteOf(profile1.name + "x")), offset = 0, limit = 10)
          byUnknownArticle <- articles.search(List(Search.FavoriteOf(profile2.name)), offset = 0, limit = 10)
        } yield assertTrue(byFavorited.nonEmpty) &&
        assertTrue(byFavorited.length == 10) &&
        assertTrue(byFavorited.map(_.id).toSet.subsetOf(toFavorite.map(_.id).toSet)) &&
        assertTrue(byFavorited.map(_.author.data).toSet == Set(profile2)) &&
        assertTrue(byFavorited.forall(a => a.favoriteCount == ArticleFavoriteCount(1))) &&
        assertTrue(byUnknownUser.isEmpty) &&
        assertTrue(byUnknownArticle.isEmpty)
    } @@ withMigration

    test("Update article") {
      transaction:
        for {
          // Create user and article
          creds   <- Generator.credentials.runHead.map(_.get)
          profile <- Generator.profileData.runHead.map(_.get)
          article <- Generator.articleData.runHead.map(_.get)
          userId  <- users.save(creds)
          _       <- profiles.create(userId, profile)
          created <- articles.save(article.copy(author = AuthorId(userId)))
          created <- ZIO.fromOption(created).mapError(_ => ArticleNotCreated)

          // Update article
          newTitle    = ArticleTitle("New " + article.title)
          newDesc     = ArticleDescription(article.description + " (edited)")
          updatedData = article.copy(title = newTitle, description = newDesc)
          updated    <- articles.save(created.id, updatedData)

          // Fetch again
          found <- articles.find(created.id).someOrFail(ArticleNotFound)
        } yield assertTrue(updated.isDefined)
        && assertTrue(updated.get.id == created.id)
        && assertTrue(updated.get.data.title == newTitle)
        && assertTrue(updated.get.data.description == newDesc)
        && assertTrue(found.id == created.id)
        && assertTrue(found.data.title == newTitle)
        && assertTrue(found.data.description == newDesc)
        && assertTrue(found.metadata.createdAt == created.metadata.createdAt)
    } @@ withMigration

    test("Delete article") {
      transaction:
        for {
          // Create user and article
          creds   <- Generator.credentials.runHead.map(_.get)
          profile <- Generator.profileData.runHead.map(_.get)
          article <- Generator.articleData.runHead.map(_.get)
          userId  <- users.save(creds)
          _       <- profiles.create(userId, profile)
          created <- articles.save(article.copy(author = AuthorId(userId)))
          created <- ZIO.fromOption(created).mapError(_ => ArticleNotCreated)

          // Delete article
          deleted  <- articles.delete(created.id)
          deleted2 <- articles.delete(ArticleId(-1))

          // Fetch again
          found <- articles.find(created.id)
        } yield assertTrue(deleted.isDefined) &&
        assertTrue(deleted.get.id == created.id) &&
        assertTrue(found.isEmpty) &&
        assertTrue(deleted2.isEmpty)
    } @@ withMigration
  } @@ TestAspect.sequential
}
