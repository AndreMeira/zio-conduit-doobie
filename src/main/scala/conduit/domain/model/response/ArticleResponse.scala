package conduit.domain.model.response

import conduit.domain.model.request.ArticleRequest
import conduit.domain.model.request.article.*
import conduit.domain.model.response.article.*

type ArticleResponse = ArticleResponse.Type
object ArticleResponse:
  type Type = GetArticleResponse | ArticleListResponse | DeleteArticleRequest | TagListResponse

  type Of[A <: ArticleRequest] = A match
    case CreateArticleRequest         => GetArticleResponse
    case UpdateArticleRequest         => GetArticleResponse
    case GetArticleRequest            => GetArticleResponse
    case AddFavoriteArticleRequest    => GetArticleResponse
    case RemoveFavoriteArticleRequest => GetArticleResponse
    case DeleteArticleRequest         => DeleteArticleRequest
    case ListArticlesRequest          => ArticleListResponse
    case ArticleFeedRequest           => ArticleListResponse
