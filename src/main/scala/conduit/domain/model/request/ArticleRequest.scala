package conduit.domain.model.request

import conduit.domain.model.request.article.*

type ArticleRequest = ArticleRequest.Type
object ArticleRequest:
  type Type = CreateArticleRequest | UpdateArticleRequest | GetArticleRequest | DeleteArticleRequest | ListArticlesRequest | ArticleFeedRequest |
    AddFavoriteArticleRequest | RemoveFavoriteArticleRequest
