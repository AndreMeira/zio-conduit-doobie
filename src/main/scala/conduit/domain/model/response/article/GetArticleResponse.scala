package conduit.domain.model.response.article

import conduit.domain.model.entity.{ Article, UserProfile }
import conduit.domain.model.response.user.GetProfileResponse

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
    author: GetProfileResponse.Payload,
  )

  def make(
    article: Article.Expanded,
    favorited: Boolean,
    following: Boolean,
  ): GetArticleResponse = GetArticleResponse(
    Payload(
      slug = article.data.slug,
      title = article.data.title,
      description = article.data.description,
      body = article.data.body,
      tagList = article.tags,
      createdAt = article.metadata.createdAt.toString,
      updatedAt = article.metadata.updatedAt.toString,
      favorited = favorited,
      favoritesCount = article.favoriteCount,
      author = GetProfileResponse.make(article.author, following).profile,
    )
  )
