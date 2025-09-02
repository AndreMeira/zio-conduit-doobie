package conduit.domain.logic.endpoint

import conduit.domain.model.error.ApplicationError
import conduit.domain.model.request.CommentRequest
import conduit.domain.model.request.comment.*
import conduit.domain.model.response.CommentResponse
import conduit.domain.model.response.comment.*
import zio.ZIO

trait CommentEndpoint[Tx] {
  type Result[A] = ZIO[Any, ApplicationError, A]

  def addComment(request: AddCommentRequest): Result[GetCommentResponse]
  def deleteComment(request: DeleteCommentRequest): Result[GetCommentResponse]
  def listComments(request: ListCommentsRequest): Result[CommentResponse]

  def handle(request: CommentRequest): Result[CommentResponse] = request match
    case r: AddCommentRequest    => addComment(r)
    case r: DeleteCommentRequest => deleteComment(r)
    case r: ListCommentsRequest  => listComments(r)
}
