package conduit.domain.model.request.article

import conduit.domain.model.entity.User
import conduit.domain.model.request.Patchable
import conduit.domain.model.types.article.ArticleSlug

case class UpdateArticleRequest(
  requester: User.Authenticated,
  slug: String,
  payload: UpdateArticleRequest.Payload,
)

object UpdateArticleRequest:
  case class Payload(article: Data) // wrapping due to spec

  case class Data(
    title: Patchable[String],
    description: Patchable[String],
    body: Patchable[String],
  )
