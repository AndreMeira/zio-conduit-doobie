package conduit.domain.logic.validation

import conduit.domain.model.entity.Comment
import conduit.domain.model.error.ApplicationError
import conduit.domain.model.request.comment.AddCommentRequest
import zio.ZIO

trait CommentValidator[Tx] {
  def validateCreate(request: AddCommentRequest): ZIO[Tx, ApplicationError, Comment.Data]
}
