package conduit.infrastructure.postgres

import com.dimafeng.testcontainers.PostgreSQLContainer
import com.zaxxer.hikari.HikariDataSource
import conduit.infrastructure.inmemory.monitor.NoopMonitor
import conduit.infrastructure.postgres.configuration.{ DatabaseSourceConfig, MigrationConfig }
import org.testcontainers.utility.DockerImageName
import zio.ZIO.acquireRelease
import zio.*

import scala.concurrent.duration.Duration

object PostgresSpecLayers {

  val image = "postgres:17-alpine"

  val postgres: ZLayer[Any, Throwable, PostgreSQLContainer] = ZLayer.scoped {
    ZIO
      .attemptBlocking(PostgreSQLContainer(DockerImageName.parse(image)))
      .flatMap: container =>
        acquireRelease(ZIO.attemptBlocking(container.start()).as(container)) { container =>
          ZIO.attemptBlocking(container.stop()).unit.orDie
        }
  }

  val migrationConfig: ZLayer[PostgreSQLContainer, Nothing, MigrationConfig]       = ZLayer {
    ZIO.service[PostgreSQLContainer].map { container =>
      MigrationConfig(
        initSql = "",
        allowClean = true,
        parameters = Map.empty,
        locations = List("classpath:migrations/schema"),
      )
    }
  }
  val datasourceConfig: ZLayer[PostgreSQLContainer, Nothing, DatabaseSourceConfig] = ZLayer {
    ZIO.service[PostgreSQLContainer].map { container =>
      DatabaseSourceConfig(
        jdbcUrl = container.jdbcUrl,
        user = container.username,
        password = container.password,
        connectionTimeout = Duration.create(60, "seconds"),
      )
    }
  }

  val configs: TaskLayer[MigrationConfig & DatabaseSourceConfig] =
    postgres >>> (migrationConfig ++ datasourceConfig)

  val datasource: TaskLayer[HikariDataSource] =
    configs >>> PostgresModule.datasource

  val unitOfWork: TaskLayer[PostgresUnitOfWork] =
    (configs ++ NoopMonitor.layer) >>> PostgresModule.unitOfWork

  val migration: TaskLayer[PostgresMigration] =
    (configs ++ NoopMonitor.layer) >>> PostgresModule.migration.layer

  val layer: TaskLayer[MigrationConfig & DatabaseSourceConfig & PostgreSQLContainer & PostgresUnitOfWork & PostgresMigration] =
    configs ++ postgres ++ unitOfWork ++ migration
}
