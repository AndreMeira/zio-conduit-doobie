package conduit.domain.model.request

import conduit.domain.model.request.comment.*

type CommentRequest = CommentRequest.Type
object CommentRequest:
  type Type = 
    AddCommentRequest 
    | DeleteCommentRequest 
    | ListCommentsRequest
