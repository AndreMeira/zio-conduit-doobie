package conduit.application.http.route

import conduit.application.http.codecs.JsonCodecs.User.given
import conduit.application.http.service.{ HttpAuth, RequestParser }
import conduit.domain.logic.entrypoint.UserEntrypoint
import conduit.domain.model.error.ApplicationError
import conduit.domain.model.request.user.*
import io.circe.syntax.*
import izumi.reflect.Tag as ReflectionTag
import zio.http.*
import zio.http.Method.{ DELETE, GET, POST, PUT }
import zio.http.codec.PathCodec.{ literal, string }
import zio.{ ZIO, ZLayer }

class UserRoutes(auth: HttpAuth, parser: RequestParser, logic: UserEntrypoint) {

  // Define the routes
  def routes: Routes[Any, ApplicationError] =
    literal("api") / Routes(
      // registration
      POST / "users" -> handler { (request: Request) =>
        for {
          user    <- auth.anonymous(request)
          payload <- parser.decode[RegistrationRequest.Payload](request)
          result  <- logic.run(RegistrationRequest(user, payload))
        } yield Response.json(result.asJson.toString)
      },

      // login
      POST / "users" / "login" -> handler { (request: Request) =>
        for {
          user    <- auth.anonymous(request)
          payload <- parser.decode[AuthenticateRequest.Payload](request)
          result  <- logic.run(AuthenticateRequest(user, payload))
        } yield Response.json(result.asJson.toString)
      },

      // get current user
      GET / "user" -> handler { (request: Request) =>
        for {
          user   <- auth.authenticated(request)
          result <- logic.run(GetUserRequest(user))
        } yield Response.json(result.asJson.toString)
      },

      // get profile
      GET / "profiles" / string("username") -> handler { (username: String, request: Request) =>
        for {
          user   <- auth.anyone(request)
          result <- logic.run(GetProfileRequest(user, username))
        } yield Response.json(result.asJson.toString)
      },

      // follow user
      POST / "profiles" / string("username") / "follow" -> handler { (username: String, request: Request) =>
        for {
          user   <- auth.authenticated(request)
          result <- logic.run(FollowUserRequest(user, username))
        } yield Response.json(result.asJson.toString)
      },

      // unfollow user
      DELETE / "profiles" / string("username") / "follow" -> handler { (username: String, request: Request) =>
        for {
          user   <- auth.authenticated(request)
          result <- logic.run(UnfollowUserRequest(user, username))
        } yield Response.json(result.asJson.toString)
      },

      // update user
      PUT / "user" -> handler { (request: Request) =>
        for {
          user    <- auth.authenticated(request)
          payload <- parser.decode[UpdateUserRequest.Payload](request)
          result  <- logic.run(UpdateUserRequest(user, payload))
        } yield Response.json(result.asJson.toString)
      },
    )
}

object UserRoutes:
  val layer: ZLayer[HttpAuth & RequestParser & UserEntrypoint, Nothing, UserRoutes] =
    ZLayer {
      for {
        auth   <- ZIO.service[HttpAuth]
        parser <- ZIO.service[RequestParser]
        logic  <- ZIO.service[UserEntrypoint]
      } yield UserRoutes(auth, parser, logic)
    }
