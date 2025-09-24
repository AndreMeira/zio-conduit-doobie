package conduit.infrastructure.postgres.repository

import conduit.domain.model.types.article.ArticleSlug
import conduit.infrastructure.inmemory.monitor.NoopMonitor
import conduit.infrastructure.postgres.PostgresTestAspect.{ transaction, withMigration }
import conduit.infrastructure.postgres.{ PostgresMigration, PostgresSpecLayers, PostgresUnitOfWork }
import zio.ZIO
import zio.test.{ TestAspect, ZIOSpec, assertTrue }

object PostgresArticleSlugRepositorySpec extends ZIOSpec[PostgresUnitOfWork & PostgresMigration] {
  override val bootstrap = PostgresSpecLayers.layer
  val slugs              = PostgresArticleSlugRepository(new NoopMonitor)

  def spec = suiteAll("PostgresArticleSlugRepository") {
    test("generate unique slugs") {
      transaction {
        for {
          slugOne <- slugs.nextAvailable(ArticleSlug("test-slug"))
          slugTwo <- slugs.nextAvailable(ArticleSlug("test-slug"))
        } yield assertTrue(slugOne != slugTwo) &&
        assertTrue(slugOne == "test-slug") &&
        assertTrue(slugTwo == "test-slug_2")
      }
    } @@ withMigration

    test("generate unique slugs concurrently") {
      val tasks = List.fill(10)(slugs.nextAvailable(ArticleSlug("concurrent-slug")))
      transaction {
        for {
          results <- ZIO.collectAllPar(tasks)
          distinct = results.distinct
        } yield assertTrue(results.size == 10) &&
        assertTrue(distinct.size == 10) &&
        assertTrue(distinct.contains("concurrent-slug")) &&
        assertTrue(distinct.contains("concurrent-slug_10"))
      }
    } @@ withMigration
  } @@ TestAspect.sequential
}
