package conduit.application.http.service

import conduit.domain.logic.monitoring.Monitor
import conduit.application.http.service.RequestParser.Failure
import conduit.domain.model.error.ApplicationError
import io.circe.{ Decoder, parser }
import zio.{ ZIO, ZLayer }
import zio.http.Request

class RequestParser(monitor: Monitor) {
  def decode[A: Decoder](request: Request): ZIO[Any, Failure, A] =
    monitor.track("RequestParser.decode"):
      for {
        body   <- request.body.asString.mapError(_ => Failure.EmptyBody)
        decoded = parser.decode[A](body).left.map(err => Failure.InvalidJson(err.getMessage))
        result <- ZIO.fromEither(decoded)
      } yield result
}

object RequestParser {
  enum Failure extends ApplicationError:
    case EmptyBody
    case InvalidJson(reason: String)

    override def message: String = this match
      case EmptyBody           => "Request body cannot be empty"
      case InvalidJson(reason) => s"Invalid JSON: $reason"

  val layer: ZLayer[Monitor, Nothing, RequestParser] = ZLayer {
    for monitor <- ZIO.service[Monitor]
    yield RequestParser(monitor)
  }
}
