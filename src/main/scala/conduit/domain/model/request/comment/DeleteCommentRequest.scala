package conduit.domain.model.request.comment

import conduit.domain.model.entity.User
import conduit.domain.model.types.comment.CommentId

case class DeleteCommentRequest(requester: User, comment: CommentId)
