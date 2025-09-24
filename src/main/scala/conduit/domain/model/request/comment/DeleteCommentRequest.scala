package conduit.domain.model.request.comment

import conduit.domain.model.entity.User

case class DeleteCommentRequest(
  requester: User.Authenticated,
  slug: String,
  commentId: Long,
)
