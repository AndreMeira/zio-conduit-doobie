package conduit.application.migration

import conduit.domain.logic.monitoring.Monitor
import conduit.domain.model.error.ApplicationError
import conduit.infrastructure.configuration.ConfigurationModule
import conduit.infrastructure.opentelemetry.{OpenTelemetryMonitor, Module as OtelModule}
import conduit.infrastructure.postgres.{PostgresMigration, PostgresModule}
import zio.{Scope, ZIO, ZIOAppDefault, ZLayer}

object MigrationApplication extends ZIOAppDefault {

  override def run: ZIO[Scope, Throwable, Unit] = ZIO.scoped {
    {
      for {
        monitor   <- ZIO.service[Monitor]
        migration <- ZIO.service[PostgresMigration]
        _         <- monitor.start("migration")(migration.applyMigrations).mapError(raiseMigrationError)
      } yield ()
    }.provide(
      OtelModule.layer,
      PostgresModule.migration.layer,
      ConfigurationModule.migration.layer,
    )
  }

  private def raiseMigrationError(err: ApplicationError): Throwable =
    new Exception(s"Migration failed: ${err.kind}:${err.message}")
}
