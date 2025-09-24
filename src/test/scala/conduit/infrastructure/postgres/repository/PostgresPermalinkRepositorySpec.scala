package conduit.infrastructure.postgres.repository

import conduit.domain.model.entity.Generator
import conduit.domain.model.error.ApplicationError
import conduit.domain.model.types.article.AuthorId
import conduit.infrastructure.inmemory.monitor.NoopMonitor
import conduit.infrastructure.postgres.PostgresTestAspect.{ transaction, withMigration }
import conduit.infrastructure.postgres.{ PostgresMigration, PostgresSpecLayers, PostgresUnitOfWork }
import zio.test.{ TestAspect, ZIOSpec, assertTrue }

object PostgresPermalinkRepositorySpec extends ZIOSpec[PostgresUnitOfWork & PostgresMigration] {
  override val bootstrap = PostgresSpecLayers.layer
  val users              = PostgresUserRepository(new NoopMonitor)
  val profiles           = PostgresUserProfileRepository(new NoopMonitor)
  val articles           = PostgresArticleRepository(new NoopMonitor)
  val permalinks         = PostgresPermalinkRepository(new NoopMonitor)

  case object ArticleNotCreated extends ApplicationError:
    def message = "Article was not created"

  def spec = suiteAll("PostgresPermalinkRepository") {
    // Tests to be implemented
    test("Add a permalink and retrieve it") {
      transaction:
        for {
          creds   <- Generator.credentials.runHead.map(_.get)
          profile <- Generator.profileData.runHead.map(_.get)
          article <- Generator.articleData.runHead.map(_.get)

          userId      <- users.save(creds)
          _           <- profiles.create(userId, profile)
          newArticle   = article.copy(author = AuthorId(userId))
          created     <- articles.save(newArticle).someOrFail(ArticleNotCreated)
          _           <- permalinks.save(created.id, created.data.slug)
          _           <- permalinks.save(created.id, created.data.slug.appendIndex(2))
          fetchedOne  <- permalinks.resolve(created.data.slug)
          fetchedTwo  <- permalinks.resolve(created.data.slug.appendIndex(2))
          fetchedNone <- permalinks.resolve(created.data.slug.appendIndex(3))
        } yield assertTrue(fetchedOne.contains(created.id)) &&
        assertTrue(fetchedTwo.contains(created.id)) &&
        assertTrue(fetchedNone.isEmpty)
    } @@ withMigration
  } @@ TestAspect.sequential

}
