package conduit.application.http.middleware

import conduit.domain.logic.monitoring.Monitor
import zio.{ ZIO, ZLayer }
import zio.http.{ Handler, Middleware, Routes }

class MonitorMiddleware(monitor: Monitor) extends Middleware[Any] {

  override def apply[Env, Err](routes: Routes[Env, Err]): Routes[Env, Err] =
    routes.transform: handler =>
      Handler.scoped[Env]:
        Handler.fromFunctionZIO: req =>
          monitor.start(req.method.toString ++ " " ++ req.url.toString)(handler(req))
}

object MonitorMiddleware:
  val layer: ZLayer[Monitor, Nothing, MonitorMiddleware] = ZLayer {
    for monitor <- ZIO.service[Monitor]
    yield new MonitorMiddleware(monitor)
  }
