package conduit.infrastructure.configuration

import conduit.domain.service.authentication.{ CredentialsAuthenticationService, TokenAuthenticationService }
import conduit.infrastructure.postgres.configuration.{ DatabaseSourceConfig, MigrationConfig }
import pureconfig.{ ConfigReader, ConfigSource }
import zio.*

object ConfigurationModule {
  given readerToken: ConfigReader[TokenAuthenticationService.Config]       = ConfigReader.derived
  given readerCreds: ConfigReader[CredentialsAuthenticationService.Config] = ConfigReader.derived
  given readerDS: ConfigReader[DatabaseSourceConfig]                       = ConfigReader.derived
  given readerMig: ConfigReader[MigrationConfig]                           = ConfigReader.derived

  object config:
    val token       = loadLayer[TokenAuthenticationService.Config]("config/token.conf")
    val credentials = loadLayer[CredentialsAuthenticationService.Config]("config/credentials.conf")
    val datasource  = loadLayer[DatabaseSourceConfig]("config/datasource.conf")
    val migration   = loadLayer[MigrationConfig]("config/migration.conf")

  // application layers
  object http:
    val layer = config.token ++ config.credentials

  object migration:
    val layer = config.datasource ++ config.migration

  object postgres:
    val layer = config.datasource ++ config.migration

  private def loadLayer[A: ConfigReader: Tag](path: String): ZLayer[Any, Throwable, A] =
    ZLayer.scoped(load[A](path))

  private def load[A: ConfigReader: Tag](path: String): ZIO[Any, Throwable, A] =
    ZIO
      .fromEither(ConfigSource.resources(path).load[A])
      .mapError(err => new RuntimeException(s"Failed to load config from $path: $err"))
}
