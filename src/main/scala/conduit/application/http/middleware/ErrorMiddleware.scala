package conduit.application.http.middleware

import conduit.application.http.codecs.JsonCodecs.Error.given
import conduit.application.http.error.HttpError
import conduit.domain.model.error.ApplicationError
import io.circe.syntax.*
import zio.ZLayer
import zio.http.{ Middleware, Response, Routes, Status }

class ErrorMiddleware extends Middleware[Any] {
  override def apply[Env, Err](routes: Routes[Env, Err]): Routes[Env, Nothing] =
    routes.handleError:
      case error: ApplicationError =>
        HttpError.fromApplicationError(error) match {
          case e: HttpError.NotFound   => Response.json(e.asJson.toString).status(Status.NotFound)
          case e: HttpError.Forbidden  => Response.json(e.asJson.toString).status(Status.Forbidden)
          case e: HttpError.BadRequest => Response.json(e.asJson.toString).status(Status.BadRequest)
          case e                       => Response.json(e.asJson.toString).status(Status.InternalServerError)
        }
      case _                       => Response.status(Status.InternalServerError)
}

object ErrorMiddleware {
  val layer: ZLayer[Any, Nothing, ErrorMiddleware] = ZLayer.succeed(new ErrorMiddleware)
}
