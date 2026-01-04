package conduit.infrastructure.postgres.repository

import conduit.domain.model.entity.{ Follower, Generator }
import conduit.domain.model.types.article.AuthorId
import conduit.infrastructure.inmemory.monitor.NoopMonitor
import conduit.infrastructure.postgres.PostgresTestAspect.{ transaction, withMigration }
import conduit.infrastructure.postgres.{ PostgresMigration, PostgresSpecLayers, PostgresUnitOfWork }
import zio.test.{ TestAspect, ZIOSpec, assertTrue }

object PostgresFollowerRepositorySpec extends ZIOSpec[PostgresUnitOfWork & PostgresMigration] {
  override val bootstrap = PostgresSpecLayers.layer
  val users              = PostgresUserRepository(new NoopMonitor)
  val profiles           = PostgresUserProfileRepository(new NoopMonitor)
  val followers          = PostgresFollowerRepository(new NoopMonitor)
  val articles           = PostgresArticleRepository(new NoopMonitor)

  def spec = suiteAll("PostgresFollowerRepository") {
    test("Follows and unfollows a user") {
      transaction:
        for {
          credsOne   <- Generator.credentials.runHead.map(_.get)
          credsTwo   <- Generator.credentials.runHead.map(_.get)
          profileOne <- Generator.profileData.runHead.map(_.get)
          profileTwo <- Generator.profileData.runHead.map(_.get)

          userIdOne      <- users.save(credsOne)
          userIdTwo      <- users.save(credsTwo)
          _              <- profiles.create(userIdOne, profileOne)
          _              <- profiles.create(userIdTwo, profileTwo)
          follower        = Follower(by = userIdTwo, author = AuthorId(userIdOne))
          _              <- followers.save(follower)
          isFollowing    <- followers.exists(follower)
          _              <- followers.delete(follower)
          isNotFollowing <- followers.exists(follower)
        } yield assertTrue(isFollowing) && assertTrue(!isNotFollowing)
    } @@ withMigration
  } @@ TestAspect.sequential
}
