package conduit.domain.model.response.comment

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
