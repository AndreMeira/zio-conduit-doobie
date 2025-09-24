package conduit.domain.service.entrypoint.dsl

import conduit.domain.logic.authorisation.definition.Authorisation
import conduit.domain.logic.persistence.UnitOfWork
import conduit.domain.model.error.{ ApplicationError, InvalidInput }
import conduit.domain.model.error.ApplicationError.{ UnauthorisedError, ValidationError }
import zio.ZIO
import zio.prelude.Validation

trait EntrypointDsl[Tx, Req, E <: UnauthorisedError](
  unitOfWork: UnitOfWork[Tx],
  authorisation: Authorisation[Tx, Req, E],
) {
  def authorise[A](request: Req)(logic: ZIO[Tx, ApplicationError, A]): ZIO[Any, ApplicationError, A] =
    unitOfWork.execute:
      for {
        _      <- authorisation.authorise(request).allowedOrFail
        result <- logic
      } yield result

  extension [R, E1, Err <: UnauthorisedError](zio: ZIO[R, E1, Authorisation.Result[Err]]) {
    def allowedOrFail: ZIO[R, E1 | Err, Unit] = zio.flatMap {
      case Authorisation.Result.Allowed   => ZIO.unit
      case Authorisation.Result.Denied(e) => ZIO.fail(e)
    }
  }

  extension [R, E1, Err <: ValidationError, A](zio: ZIO[R, E1, Validation[Err, A]]) {
    def validOrFail: ZIO[R, E1 | InvalidInput, A]                              = zio.flatMap {
      case Validation.Success(_, a) => ZIO.succeed(a)
      case Validation.Failure(_, e) => ZIO.fail(InvalidInput(e.toList))
    }
    def validOrFailWith[E2 <: ApplicationError](error: E1): ZIO[R, E1 | E2, A] = zio.flatMap {
      case Validation.Success(_, a) => ZIO.succeed(a)
      case Validation.Failure(_, _) => ZIO.fail(error)
    }
  }

  extension [R, E1, Err <: ApplicationError, A](zio: ZIO[R, E1, Option[A]]) {
    def ?!(error: => Err): ZIO[R, E1 | Err, A] = zio.someOrFail(error)
  }
}
