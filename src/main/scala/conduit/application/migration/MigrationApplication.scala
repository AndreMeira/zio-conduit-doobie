package conduit.application.migration

import com.zaxxer.hikari.HikariDataSource
import conduit.domain.logic.monitoring.Monitor
import conduit.domain.model.error.ApplicationError
import conduit.infrastructure.configuration.ConfigurationModule
import conduit.infrastructure.inmemory.monitor.InMemoryMonitor
import conduit.infrastructure.postgres.{ PostgresMigration, PostgresModule }
import conduit.infrastructure.postgres.configuration.DatabaseSourceConfig
import zio.{ Scope, ZIO, ZIOAppDefault, ZLayer }

object MigrationApplication extends ZIOAppDefault {

  override def run: ZIO[Scope, Throwable, Unit] = ZIO.scoped {
    {
      for {
        monitor   <- ZIO.service[Monitor]
        migration <- ZIO.service[PostgresMigration]
        _         <- monitor.start("migration")(migration.applyMigrations).mapError(raiseMigrationError)
      } yield ()
    }.provide(
      InMemoryMonitor.layer,
      PostgresModule.migration.layer,
      ConfigurationModule.migration.layer,
    )
  }

  private def raiseMigrationError(err: ApplicationError): Throwable =
    new Exception(s"Migration failed: ${err.kind}:${err.message}")
}
