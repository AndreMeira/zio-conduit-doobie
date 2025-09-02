package conduit.domain.model.response.comment

import conduit.domain.model.entity.{ Comment, UserProfile }
import conduit.domain.model.response.user.ProfileResponse

case class GetCommentResponse(comment: GetCommentResponse.Payload)

object GetCommentResponse:
  case class Payload(
    id: Long,
    createdAt: String,
    updatedAt: String,
    body: String,
    author: ProfileResponse.Payload,
  )

  def make(comment: Comment, author: UserProfile, following: Boolean): GetCommentResponse =
    GetCommentResponse(
      Payload(
        id = comment.id,
        body = comment.data.body,
        createdAt = comment.metadata.createdAt.toString,
        updatedAt = comment.metadata.updatedAt.toString,
        author = ProfileResponse.make(author, following).profile,
      )
    )
