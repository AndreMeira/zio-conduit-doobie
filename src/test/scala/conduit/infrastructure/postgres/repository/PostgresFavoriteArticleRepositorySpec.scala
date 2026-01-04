package conduit.infrastructure.postgres.repository

import conduit.domain.model.entity.{ FavoriteArticle, Generator }
import conduit.domain.model.error.ApplicationError
import conduit.domain.model.types.article.AuthorId
import conduit.infrastructure.inmemory.monitor.NoopMonitor
import conduit.infrastructure.postgres.PostgresTestAspect.{ transaction, withMigration }
import conduit.infrastructure.postgres.{ PostgresMigration, PostgresSpecLayers, PostgresUnitOfWork }
import zio.test.{ TestAspect, ZIOSpec, assertTrue }

object PostgresFavoriteArticleRepositorySpec extends ZIOSpec[PostgresUnitOfWork & PostgresMigration] {
  override val bootstrap = PostgresSpecLayers.layer
  val users              = PostgresUserRepository(new NoopMonitor)
  val profiles           = PostgresUserProfileRepository(new NoopMonitor)
  val favorites          = PostgresFavoriteArticleRepository(new NoopMonitor)
  val articles           = PostgresArticleRepository(new NoopMonitor)

  case object ArticleNotCreated extends ApplicationError:
    val message = "Article was not created"

  def spec = suiteAll("PostgresFavoriteArticleRepository") {
    test("Adds a favorite article and retrieves it") {
      transaction:
        for {
          credsOne   <- Generator.credentials.runHead.map(_.get)
          credsTwo   <- Generator.credentials.runHead.map(_.get)
          article    <- Generator.articleData.runHead.map(_.get)
          profileOne <- Generator.profileData.runHead.map(_.get)
          profileTwo <- Generator.profileData.runHead.map(_.get)

          userIdOne <- users.save(credsOne)
          userIdTwo <- users.save(credsTwo)
          _         <- profiles.create(userIdOne, profileOne)
          _         <- profiles.create(userIdTwo, profileTwo)

          newArticle = article.copy(author = AuthorId(userIdOne))
          created   <- articles.save(newArticle).someOrFail(ArticleNotCreated)
          favorite   = FavoriteArticle(by = userIdTwo, article = created.id)
          _         <- favorites.add(favorite)
          isFav     <- favorites.exists(favorite)
        } yield assertTrue(isFav)
    } @@ withMigration

    test("adding a favorite twice is idempotent") {
      transaction:
        for {
          credsOne   <- Generator.credentials.runHead.map(_.get)
          credsTwo   <- Generator.credentials.runHead.map(_.get)
          article    <- Generator.articleData.runHead.map(_.get)
          profileOne <- Generator.profileData.runHead.map(_.get)
          profileTwo <- Generator.profileData.runHead.map(_.get)

          userIdOne <- users.save(credsOne)
          userIdTwo <- users.save(credsTwo)
          _         <- profiles.create(userIdOne, profileOne)
          _         <- profiles.create(userIdTwo, profileTwo)

          newArticle = article.copy(author = AuthorId(userIdOne))
          created   <- articles.save(newArticle).someOrFail(ArticleNotCreated)
          favorite   = FavoriteArticle(by = userIdTwo, article = created.id)
          _         <- favorites.add(favorite)
          _         <- favorites.add(favorite) // add again
          isFav     <- favorites.exists(favorite)
        } yield assertTrue(isFav)
    } @@ withMigration

    test("delete a favorite article") {
      transaction:
        for {
          credsOne   <- Generator.credentials.runHead.map(_.get)
          credsTwo   <- Generator.credentials.runHead.map(_.get)
          article    <- Generator.articleData.runHead.map(_.get)
          profileOne <- Generator.profileData.runHead.map(_.get)
          profileTwo <- Generator.profileData.runHead.map(_.get)

          userIdOne <- users.save(credsOne)
          userIdTwo <- users.save(credsTwo)
          _         <- profiles.create(userIdOne, profileOne)
          _         <- profiles.create(userIdTwo, profileTwo)

          newArticle = article.copy(author = AuthorId(userIdOne))
          created   <- articles.save(newArticle).someOrFail(ArticleNotCreated)
          favorite   = FavoriteArticle(by = userIdTwo, article = created.id)
          _         <- favorites.add(favorite)
          isFav     <- favorites.exists(favorite)
          _         <- favorites.delete(favorite)
          isFav2    <- favorites.exists(favorite)
        } yield assertTrue(isFav) && assertTrue(!isFav2)
    } @@ withMigration
  } @@ TestAspect.sequential

}
