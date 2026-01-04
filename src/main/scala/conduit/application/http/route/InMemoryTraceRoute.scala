package conduit.application.http.route

import conduit.application.http.route.InMemoryTraceRoute.given
import conduit.domain.logic.monitoring.Monitor
import conduit.infrastructure.inmemory.monitor.{ InMemoryMonitor, Span }
import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder as encoder
import io.circe.syntax.*
import zio.{ ZIO, ZLayer }
import zio.http.Method.GET
import zio.http.codec.PathCodec.literal
import zio.http.{ Request, Response, Routes, Status, handler }

import java.time.Duration

class InMemoryTraceRoute(monitor: Monitor) {
  def routes: Routes[Any, Nothing] =
    literal("api") / Routes(
      // get traces
      GET / "traces"          -> handler { (_: Request) =>
        for {
          traces <- getTraces
        } yield Response.json(traces.asJson.toString)
      },
      GET / "traces" / "last" -> handler { (_: Request) =>
        for {
          traces <- getTraces
          last    = traces.headOption // get the most recent trace
        } yield last match
          case Some(span) => Response.json(span.asJson.toString)
          case None       => Response.status(Status.NotFound)
      },
    )

  private def getTraces: ZIO[Any, Nothing, List[Span]] =
    monitor match {
      case m: InMemoryMonitor => m.getTraces
      case _                  => ZIO.succeed(List.empty) // if not InMemoryMonitor, return empty list
    }
}

object InMemoryTraceRoute:
  given Encoder[Span]          = encoder
  given Encoder[Span.Id]       = encoder
  given Encoder[Span.Data]     = encoder
  given Encoder[Span.Timeline] = encoder
  given Encoder[Duration]      = Encoder[Long].contramap(duration => duration.toMillis)

  val layer = ZLayer {
    for {
      monitor <- ZIO.service[Monitor]
    } yield new InMemoryTraceRoute(monitor)
  }
