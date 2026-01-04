package conduit.infrastructure.postgres

import conduit.domain.model.error.ApplicationError
import zio.{ URIO, ZIO }
import zio.test.TestAspect

object PostgresTestAspect {
  val withMigration: TestAspect[Nothing, PostgresMigration, Throwable, Any] =
    TestAspect.aroundAllWith(migrate)(_ => cleanMigration)

  private def migrate: URIO[PostgresMigration, Unit] =
    ZIO
      .serviceWithZIO[PostgresMigration] { migration =>
        migration.cleanMigrations *> migration.applyMigrations
      }
      .orDieWith(raiseError)

  private def cleanMigration: URIO[PostgresMigration, Unit] =
    ZIO.serviceWithZIO[PostgresMigration](_.cleanMigrations).orDieWith(raiseError)

  def transaction[R, A](
    effect: ZIO[R & Transaction, ApplicationError, A]
  ): ZIO[R & PostgresUnitOfWork, ApplicationError, A] =
    ZIO.serviceWithZIO[PostgresUnitOfWork](_.execute(effect))

  private def raiseError(error: PostgresMigration.Error): Throwable =
    error match {
      case PostgresMigration.Error.CleanFailed(cause)     => new RuntimeException("Failed to clean migrations", cause)
      case PostgresMigration.Error.MigrationFailed(cause) => new RuntimeException("Failed to apply migrations", cause)
    }
}
