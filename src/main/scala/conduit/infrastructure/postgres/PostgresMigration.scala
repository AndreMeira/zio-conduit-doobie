package conduit.infrastructure.postgres

import com.zaxxer.hikari.HikariDataSource
import conduit.domain.logic.monitoring.Monitor
import conduit.domain.model.error.ApplicationError
import conduit.domain.model.error.ApplicationError.FromException
import conduit.infrastructure.postgres.PostgresMigration.Error
import conduit.infrastructure.postgres.configuration.MigrationConfig
import org.flywaydb.core.Flyway
import zio.{ ZIO, ZLayer }

class PostgresMigration(monitor: Monitor, flyway: Flyway) {

  def applyMigrations: ZIO[Any, PostgresMigration.Error, Unit] =
    monitor.track("PostgresMigration.applyMigrations", "resource" -> "db"):
      ZIO.attemptBlocking(flyway.migrate()).mapError(Error.MigrationFailed(_)).unit

  def cleanMigrations: ZIO[Any, PostgresMigration.Error, Unit] =
    monitor.track("PostgresMigration.cleanMigrations", "resource" -> "db"):
      ZIO.attemptBlocking(flyway.clean()).mapError(Error.CleanFailed(_)).unit

}

object PostgresMigration {
  // @todo refine errors
  enum Error extends ApplicationError:
    case CleanFailed(cause: Throwable)     extends Error, FromException(cause)
    case MigrationFailed(cause: Throwable) extends Error, FromException(cause)

  val layer: ZLayer[Monitor & MigrationConfig & HikariDataSource, Throwable, PostgresMigration] =
    ZLayer.scoped {
      for {
        monitor    <- ZIO.service[Monitor]
        config     <- ZIO.service[MigrationConfig].map(_.toFlywayConfig)
        datasource <- ZIO.service[HikariDataSource]
        flyway     <- ZIO.attempt(config.dataSource(datasource).load())
      } yield PostgresMigration(monitor, flyway)
    }
}
