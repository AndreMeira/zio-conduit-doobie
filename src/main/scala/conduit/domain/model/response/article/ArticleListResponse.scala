package conduit.domain.model.response.article

import conduit.domain.model.response.user.ProfileResponse

case class ArticleListResponse(articles: List[ArticleListResponse.Payload], articlesCount: Int)

object ArticleListResponse:
  case class Payload(
    slug: String,
    title: String,
    description: String,
    tagList: List[String],
    createdAt: String,
    updatedAt: String,
    favorited: Boolean,
    favoritesCount: Int,
    author: ProfileResponse.Payload
  )
