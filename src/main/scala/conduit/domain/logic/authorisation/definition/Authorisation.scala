package conduit.domain.logic.authorisation.definition

import conduit.domain.model.error.ApplicationError
import conduit.domain.model.error.ApplicationError.UnauthorisedError
import zio.ZIO

trait Authorisation[Tx, Request, Failure <: UnauthorisedError] {
  type Error <: ApplicationError
  protected type Result = ZIO[Tx, Error, Authorisation.Result[Failure]]

  def authorise(request: Request): Result

  // Helper method when no logic is needed
  protected def allowed: Result = ZIO.succeed(Authorisation.Result.Allowed)
}

object Authorisation:
  enum Result[+E <: UnauthorisedError]:
    case Allowed
    case Denied(reason: E)
