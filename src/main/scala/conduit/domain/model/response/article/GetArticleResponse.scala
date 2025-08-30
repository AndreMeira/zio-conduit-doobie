package conduit.domain.model.response.article

import conduit.domain.model.response.user.ProfileResponse

case class GetArticleResponse(article: GetArticleResponse.Payload)

object GetArticleResponse:
  case class Payload(
    slug: String,
    title: String,
    description: String,
    body: String,
    tagList: List[String],
    createdAt: String,
    updatedAt: String,
    favorited: Boolean,
    favoritesCount: Int,
    author: ProfileResponse.Payload,
  )
