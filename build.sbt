val scala3Version         = "3.7.2"
val zioVersion            = "2.1.20"
val zioJsonVersion        = "0.7.44"
val zioConfigVersion       = "4.0.4"
val zioLoggingVersion     = "2.5.1"
val logbackClassicVersion = "1.5.18"
val postgresqlVersion     = "42.7.7"
val testContainersVersion = "0.43.0"
val zioHttpVersion        = "3.4.0"
val quillVersion          = "4.8.6"
val pureConfigVersion      = "0.17.9"
val circeVersion          = "0.14.14"
val commonCodecVersion    = "1.19.0"
val jwtVersion            = "0.13.0"

ThisBuild / organization := "com.andremeira"
ThisBuild / scalaVersion := scala3Version

lazy val root = project
  .in(file("."))
  .settings(
    name := "zio-conduit-quill",
    version := "0.1.0-SNAPSHOT",

    scalaVersion := scala3Version,

    libraryDependencies ++= Seq(
      "io.getquill"   %% "quill-jdbc-zio"      % quillVersion,
      "org.postgresql" % "postgresql"          % postgresqlVersion,
      "dev.zio"       %% "zio"                 % zioVersion,
      "dev.zio"       %% "zio-streams"         % zioVersion,
      "dev.zio"       %% "zio-http"            % zioHttpVersion,
      "ch.qos.logback" % "logback-classic"     % logbackClassicVersion,
      "dev.zio"       %% "zio-json"            % zioJsonVersion,

      // config
      "com.github.pureconfig" %% "pureconfig-core"     % pureConfigVersion,
      "dev.zio"              %% "zio-config"          % zioConfigVersion,
      "dev.zio"              %% "zio-config-typesafe" % zioConfigVersion,
      "dev.zio"              %% "zio-config-magnolia" % zioConfigVersion,

      // logging
      "dev.zio"       %% "zio-logging"       % zioLoggingVersion,
      "dev.zio"       %% "zio-logging-slf4j" % zioLoggingVersion,
      "ch.qos.logback" % "logback-classic"   % logbackClassicVersion,

      // json
      "io.circe"      %% "circe-generic" % circeVersion,
      "io.circe"      %% "circe-core"    % circeVersion,
      "io.circe"      %% "circe-parser"  % circeVersion,

      // cryptography and encoding
      "commons-codec"  % "commons-codec" % commonCodecVersion,
      "io.jsonwebtoken" % "jjwt-api"     % jwtVersion,
      "io.jsonwebtoken" % "jjwt-impl"    % jwtVersion,
      "io.jsonwebtoken" % "jjwt-jackson" % jwtVersion,

      // test
      "dev.zio"      %% "zio-test"                        % zioVersion            % Test,
      "dev.zio"      %% "zio-test-sbt"                    % zioVersion            % Test,
      "dev.zio"      %% "zio-test-junit"                  % zioVersion            % Test,
      "com.dimafeng" %% "testcontainers-scala-postgresql" % testContainersVersion % Test,
      "dev.zio"      %% "zio-test-magnolia"               % zioVersion            % Test,
    ),
    testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework")),
  )
  .enablePlugins(JavaAppPackaging)
