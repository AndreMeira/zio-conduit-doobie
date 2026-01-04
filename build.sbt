val scala3Version         = "3.7.2"
val zioVersion            = "2.1.24"
val zioJsonVersion        = "0.8.0"
val zioConfigVersion      = "4.0.6"
val zioLoggingVersion     = "2.5.2"
val logbackClassicVersion = "1.5.23"
val postgresqlVersion     = "42.7.8"
val testContainersVersion = "0.44.1"
val zioHttpVersion        = "3.7.4"
val pureConfigVersion     = "0.17.9"
val circeVersion          = "0.14.15"
val commonCodecVersion    = "1.20.0"
val jwtVersion            = "0.13.0"
val flywayVersion         = "11.20.0"
val zioOtel               = "3.1.13"
val javaOtel              = "1.57.0"
val OtelSemconv           = "1.37.0"
val javaOtelAgent         = "2.20.1"

ThisBuild / organization := "com.andremeira"
ThisBuild / scalaVersion := scala3Version
Test / parallelExecution := false

def getConfig(key: String, default: String): String =
  sys.env.getOrElse(key, sys.props.getOrElse(key.toLowerCase.replace("_", "."), default))

def otelJavaOptions: Seq[String] = Seq(
  s"-Dotel.logs.exporter=${getConfig("OTEL_LOGS_EXPORTER", "none")}",
  s"-Dotel.service.name=${getConfig("OTEL_SERVICE_NAME", "conduit-doobie")}",
  s"-Dotel.traces.exporter=${getConfig("OTEL_TRACES_EXPORTER", "otlp")}",
  s"-Dotel.metrics.exporter=${getConfig("OTEL_METRICS_EXPORTER", "otlp")}",
  s"-Dotel.javaagent.debug=${getConfig("OTEL_JAVAAGENT_DEBUG", "true")}",
  s"-Dotel.exporter.otlp.protocol=${getConfig("OTEL_EXPORTER_OTLP_PROTOCOL", "grpc")}",
  s"""-Dotel.exporter.otlp.endpoint=${getConfig("OTEL_EXPORTER_OTLP_ENDPOINT", "http://localhost:4317")}""",
)

lazy val root = project
  .in(file("."))
  .settings(
    name                                      := "zio-conduit-doobie",
    scalaVersion                              := scala3Version,
    version                                   := "0.1.0-SNAPSHOT",
    javaAgents += "io.opentelemetry.javaagent" % "opentelemetry-javaagent" % javaOtelAgent % "compile;dist",
    javaOptions ++= otelJavaOptions,
    libraryDependencies ++= Seq(
      // "io.getquill"   %% "quill-jdbc-zio"      % quillVersion,
      "org.postgresql" % "postgresql"      % postgresqlVersion,
      "dev.zio"       %% "zio"             % zioVersion,
      "dev.zio"       %% "zio-streams"     % zioVersion,
      "dev.zio"       %% "zio-http"        % zioHttpVersion,
      "ch.qos.logback" % "logback-classic" % logbackClassicVersion,
      "dev.zio"       %% "zio-json"        % zioJsonVersion,

      // databases
      "com.zaxxer"     % "HikariCP"         % "7.0.2",
      "org.tpolecat"  %% "doobie-core"      % "1.0.0-RC11",
      "org.tpolecat"  %% "doobie-hikari"    % "1.0.0-RC11",
      "org.tpolecat"  %% "doobie-postgres"  % "1.0.0-RC11",
      "org.postgresql" % "postgresql"       % "42.7.8",
      "dev.zio"       %% "zio-interop-cats" % "23.1.0.13",

      // config
      "com.github.pureconfig" %% "pureconfig-core"            % pureConfigVersion,
      "dev.zio"               %% "zio-config"                 % zioConfigVersion,
      "dev.zio"               %% "zio-config-typesafe"        % zioConfigVersion,
      "dev.zio"               %% "zio-config-magnolia"        % zioConfigVersion,
      "org.flywaydb"           % "flyway-core"                % flywayVersion,
      "org.flywaydb"           % "flyway-database-postgresql" % flywayVersion,

      // logging
      "dev.zio"       %% "zio-logging"       % zioLoggingVersion,
      "dev.zio"       %% "zio-logging-slf4j" % zioLoggingVersion,
      "ch.qos.logback" % "logback-classic"   % logbackClassicVersion,

      // json
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-core"    % circeVersion,
      "io.circe" %% "circe-parser"  % circeVersion,

      // cryptography and encoding
      "commons-codec"   % "commons-codec" % commonCodecVersion,
      "io.jsonwebtoken" % "jjwt-api"      % jwtVersion,
      "io.jsonwebtoken" % "jjwt-impl"     % jwtVersion,
      "io.jsonwebtoken" % "jjwt-jackson"  % jwtVersion,

      // opentelemetry
      "dev.zio"                 %% "zio-opentelemetry"                   % zioOtel,
      "io.opentelemetry"         % "opentelemetry-sdk"                   % javaOtel,
      "io.opentelemetry"         % "opentelemetry-sdk-trace"             % javaOtel,
      "io.opentelemetry"         % "opentelemetry-exporter-otlp"         % javaOtel,
      "io.opentelemetry"         % "opentelemetry-exporter-logging-otlp" % javaOtel,
      "io.opentelemetry.semconv" % "opentelemetry-semconv"               % OtelSemconv,

      // test
      "dev.zio"      %% "zio-test"                        % zioVersion            % Test,
      "dev.zio"      %% "zio-test-sbt"                    % zioVersion            % Test,
      "dev.zio"      %% "zio-test-junit"                  % zioVersion            % Test,
      "com.dimafeng" %% "testcontainers-scala-postgresql" % testContainersVersion % Test,
      "dev.zio"      %% "zio-test-magnolia"               % zioVersion            % Test,
    ),
    testFrameworks                            := Seq(new TestFramework("zio.test.sbt.ZTestFramework")),
  )
  .enablePlugins(JavaAppPackaging, JavaAgent)
