package conduit.domain.model.request.comment

import conduit.domain.model.entity.Requester
import conduit.domain.model.types.comment.CommentId

case class DeleteCommentRequest(requester: Requester, comment: CommentId)
