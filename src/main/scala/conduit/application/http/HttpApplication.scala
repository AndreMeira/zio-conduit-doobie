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

object HttpApplication extends ZIOAppDefault {
  def routes: ZIO[UserRoutes & ArticleRoutes & CommentRoutes, Nothing, Routes[Any, ApplicationError]] =
    for {
      commentRoutes <- ZIO.service[CommentRoutes]
      articleRoutes <- ZIO.service[ArticleRoutes]
      userRoutes    <- ZIO.service[UserRoutes]
    } yield commentRoutes.routes ++ articleRoutes.routes ++ userRoutes.routes

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

  override def run: ZIO[ZIOAppArgs & Scope, Any, Any] =
    getArgs.flatMap:
      case Chunk("inmemory")  => inmemory
      case Chunk("local")     => local
      case Chunk("migration") => migration
      case Chunk("live")      => live
      case _                  => ZIO.logError("Please specify what to run")
}
