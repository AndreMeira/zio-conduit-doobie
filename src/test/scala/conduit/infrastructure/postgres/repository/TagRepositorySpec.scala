package conduit.infrastructure.postgres.repository

import conduit.domain.model.entity.{ Generator, Tag }
import conduit.domain.model.error.ApplicationError
import conduit.domain.model.types.article.{ ArticleTag, AuthorId }
import conduit.infrastructure.inmemory.monitor.NoopMonitor
import conduit.infrastructure.postgres.PostgresTestAspect.{ transaction, withMigration }
import conduit.infrastructure.postgres.{ PostgresMigration, PostgresSpecLayers, PostgresUnitOfWork }
import zio.test.{ TestAspect, ZIOSpec, assertTrue }

object TagRepositorySpec extends ZIOSpec[PostgresUnitOfWork & PostgresMigration] {
  override val bootstrap = PostgresSpecLayers.layer
  val users              = PostgresUserRepository(new NoopMonitor)
  val profiles           = PostgresUserProfileRepository(new NoopMonitor)
  val articles           = PostgresArticleRepository(new NoopMonitor)
  val tags               = PostgresTagRepository(new NoopMonitor)

  case object ArticleNotCreated extends ApplicationError:
    def message = "Article was not created"

  def spec = suiteAll("PostgresTagRepository") {
    test("Add tags and retrieve them") {
      transaction:
        for {
          creds   <- Generator.credentials.runHead.map(_.get)
          profile <- Generator.profileData.runHead.map(_.get)
          article <- Generator.articleData.runHead.map(_.get)

          userId             <- users.save(creds)
          _                  <- profiles.create(userId, profile)
          newArticle          = article.copy(author = AuthorId(userId))
          created            <- articles.save(newArticle).someOrFail(ArticleNotCreated)
          articleTags         = List(Tag(created.id, ArticleTag("tag1")), Tag(created.id, ArticleTag("tag2")))
          _                  <- tags.save(articleTags)
          fetchedTags        <- tags.distinct
          _                  <- tags.deleteByArticle(created.id)
          fetchedAfterDelete <- tags.distinct
        } yield assertTrue(fetchedTags.toSet == articleTags.map(_.tag).toSet) &&
        assertTrue(fetchedAfterDelete.isEmpty)
    } @@ withMigration
  } @@ TestAspect.sequential

}
