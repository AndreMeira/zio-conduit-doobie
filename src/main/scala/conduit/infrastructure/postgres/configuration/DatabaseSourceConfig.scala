package conduit.infrastructure.postgres.configuration

import com.zaxxer.hikari.HikariConfig

import scala.concurrent.duration.Duration

case class DatabaseSourceConfig(
  jdbcUrl: String,
  user: String,
  password: String,
  poolSize: Int = 10,
  connectionTestQuery: String = "SELECT 1",
  driverClass: String = "org.postgresql.Driver",
  connectionTimeout: Duration = Duration.create(60, "seconds"),
) {
  def toHikariConfig: HikariConfig =
    val config = new HikariConfig()
    config.setJdbcUrl(jdbcUrl)
    config.setUsername(user)
    config.setPassword(password)
    config.setMinimumIdle(poolSize)
    config.setMaximumPoolSize(poolSize * 2)
    config.setConnectionTestQuery(connectionTestQuery)
    config.setDriverClassName(driverClass)
    config.setConnectionTimeout(connectionTimeout.toMillis)
    config.setAutoCommit(false)
    config
}
