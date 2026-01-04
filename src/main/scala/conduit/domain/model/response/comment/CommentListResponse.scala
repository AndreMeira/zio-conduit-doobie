package conduit.domain.model.response.comment

import conduit.domain.model.entity.{ Comment, UserProfile }
import conduit.domain.model.types.article.AuthorId

import scala.util.chaining.scalaUtilChainingOps

case class CommentListResponse(comments: List[GetCommentResponse.Payload])

object CommentListResponse:
  def make(comments: List[Comment], profiles: List[UserProfile], followed: Set[AuthorId]): CommentListResponse =
    profiles
      .map(a => a.id -> a)
      .toMap
      .pipe: profileById =>
        for {
          comment  <- comments
          profile  <- profileById.get(comment.data.author)
          following = followed.contains(AuthorId(profile.id))
        } yield GetCommentResponse.make(comment, profile, following)
      .pipe: payload =>
        CommentListResponse(payload.map(_.comment))
