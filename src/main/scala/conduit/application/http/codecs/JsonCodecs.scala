package conduit.application.http.codecs

import conduit.application.http.error.HttpError
import conduit.domain.model.request.Patchable
import conduit.domain.model.request.user.*
import conduit.domain.model.response.user.*
import conduit.domain.model.request.article.*
import conduit.domain.model.response.article.*
import conduit.domain.model.request.comment.*
import conduit.domain.model.response.{ ArticleResponse, CommentResponse, UserResponse }
import conduit.domain.model.response.comment.*
import io.circe.{ Decoder, DecodingFailure, Encoder }
import io.circe.generic.semiauto.deriveDecoder as decoder
import io.circe.generic.semiauto.deriveEncoder as encoder

object JsonCodecs {

  object Error {
    // encoder for response
    given resError: Encoder[HttpError]                    = encoder
    given notFound: Encoder[HttpError.NotFound]           = encoder
    given badRequest: Encoder[HttpError.BadRequest]       = encoder
    given forbidden: Encoder[HttpError.Forbidden]         = encoder
    given badParams: Encoder[HttpError.BadParameter]      = encoder
    given internalError: Encoder[HttpError.InternalError] = encoder
  }

  object Article {
    import User.resProfilePayload

    // decoders for request bodies
    given reqCreateData: Decoder[CreateArticleRequest.Data]       = decoder
    given reqCreatePayload: Decoder[CreateArticleRequest.Payload] = decoder
    given reqUpdatePayload: Decoder[UpdateArticleRequest.Payload] = decoder

    // encoders for response
    given resArticle: Encoder[GetArticleResponse]                     = encoder
    given resArticlePayload: Encoder[GetArticleResponse.Payload]      = encoder
    given resMultipleArticles: Encoder[ArticleListResponse]           = encoder
    given resArticleListPayload: Encoder[ArticleListResponse.Payload] = encoder
    given resDeleteArticle: Encoder[DeleteArticleResponse]            = encoder
    given resTags: Encoder[TagListResponse]                           = encoder

    given reqUpdateData: Decoder[UpdateArticleRequest.Data] = Decoder.instance { cursor =>
      for {
        title       <- cursor.patchable[String]("title")
        description <- cursor.patchable[String]("description")
        body        <- cursor.patchable[String]("body")
      } yield UpdateArticleRequest.Data(title, description, body)
    }

    given response: Encoder[ArticleResponse] = Encoder.instance {
      case article: GetArticleResponse   => resArticle.apply(article)
      case articles: ArticleListResponse => resMultipleArticles.apply(articles)
      case tags: TagListResponse         => resTags.apply(tags)
      case delete: DeleteArticleResponse => resDeleteArticle.apply(delete) // reuse single article encoder
    }
  }

  object Comment {
    import User.resProfilePayload

    // decoders for request bodies
    given reqAddData: Decoder[AddCommentRequest.Data]       = decoder
    given reqAddPayload: Decoder[AddCommentRequest.Payload] = decoder

    // encoders for response
    given resComment: Encoder[GetCommentResponse]                = encoder
    given resCommentPayload: Encoder[GetCommentResponse.Payload] = encoder
    given resComments: Encoder[CommentListResponse]              = encoder
    given resDeleteComment: Encoder[DeleteCommentResponse]       = encoder

    given response: Encoder[CommentResponse] = Encoder.instance {
      case comment: GetCommentResponse    => resComment.apply(comment)
      case comments: CommentListResponse  => resComments.apply(comments)
      case deleted: DeleteCommentResponse => resDeleteComment.apply(deleted)
    }
  }

  object User {
    // decoders for request bodies
    given reqAuthData: Decoder[AuthenticateRequest.Data]        = decoder
    given reqAuthPayload: Decoder[AuthenticateRequest.Payload]  = decoder
    given reqRegisterData: Decoder[RegistrationRequest.Payload] = decoder
    given reqRegisterPayload: Decoder[RegistrationRequest.Data] = decoder
    given reqUpdatePayload: Decoder[UpdateUserRequest.Payload]  = decoder

    given reqUpdateData: Decoder[UpdateUserRequest.Data] = Decoder.instance { cursor =>
      for {
        email    <- cursor.patchable[String]("email")
        username <- cursor.patchable[String]("username")
        password <- cursor.patchable[String]("password")
        bio      <- cursor.patchable[String]("bio")
        image    <- cursor.patchable[String]("image")
      } yield UpdateUserRequest.Data(email, username, password, bio, image)
    }

    // encoders for response
    given resAuth: Encoder[AuthenticationResponse]                = encoder
    given resAuthPayload: Encoder[AuthenticationResponse.Payload] = encoder
    given resProfile: Encoder[GetProfileResponse]                 = encoder
    given resProfilePayload: Encoder[GetProfileResponse.Payload]  = encoder

    given response: Encoder[UserResponse] = Encoder.instance {
      case auth: AuthenticationResponse => resAuth.apply(auth)
      case profile: GetProfileResponse  => resProfile.apply(profile)
    }
  }

  // helper to decode Patchable fields
  extension (cursor: io.circe.HCursor)
    private def patchable[A: Decoder](field: String): Either[DecodingFailure, Patchable[A]] =
      cursor
        .get[Option[A]](field)
        .map:
          case Some(value) => Patchable.Present(value)
          case None        => if cursor.downField(field).succeeded then Patchable.Empty else Patchable.Absent

}
