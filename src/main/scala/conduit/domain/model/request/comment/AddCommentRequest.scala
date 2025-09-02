package conduit.domain.model.request.comment

import conduit.domain.model.entity.User
import conduit.domain.model.types.article.ArticleSlug
import conduit.domain.model.types.comment.CommentBody

case class AddCommentRequest(
  requester: User.Authenticated,
  article: ArticleSlug,
  payload: AddCommentRequest.Payload,
)

object AddCommentRequest:
  case class Payload(comment: Data) // wrapping due to api spec
  case class Data(body: CommentBody)
