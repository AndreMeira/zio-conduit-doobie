package conduit.domain.model.request.article

import conduit.domain.model.entity.User
import conduit.domain.model.request.Patchable

case class UpdateArticleRequest(requester: User, payload: UpdateArticleRequest.Payload)

object UpdateArticleRequest:
  case class Payload(article: Data) // wrapping due to spec
  case class Data(
      title: Patchable[String],
      description: Patchable[String],
      body: Patchable[String],
    )
