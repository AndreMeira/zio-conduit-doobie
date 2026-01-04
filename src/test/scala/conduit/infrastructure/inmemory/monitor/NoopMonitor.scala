package conduit.infrastructure.inmemory.monitor

import conduit.domain.logic.monitoring.Monitor
import zio.{ ULayer, ZIO, ZLayer }

class NoopMonitor extends Monitor {
  override def start[R, E, A](name: String)(effect: => ZIO[R, E, A]): ZIO[R, E, A]                          = effect
  override def track[R, E, A](name: String, tags: (String, String)*)(effect: => ZIO[R, E, A]): ZIO[R, E, A] = effect
}

object NoopMonitor {
  val layer: ULayer[NoopMonitor] = ZLayer.succeed(new NoopMonitor)
}
