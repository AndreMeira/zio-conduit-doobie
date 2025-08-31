package conduit.domain.model.request.article

import conduit.domain.model.entity.User

case class CreateArticleRequest(requester: User, payload: CreateArticleRequest.Payload)

object CreateArticleRequest:
  case class Payload(article: Data) // wrapping due to api spec
  case class Data(
      title: String,
      description: String,
      body: String,
      tagList: Option[List[String]],
    )
