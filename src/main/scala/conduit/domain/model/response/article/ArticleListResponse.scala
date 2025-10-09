package conduit.domain.model.response.article

import conduit.domain.model.entity.Article
import conduit.domain.model.response.user.GetProfileResponse
import conduit.domain.model.types.article.{ ArticleId, AuthorId }

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
    author: GetProfileResponse.Payload,
  )

  def make(
    count: Int,
    articles: List[Article.Overview],
    favorites: Set[ArticleId],
    followed: Set[AuthorId],
  ): ArticleListResponse = ArticleListResponse(
    articles = articles.map { article =>
      Payload(
        slug = article.info.slug,
        title = article.info.title,
        description = article.info.description,
        tagList = article.tags,
        createdAt = article.metadata.createdAt.toString,
        updatedAt = article.metadata.updatedAt.toString,
        favorited = favorites.contains(article.id),
        favoritesCount = article.favoriteCount,
        author = GetProfileResponse.make(article.author, followed.contains(AuthorId(article.author.id))).profile,
      )
    },
    articlesCount = count,
  )
