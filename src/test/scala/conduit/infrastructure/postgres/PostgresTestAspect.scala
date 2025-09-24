package conduit.infrastructure.postgres

import conduit.domain.model.error.ApplicationError
import zio.ZIO
import zio.test.TestAspect

object PostgresTestAspect {
  val withMigration: TestAspect[Nothing, PostgresMigration, Throwable, Any] =
    TestAspect.aroundAllWith(ZIO.serviceWithZIO[PostgresMigration](m => m.cleanMigrations *> m.applyMigrations).orDieWith(raiseError)) { _ =>
      ZIO.serviceWithZIO[PostgresMigration](_.cleanMigrations).orDieWith(raiseError)
    }

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
