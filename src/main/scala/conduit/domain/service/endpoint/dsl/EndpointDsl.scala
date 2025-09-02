package conduit.domain.service.endpoint.dsl

import conduit.domain.logic.authorisation.Authorisation
import conduit.domain.model.error.ApplicationError.{ NotFoundError, UnauthorisedError, ValidationError }
import conduit.domain.model.error.{ ApplicationError, InvalidInput }
import zio.ZIO
import zio.prelude.Validation

trait EndpointDsl {
  extension [R, E, Err <: ValidationError, A](zio: ZIO[R, E, Validation[Err, A]]) {
    def validOrFail: ZIO[R, E | InvalidInput, A]                              = zio.flatMap {
      case Validation.Success(_, a) => ZIO.succeed(a)
      case Validation.Failure(_, e) => ZIO.fail(InvalidInput(e.toList))
    }
    def validOrFailWith[E1 <: ApplicationError](error: E1): ZIO[R, E | E1, A] = zio.flatMap {
      case Validation.Success(_, a) => ZIO.succeed(a)
      case Validation.Failure(_, _) => ZIO.fail(error)
    }
  }
  extension [R, E, E1 <: UnauthorisedError](zio: ZIO[R, E, Authorisation.Result[E1]]) {
    def allowedOrFail: ZIO[R, E | UnauthorisedError, Unit] = zio.flatMap {
      case Authorisation.Result.Allowed       => ZIO.unit
      case Authorisation.Result.NotAllowed(e) => ZIO.fail(e)
    }
  }
}
