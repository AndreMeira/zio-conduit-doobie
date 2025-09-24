package conduit.application.http.error

import conduit.domain.model.error.{ ApplicationError, InvalidInput }
import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder as encoder

enum HttpError:
  case InternalError(kind: String, message: String)
  case NotFound(kind: String, message: String)
  case Forbidden(kind: String, message: String)
  case BadRequest(errors: List[HttpError.BadParameter])

object HttpError:
  given Encoder[HttpError.NotFound]      = encoder
  given Encoder[HttpError.Forbidden]     = encoder
  given Encoder[HttpError.BadRequest]    = encoder
  given Encoder[HttpError.BadParameter]  = encoder
  given Encoder[HttpError.InternalError] = encoder
  given Encoder[HttpError]               = encoder

  case class BadParameter(key: String, kind: String, message: String)

  object BadParameter:
    def list(errors: List[ApplicationError.ValidationError]): List[BadParameter] =
      errors.map(err => BadParameter(err.key, err.kind, err.message))

  def fromApplicationError(error: ApplicationError): HttpError = error match
    case e: ApplicationError.NotFoundError     => HttpError.NotFound(e.kind, e.message)
    case e: ApplicationError.UnauthorisedError => HttpError.Forbidden(e.kind, e.message)
    case InvalidInput(errs)                    => HttpError.BadRequest(BadParameter.list(errs))
    case e                                     => HttpError.InternalError(e.kind, e.message)
