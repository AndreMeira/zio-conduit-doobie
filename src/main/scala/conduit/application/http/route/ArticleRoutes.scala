package conduit.application.http.route

import conduit.application.http.codecs.JsonCodecs.Article.given
import conduit.application.http.service.{ HttpAuth, RequestParser }
import conduit.domain.logic.entrypoint.ArticleEntrypoint
import conduit.domain.model.error.ApplicationError
import conduit.domain.model.request.article.*
import io.circe.syntax.*
import izumi.reflect.Tag as ReflectionTag
import zio.ZLayer
import zio.http.*
import zio.http.Method.{ DELETE, GET, POST, PUT }
import zio.http.codec.PathCodec.{ literal, string }

class ArticleRoutes(auth: HttpAuth, parser: RequestParser, logic: ArticleEntrypoint) {

  def routes: Routes[Any, ApplicationError] =
    literal("api") / Routes(
      // get article by slug
      GET / "articles" / string("slug") -> handler { (slug: String, request: Request) =>
        for {
          user   <- auth.anyone(request)
          result <- logic.run(GetArticleRequest(user, slug))
        } yield Response.json(result.asJson.toString)
      },

      // list articles
      GET / "articles" -> handler { (request: Request) =>
        for {
          user   <- auth.anyone(request)
          page    = pagination(request)
          search  = filters(request)
          result <- logic.run(ListArticlesRequest(user, page.offset, page.limit, search))
        } yield Response.json(result.asJson.toString)
      },

      // list feed articles
      GET / "articles" / "feed" -> handler { (request: Request) =>
        for {
          user   <- auth.authenticated(request)
          page    = pagination(request)
          result <- logic.run(ArticleFeedRequest(user, page.offset, page.limit))
        } yield Response.json(result.asJson.toString)
      },

      // list tags
      GET / "tags" -> handler { (request: Request) =>
        for {
          user   <- auth.anyone(request)
          result <- logic.run(ListTagsRequest(user))
        } yield Response.json(result.asJson.toString)
      },

      // create article
      POST / "articles" -> handler { (request: Request) =>
        for {
          user    <- auth.authenticated(request)
          payload <- parser.decode[CreateArticleRequest.Payload](request)
          result  <- logic.run(CreateArticleRequest(user, payload))
        } yield Response.json(result.asJson.toString)
      },

      // update article
      PUT / "articles" / string("slug")               -> handler { (slug: String, request: Request) =>
        for {
          user    <- auth.authenticated(request)
          payload <- parser.decode[UpdateArticleRequest.Payload](request)
          result  <- logic.run(UpdateArticleRequest(user, slug, payload))
        } yield Response.json(result.asJson.toString)
      },
      POST / "articles" / string("slug") / "favorite" -> handler { (slug: String, request: Request) =>
        for {
          user   <- auth.authenticated(request)
          result <- logic.run(AddFavoriteArticleRequest(user, slug))
        } yield Response.json(result.asJson.toString)
      },

      // delete article
      DELETE / "articles" / string("slug") -> handler { (slug: String, request: Request) =>
        for {
          user <- auth.authenticated(request)
          _    <- logic.run(DeleteArticleRequest(user, slug))
        } yield Response.status(Status.NoContent)
      },
    )

  private def pagination(request: Request): (offset: Int, limit: Int) = {
    val offset = request.url.queryParams.queryParam("offset").flatMap(s => s.toIntOption).getOrElse(0)
    val limit  = request.url.queryParams.queryParam("limit").flatMap(s => s.toIntOption).getOrElse(20)
    (offset = offset, limit = limit)
  }

  private def filters(request: Request): List[ListArticlesRequest.Filter] =
    List(
      request.url.queryParams.queryParam("tag").map(ListArticlesRequest.Filter.Tag(_)),
      request.url.queryParams.queryParam("author").map(ListArticlesRequest.Filter.Author(_)),
      request.url.queryParams.queryParam("favorited").map(ListArticlesRequest.Filter.FavoriteOf(_)),
    ).flatten
}

object ArticleRoutes:
  val layer: ZLayer[ArticleEntrypoint & RequestParser & HttpAuth, Nothing, ArticleRoutes] =
    ZLayer {
      for
        auth   <- zio.ZIO.service[HttpAuth]
        parser <- zio.ZIO.service[RequestParser]
        logic  <- zio.ZIO.service[ArticleEntrypoint]
      yield ArticleRoutes(auth, parser, logic)
    }
