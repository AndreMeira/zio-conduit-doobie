package conduit.domain.logic.monitoring

import zio.ZIO

trait Monitor {
  def start[R, E, A](name: String)(effect: => ZIO[R, E, A]): ZIO[R, E, A]
  def track[R, E, A](name: String)(effect: => ZIO[R, E, A]): ZIO[R, E, A] = track(name, List.empty*)(effect)
  def track[R, E, A](name: String, tags: (String, String)*)(effect: => ZIO[R, E, A]): ZIO[R, E, A]
}
