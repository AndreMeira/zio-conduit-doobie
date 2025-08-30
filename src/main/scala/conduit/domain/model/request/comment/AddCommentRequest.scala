package conduit.domain.model.request.comment

import conduit.domain.model.types.article.ArticleSlug
import conduit.domain.model.types.comment.CommentBody
import conduit.domain.model.types.user.UserId

case class AddCommentRequest(requester: UserId, article: ArticleSlug, payload: AddCommentRequest.Payload)

object AddCommentRequest:
  case class Payload(comment: Data) // wrapping due to api spec
  case class Data(body: CommentBody)
