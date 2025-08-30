package conduit.domain.model.response

import conduit.domain.model.request.CommentRequest
import conduit.domain.model.request.comment.*
import conduit.domain.model.response.comment.*

type CommentResponse = CommentResponse.Type
object CommentResponse:
  type Type = GetCommentResponse | CommentListResponse | DeleteCommentResponse

  type Of[A <: CommentRequest] = A match
    case AddCommentRequest    => GetCommentResponse
    case ListCommentsRequest  => CommentListResponse
    case DeleteCommentRequest => DeleteCommentResponse
