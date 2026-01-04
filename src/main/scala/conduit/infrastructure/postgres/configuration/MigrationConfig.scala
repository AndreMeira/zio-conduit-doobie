package conduit.infrastructure.postgres.configuration

import org.flywaydb.core.Flyway
import org.flywaydb.core.api.configuration.FluentConfiguration

import scala.jdk.CollectionConverters.*

case class MigrationConfig(
  initSql: String,
  locations: List[String],
  parameters: Map[String, String] = Map.empty,
  placeholders: Map[String, String] = Map.empty,
  allowClean: Boolean = false,
) {
  def toFlywayConfig: FluentConfiguration = {
    val builder = Flyway.configure().initSql(initSql).locations(locations*).cleanDisabled(!allowClean)
    if (parameters.nonEmpty) builder.configuration(parameters.asJava)
    if (placeholders.nonEmpty) builder.placeholders(placeholders.asJava)
    builder
  }
}
