package conduit.application.http

import conduit.application.http.middleware.{ErrorMiddleware, MonitorMiddleware}
import conduit.application.http.route.{ArticleRoutes, CommentRoutes, InMemoryTraceRoute, UserRoutes}
import conduit.application.http.service.{HttpAuth, RequestParser}
import zio.http.Server

object HttpModule {
  val layer =
    (HttpAuth.layer ++ RequestParser.layer) >>> (UserRoutes.layer ++ ArticleRoutes.layer ++ CommentRoutes.layer)
      ++ InMemoryTraceRoute.layer ++ MonitorMiddleware.layer ++ ErrorMiddleware.layer ++ Server.default
}
