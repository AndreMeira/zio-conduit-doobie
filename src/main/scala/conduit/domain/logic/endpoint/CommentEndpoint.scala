package conduit.domain.logic.endpoint

import conduit.domain.model.error.ApplicationError
import conduit.domain.model.request.comment.*
import conduit.domain.model.response.comment.*
import zio.ZIO

trait CommentEndpoint[Tx] {
  def addComment(request: AddCommentRequest): ZIO[Tx, ApplicationError, GetCommentResponse]
  def deleteComment(request: DeleteCommentRequest): ZIO[Tx, ApplicationError, GetCommentResponse]
}
