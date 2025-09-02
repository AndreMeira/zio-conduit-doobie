package conduit.domain.logic.persistence

import conduit.domain.model.error.ApplicationError
import zio.ZIO

trait UnitOfWork[Tx] {
  def execute[R, E <: ApplicationError, A](effect: ZIO[R & Tx, E, A]): ZIO[R, E, A]
}
