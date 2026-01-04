package conduit.application.http.route

import conduit.application.http.codecs.JsonCodecs.Comment.given
import conduit.application.http.service.{ HttpAuth, RequestParser }
import conduit.domain.logic.entrypoint.CommentEntrypoint
import conduit.domain.model.error.ApplicationError
import conduit.domain.model.request.comment.*
import io.circe.syntax.*
import zio.http.Method.{ DELETE, GET, POST }
import zio.http.codec.PathCodec.{ literal, long, string }
import zio.http.*
import zio.{ ZIO, ZLayer }
import izumi.reflect.Tag as ReflectionTag

class CommentRoutes(auth: HttpAuth, parser: RequestParser, logic: CommentEntrypoint) {

  def routes: Routes[Any, ApplicationError] =
    literal("api/articles") / Routes(
      // Add comment to an article
      POST / string("slug") / "comments" -> handler { (slug: String, request: Request) =>
        for {
          user    <- auth.authenticated(request)
          payload <- parser.decode[AddCommentRequest.Payload](request)
          result  <- logic.run(AddCommentRequest(user, slug, payload))
        } yield Response.json(result.asJson.toString)
      },

      // List comments for an article
      GET / string("slug") / "comments" -> handler { (slug: String, request: Request) =>
        for {
          user   <- auth.anyone(request)
          result <- logic.run(ListCommentsRequest(user, slug))
        } yield Response.json(result.asJson.toString)
      },

      // Delete a comment
      DELETE / string("slug") / "comments" / long("id") -> handler { (slug: String, id: Long, request: Request) =>
        for {
          user <- auth.authenticated(request)
          _    <- logic.run(DeleteCommentRequest(user, slug, id))
        } yield Response.status(Status.NoContent)
      },
    )
}

object CommentRoutes:
  val layer: ZLayer[HttpAuth & RequestParser & CommentEntrypoint, Nothing, CommentRoutes] =
    ZLayer {
      for {
        auth   <- ZIO.service[HttpAuth]
        parser <- ZIO.service[RequestParser]
        logic  <- ZIO.service[CommentEntrypoint]
      } yield CommentRoutes(auth, parser, logic)
    }
