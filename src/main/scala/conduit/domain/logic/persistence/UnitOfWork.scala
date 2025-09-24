package conduit.domain.logic.persistence

import conduit.domain.model.error.ApplicationError
import zio.ZIO

trait UnitOfWork[Tx] {
  def execute[R, A](effect: ZIO[R & Tx, ApplicationError, A]): ZIO[R, ApplicationError, A]
}
