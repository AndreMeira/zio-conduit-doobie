package conduit.infrastructure.opentelemetry

import conduit.domain.logic.monitoring.Monitor
import io.opentelemetry.api.common.Attributes
import zio.telemetry.opentelemetry.OpenTelemetry
import zio.telemetry.opentelemetry.metrics.Meter
import zio.telemetry.opentelemetry.tracing.Tracing
import zio.{ Cause, UIO, ZIO, ZLayer }

class OpenTelemetryMonitor(tracing: Tracing, meter: Meter) extends Monitor {
  import tracing.aspects.span
  import tracing.aspects.root

  override def start[R, E, A](name: String)(effect: => ZIO[R, E, A]): ZIO[R, E, A] =
    for {
      attrs              <- buildAttributes("reference" -> "root")
      _                  <- meter.counter(s"hits-${normalizeMetricsName(name)}").flatMap(_.inc())
      (duration, result) <- (effect @@ root(name, attributes = attrs)).onError(onRootError(name, _)).timed
      timer              <- meter.histogram(s"latency-${normalizeMetricsName(name)}", unit = Some("ms"))
      _                  <- timer.record(duration.toMillis.toDouble)
    } yield result

  override def track[R, E, A](name: String, tags: (String, String)*)(effect: => ZIO[R, E, A]): ZIO[R, E, A] =
    for {
      attrs              <- buildAttributes(tags*)
      _                  <- meter.counter(s"hits-${normalizeMetricsName(name)}").flatMap(_.inc())
      (duration, result) <- (effect @@ span(name, attributes = attrs)).timed
      timer              <- meter.histogram(s"latency-${normalizeMetricsName(name)}", unit = Some("ms"))
      _                  <- timer.record(duration.toMillis.toDouble)
    } yield result

  private def onRootError[E](name: String, error: Cause[E]): UIO[Unit] =
    for {
      _ <- ZIO.logError(error.toString)
      _ <- tracing.addEvent(s"$name produced ${error.failures}")
      _ <- tracing.setAttribute("error", true)
      _ <- meter.counter(s"errors-${normalizeMetricsName(name)}").flatMap(_.inc())
    } yield ()

  private def buildAttributes(tags: (String, String)*): UIO[Attributes] =
    ZIO.succeed:
      val builder = Attributes.builder()
      tags.foreach { case (k, v) => builder.put(k, v) }
      builder.build()
      
  private def normalizeMetricsName(name: String): String =
    name.toLowerCase.replaceAll("[^a-z0-9_]", "_")
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
