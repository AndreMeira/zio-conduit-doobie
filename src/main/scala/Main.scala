import conduit.application.http.HttpApplication
import conduit.application.migration.MigrationApplication
import zio.*

object Main extends ZIOAppDefault:
  override def run: ZIO[ZIOAppArgs & Scope, Any, Any] =
    getArgs.flatMap:
      case Chunk("http", "inmemory")    => HttpApplication.inmemory
      case Chunk("http", "local")       => HttpApplication.local
      case Chunk("database", "migrate") => MigrationApplication.run
      case _                            => ZIO.logError("Please specify what to run")
