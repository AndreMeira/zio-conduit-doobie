package conduit.infrastructure.opentelemetry

import conduit.domain.logic.monitoring.Monitor
import io.opentelemetry.api.common.Attributes
import zio.telemetry.opentelemetry.OpenTelemetry
import zio.telemetry.opentelemetry.metrics.Meter
import zio.telemetry.opentelemetry.tracing.Tracing
import zio.{ Cause, UIO, ZIO, ZLayer }

class OpenTelemetryMonitor(tracing: Tracing, meter: Meter) extends Monitor {

  override def start[R, E, A](name: String)(effect: => ZIO[R, E, A]): ZIO[R, E, A] =
    for {
      _                  <- tracing.addEvent(s"Starting root $name")
      _                  <- tracing.setAttribute("span.reference", s"$name")
      _                  <- tracing.setAttribute("logic-root", true)
      counter            <- meter.counter(s"hits: $name")
      _                  <- counter.inc()
      (duration, result) <- effect.onError(onRootError(name, _)).timed
      timer              <- meter.histogram(s"duration: $name")
      _                  <- timer.record(duration.toMillis.toDouble)
    } yield result

  override def track[R, E, A](name: String, tags: (String, String)*)(effect: => ZIO[R, E, A]): ZIO[R, E, A] =
    for {
      _                  <- ZIO.foreach(tags)(tracing.setAttribute(_, _))
      _                  <- tracing.addEvent(s"Tracking $name with tags ${tags.toMap}")
      _                  <- tracing.setAttribute("span.reference", s"$name")
      _                  <- tracing.setAttribute("logic-root", false)
      counter            <- meter.counter(s"hits: $name")
      _                  <- counter.inc()
      (duration, result) <- effect.timed
      timer              <- meter.histogram(s"latency: $name", unit = Some("ms"))
      _                  <- timer.record(duration.toMillis.toDouble)
    } yield result

  private def onRootError[E](name: String, error: Cause[E]): UIO[Unit] =
    for {
      _       <- ZIO.logError(error.toString)
      _       <- tracing.addEvent(s"$name produced ${error.failures}")
      _       <- tracing.setAttribute("error", true)
      counter <- meter.counter(s"errors: $name")
      _       <- counter.inc()
    } yield ()

  private def buildAttributes(tags: (String, String)*): UIO[Attributes] =
    ZIO.succeed:
      val builder = Attributes.builder()
      tags.foreach { case (k, v) => builder.put(k, v) }
      builder.build()
}

object OpenTelemetryMonitor:
  val otelSdk     = OpenTelemetry.global
  val otelTracing = OpenTelemetry.tracing("conduit-tracing")
  val otelContext = OpenTelemetry.contextJVM
  val otelMetrics = OpenTelemetry.metrics("conduit-metrics")
  val otel        = (otelSdk ++ otelContext) >>> otelTracing
  val metrics     = (otelSdk ++ otelContext) >>> otelMetrics

  val layer = ZLayer {
    for {
      tracing <- ZIO.service[Tracing]
      meter   <- ZIO.service[Meter]
    } yield new OpenTelemetryMonitor(tracing, meter)
  }
