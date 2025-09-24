package conduit.application.http

import conduit.application.http.middleware.{ ErrorMiddleware, MonitorMiddleware }
import conduit.application.http.route.{ ArticleRoutes, CommentRoutes, TraceRoute, UserRoutes }
import conduit.application.http.service.{ HttpAuth, RequestParser }

object HttpModule {
  val layer =
    (HttpAuth.layer ++ RequestParser.layer) >>> (UserRoutes.layer ++ ArticleRoutes.layer ++ CommentRoutes.layer)
      ++ TraceRoute.layer ++ MonitorMiddleware.layer ++ ErrorMiddleware.layer
}
