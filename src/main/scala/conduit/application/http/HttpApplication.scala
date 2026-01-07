package conduit.application.http

import conduit.application.http.middleware.{ ErrorMiddleware, MonitorMiddleware }
import conduit.application.http.route.*
import conduit.domain.model.error.ApplicationError
import conduit.domain.service.DomainServiceModule
import conduit.infrastructure.configuration.ConfigurationModule
import conduit.infrastructure.inmemory.InMemoryModule
import conduit.infrastructure.inmemory.repository.Transaction as MemoryTransaction
import conduit.infrastructure.opentelemetry.Module as OtelModule
import conduit.infrastructure.postgres.{ PostgresMigration, PostgresModule, Transaction as PostgresTransaction }
import izumi.reflect.Tag as ReflectionTag
import zio.*
import zio.http.{ Middleware, Routes, Server }

/**
 * Main HTTP application for the Conduit system.
 *
 * This object serves as the entry point for running the Conduit API server
 * in different modes (inmemory, local development, production, migration-only).
 * It uses ZIO's dependency injection to wire together all layers of the application.
 */
object HttpApplication extends ZIOAppDefault {

  /**
   * Combines all HTTP routes into a single Routes instance.
   *
   * Aggregates user, article, and comment routes into a unified
   * routing structure for the HTTP server.
   *
   * @return Combined routes from all modules
   */
  def routes: ZIO[UserRoutes & ArticleRoutes & CommentRoutes, Nothing, Routes[Any, ApplicationError]] =
    for {
      commentRoutes <- ZIO.service[CommentRoutes]
      articleRoutes <- ZIO.service[ArticleRoutes]
      userRoutes    <- ZIO.service[UserRoutes]
    } yield commentRoutes.routes ++ articleRoutes.routes ++ userRoutes.routes

  /**
   * Runs the application in inmemory mode for testing and development.
   *
   * Uses in-memory storage instead of a database, making it suitable for
   * rapid development and testing. Includes tracing routes and CORS support.
   *
   * @return ZIO effect that runs the server until interrupted
   */
  def inmemory: ZIO[Scope, Throwable, Unit] = ZIO.scoped {
    {
      for {
        routes   <- routes
        traces   <- ZIO.service[InMemoryTraceRoute]
        monitor  <- ZIO.service[MonitorMiddleware]
        recover  <- ZIO.service[ErrorMiddleware]
        allRoutes = recover(routes @@ monitor) ++ (traces.routes @@ Middleware.cors)
        _        <- Server.serve(allRoutes)
      } yield ()
    }.provide(
      HttpModule.layer,                             // application layer
      DomainServiceModule.layer[MemoryTransaction], // domain layer
      InMemoryModule.layer,                         // infrastructure layer
      ConfigurationModule.http.layer,               // configuration layer
    )
  }

  /**
   * Runs the application in local development mode with PostgreSQL.
   *
   * Applies database migrations and runs the server with full monitoring
   * and PostgreSQL persistence. Suitable for local development that needs
   * database state persistence.
   *
   * @return ZIO effect that runs the server until interrupted
   */
  def local: ZIO[Scope, Throwable | PostgresMigration.Error, Unit] = ZIO.scoped {
    {
      for {
        migration <- ZIO.service[PostgresMigration]
        _         <- migration.applyMigrations
        routes    <- routes
        monitor   <- ZIO.service[MonitorMiddleware]
        recover   <- ZIO.service[ErrorMiddleware]
        allRoutes  = recover(routes @@ monitor)
        _         <- Server.serve(allRoutes)
      } yield ()
    }.provide(
      HttpModule.layer,                               // application layer
      OtelModule.layer,                               // monitoring layer
      DomainServiceModule.layer[PostgresTransaction], // domain layer
      ConfigurationModule.http.layer,                 // configuration layer
      ConfigurationModule.postgres.layer,             // configuration layer
      PostgresModule.layer,                           // infrastructure layer
      PostgresModule.migration.layer,                 // migration layer
    )
  }

  /**
   * Runs database migrations only without starting the HTTP server.
   *
   * Useful for deployment pipelines where database schema needs to be
   * updated before starting the application servers.
   *
   * @return ZIO effect that applies migrations and exits
   */
  def migration: ZIO[Scope, Throwable | PostgresMigration.Error, Unit] = ZIO.scoped {
    {
      for {
        migration <- ZIO.service[PostgresMigration]
        _         <- migration.applyMigrations
      } yield ()
    }.provide(
      OtelModule.layer,                   // monitoring layer
      ConfigurationModule.postgres.layer, // configuration layer
      PostgresModule.migration.layer,     // migration layer
    )
  }

  /**
   * Runs the application in production mode.
   *
   * Full production setup with PostgreSQL persistence, monitoring,
   * and all production-ready middleware. Database migrations are
   * expected to have been run separately.
   *
   * @return ZIO effect that runs the server until interrupted
   */
  def live: ZIO[Scope, Throwable, Unit] = ZIO.scoped {
    {
      for {
        routes   <- routes
        monitor  <- ZIO.service[MonitorMiddleware]
        recover  <- ZIO.service[ErrorMiddleware]
        allRoutes = recover(routes @@ monitor)
        _        <- Server.serve(allRoutes)
      } yield ()
    }.provide(
      HttpModule.layer,                               // application layer
      OtelModule.layer,                               // monitoring layer
      DomainServiceModule.layer[PostgresTransaction], // domain layer
      ConfigurationModule.http.layer,                 // configuration layer
      ConfigurationModule.postgres.layer,             // configuration layer
      PostgresModule.layer,                           // infrastructure layer
    )
  }

  /**
   * Application entry point that dispatches to the appropriate mode.
   *
   * Examines command line arguments to determine which mode to run:
   * - "inmemory": In-memory mode for testing
   * - "local": Local development with database
   * - "migration": Run migrations only
   * - "live": Production mode
   *
   * @return ZIO effect based on the selected mode
   */
  override def run: ZIO[ZIOAppArgs & Scope, Any, Any] =
    getArgs.flatMap:
      case Chunk("inmemory")  => inmemory
      case Chunk("local")     => local
      case Chunk("migration") => migration
      case Chunk("live")      => live
      case _                  => ZIO.logError("Please specify what to run")
}
