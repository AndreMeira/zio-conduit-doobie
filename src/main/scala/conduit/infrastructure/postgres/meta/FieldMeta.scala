package conduit.infrastructure.postgres.meta

import conduit.domain.model.types.article.*
import conduit.domain.model.types.comment.*
import conduit.domain.model.types.user.*
import doobie.{ Meta, Read }

import java.net.URI

object FieldMeta {
  given Meta[ArticleSlug]          = Meta[String].imap(ArticleSlug.apply)(identity)
  given Meta[UserId]               = Meta[Long].imap(UserId.apply)(identity)
  given Meta[AuthorId]             = Meta[UserId].imap(AuthorId.apply)(identity)
  given Meta[ArticleTitle]         = Meta[String].imap(ArticleTitle.apply)(identity)
  given Meta[ArticleDescription]   = Meta[String].imap(ArticleDescription.apply)(identity)
  given Meta[ArticleBody]          = Meta[String].imap(ArticleBody.apply)(identity)
  given Meta[ArticleId]            = Meta[Long].imap(ArticleId.apply)(identity)
  given Meta[ArticleFavoriteCount] = Meta[Int].imap(ArticleFavoriteCount.apply)(identity)
  given Meta[ArticleTag]           = Meta[String].imap(ArticleTag.apply)(identity)
  given Meta[UserName]             = Meta[String].imap(UserName.apply)(identity)
  given Meta[Email]                = Meta[String].imap(Email.apply)(identity)
  given Meta[Biography]            = Meta[String].imap(Biography.apply)(identity)
  given Meta[URI]                  = Meta[String].imap(URI.create)(_.toString) // @todo handle invalid URIs
  given Meta[UserImage]            = Meta[URI].imap(UserImage.apply)(identity)
  given Meta[HashedPassword]       = Meta[String].imap(HashedPassword.apply)(identity)
  given Meta[CommentId]            = Meta[Long].imap(CommentId.apply)(identity)
  given Meta[CommentBody]          = Meta[String].imap(CommentBody.apply)(identity)
  given Meta[CommentAuthorId]      = Meta[UserId].imap(CommentAuthorId.apply)(identity)

  given (using Read[List[String]]): Read[List[ArticleTag]] = Read[List[String]].map(_.map(ArticleTag.apply).toList)
}
