package conduit.infrastructure.opentelemetry

object Module {
  val layer = OpenTelemetryMonitor.metrics ++ OpenTelemetryMonitor.otel >>> OpenTelemetryMonitor.layer
}
