package conduit.domain.logic.endpoint

import conduit.domain.model.error.ApplicationError
import conduit.domain.model.request.ArticleRequest
import conduit.domain.model.request.article.*
import conduit.domain.model.response.ArticleResponse
import conduit.domain.model.response.article.*
import zio.ZIO

trait ArticleEndpoint[Tx] {
  protected type Result[A] = ZIO[Tx, ApplicationError, A]

  def tags(request: ListTagsRequest): Result[TagListResponse]
  def get(request: GetArticleRequest): Result[GetArticleResponse]
  def feed(request: ArticleFeedRequest): Result[ArticleListResponse]
  def create(request: CreateArticleRequest): Result[GetArticleResponse]
  def update(request: UpdateArticleRequest): Result[GetArticleResponse]
  def recent(request: ListArticlesRequest): Result[ArticleListResponse]
  def delete(request: DeleteArticleRequest): Result[DeleteArticleResponse]
  def addFavorite(request: AddFavoriteArticleRequest): Result[GetArticleResponse]
  def removeFavorite(request: RemoveFavoriteArticleRequest): Result[GetArticleResponse]

  def handle(request: ArticleRequest): Result[ArticleResponse.Type] = request match
    case r: GetArticleRequest            => get(r)
    case r: ArticleFeedRequest           => feed(r)
    case r: CreateArticleRequest         => create(r)
    case r: UpdateArticleRequest         => update(r)
    case r: ListArticlesRequest          => recent(r)
    case r: DeleteArticleRequest         => delete(r)
    case r: AddFavoriteArticleRequest    => addFavorite(r)
    case r: RemoveFavoriteArticleRequest => removeFavorite(r)
}
