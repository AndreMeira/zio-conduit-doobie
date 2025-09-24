package conduit.infrastructure.postgres

import com.zaxxer.hikari.HikariDataSource
import conduit.domain.logic.monitoring.Monitor
import conduit.infrastructure.postgres.configuration.{ DatabaseSourceConfig, MigrationConfig }
import conduit.infrastructure.postgres.repository.*
import zio.*

object PostgresModule {
  val datasource = ZLayer.scoped:
    ZIO.service[DatabaseSourceConfig].map(config => new HikariDataSource(config.toHikariConfig)).withFinalizerAuto

  val unitOfWork = datasource >>> PostgresUnitOfWork.layer
  val layer      = PostgresRepositoryModule.layer ++ unitOfWork ++ datasource

  object migration:
    val layer = datasource >>> PostgresMigration.layer

}
