package conduit.infrastructure.postgres.repository

import conduit.domain.model.entity.Credentials
import conduit.domain.model.types.user.{ Email, HashedPassword }
import conduit.infrastructure.inmemory.monitor.NoopMonitor
import conduit.infrastructure.postgres.PostgresTestAspect.{ transaction, withMigration }
import conduit.infrastructure.postgres.{ PostgresMigration, PostgresSpecLayers, PostgresUnitOfWork }
import zio.test.{ Spec, TestAspect, ZIOSpec, assertTrue }

object PostgresUserRepositorySpec extends ZIOSpec[PostgresUnitOfWork & PostgresMigration] {
  override val bootstrap = PostgresSpecLayers.layer
  val repository         = PostgresUserRepository(new NoopMonitor)

  def spec = suiteAll("PostgresArticleRepository") {

    test("Insert a user and retrieve it") {
      transaction:
        val email    = Email("andre.meira@conduit.com")
        val password = HashedPassword("hashedpassword")
        for {
          userId <- repository.save(Credentials.Hashed(email, password))
          found  <- repository.findCredentials(userId)
        } yield assertTrue(found.contains(Credentials.Hashed(email, password)))
    } @@ withMigration

    test("Insert a user and found their email") {
      transaction:
        val email    = Email("andre.meira@conduit.com")
        val password = HashedPassword("hashedpassword")
        for {
          userId <- repository.save(Credentials.Hashed(email, password))
          found  <- repository.findEmail(userId)
        } yield assertTrue(found.contains(email))
    } @@ withMigration

    test("Insert a user and retrieve their credentials") {
      transaction:
        val email    = Email("andre.meira@conduit.com")
        val password = HashedPassword("hashedpassword")
        for {
          userId <- repository.save(Credentials.Hashed(email, password))
          found  <- repository.findCredentials(userId)
        } yield assertTrue(found.contains(Credentials.Hashed(email, password)))
    } @@ withMigration

    test("Check email existence") {
      transaction:
        val email    = Email("andre.meira@conduit.com")
        val password = HashedPassword("hashedpassword")
        for {
          existsBefore <- repository.emailExists(email)
          _            <- repository.save(Credentials.Hashed(email, password))
          existsAfter  <- repository.emailExists(email)
        } yield assertTrue(!existsBefore && existsAfter)
    } @@ withMigration

    test("Update credentials") {
      transaction:
        val email       = Email("")
        val password    = HashedPassword("hashedpassword")
        val newPassword = HashedPassword("newhashedpassword")
        for {
          userId <- repository.save(Credentials.Hashed(email, password))
          _      <- repository.save(userId, Credentials.Hashed(email, newPassword))
          found  <- repository.findCredentials(userId)
        } yield assertTrue(found.contains(Credentials.Hashed(email, newPassword)))
    } @@ withMigration

  } @@ TestAspect.sequential
}
