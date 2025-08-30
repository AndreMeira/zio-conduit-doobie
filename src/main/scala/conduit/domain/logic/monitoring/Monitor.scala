package conduit.domain.logic.monitoring

import conduit.domain.model.error.ApplicationError
import zio.ZIO

trait Monitor {
  def track[R, A](effect: ZIO[R, ApplicationError, A]): ZIO[R, ApplicationError, A]
}
